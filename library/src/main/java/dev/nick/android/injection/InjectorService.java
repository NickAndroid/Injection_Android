/*
 * Copyright (c) 2016 Nick Guo
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.nick.android.injection;

import android.content.Context;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.XmlRes;

import com.google.common.base.Preconditions;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

import dev.nick.android.injection.processors.FieldProcessor;
import dev.nick.android.injection.processors.TypeProcessor;
import dev.nick.android.injection.utils.ReflectionUtils;
import dev.nick.logger.Logger;
import dev.nick.logger.LoggerManager;
import lombok.Getter;

class InjectorService {

    private static InjectorService sBindings;
    // Synchronized leak.
    private final Map<Class<? extends Annotation>, FieldProcessor> mOFs;
    private final Map<Class<? extends Annotation>, TypeProcessor> mOTs;
    @Getter
    private Context context;

    private Logger mLogger;

    @XmlRes
    private int prebuiltProcessorsXmlRes;

    public InjectorService(Context context) {
        this.context = context;
        this.mLogger = LoggerManager.getLogger(getClass());
        this.mOFs = new HashMap<>();
        this.mOTs = new HashMap<>();
        setPrebuiltProcessorsXmlRes(R.xml.prebuilt_annonation_processors);
    }

    synchronized static void init(Context applicationContext) {
        if (sBindings == null) sBindings = new InjectorService(applicationContext);
    }

    static InjectorService shared() {
        Preconditions.checkNotNull(sBindings, "Call #createShared first.");
        return sBindings;
    }

    void addFieldProcessor(@NonNull FieldProcessor processor) {
        mLogger.verbose(processor);
        mOFs.put(Preconditions.checkNotNull(processor.targetAnnotation()), Preconditions.checkNotNull(processor));
    }

    void addTypeProcessor(@NonNull TypeProcessor processor) {
        mLogger.verbose(processor);
        mOTs.put(Preconditions.checkNotNull(processor.targetAnnotation()), Preconditions.checkNotNull(processor));
    }

    private void publishPrebuiltProcessors() {
        mOFs.clear();
        mOTs.clear();
        new AbsProcessorXmlParser(getContext()) {
            @Override
            protected void onCreateItem(ProcessorItem item) {
                super.onCreateItem(item);
                ProcessorScope scope = ProcessorScope.scope(item.getScope());
                String clzName = item.getClz();
                switch (Preconditions.checkNotNull(scope, "Bad scope:" + scope)) {
                    case Type:
                        TypeProcessor t = (TypeProcessor) invokeProcessorClz(clzName);
                        addTypeProcessor(t);
                        break;
                    case Field:
                        FieldProcessor f = (FieldProcessor) invokeProcessorClz(clzName);
                        addFieldProcessor(f);
                        break;
                    default:
                        throw new IllegalStateException("UnSupported.");
                }
            }
        }.parse(prebuiltProcessorsXmlRes);
    }

    @SuppressWarnings({"unchecked", "TryWithIdenticalCatches"})
    Object invokeProcessorClz(String clzName) {
        Class clz;
        try {
            clz = Class.forName(clzName);
        } catch (ClassNotFoundException e) {
            return null;
        }
        // Find empty constructor.
        Constructor constructor = null;
        try {
            constructor = clz.getDeclaredConstructor();
        } catch (NoSuchMethodException e) {
            mLogger.verbose("No empty constructor for:" + clzName);
        }
        if (constructor != null) {
            ReflectionUtils.makeAccessible(constructor);
            try {
                return constructor.newInstance();
            } catch (InstantiationException ignored) {
            } catch (IllegalAccessException ignored) {
            } catch (InvocationTargetException ignored) {
            }
        }
        mLogger.verbose("Can not find empty Constructor for class:" + clzName);
        // Find Context constructor.
        try {
            constructor = clz.getDeclaredConstructor(Context.class);
        } catch (NoSuchMethodException e) {
            mLogger.warn("No context-ed constructor for:" + clzName);
        }
        if (constructor != null) {
            ReflectionUtils.makeAccessible(constructor);
            try {
                return constructor.newInstance(getContext());
            } catch (InstantiationException ignored) {
            } catch (IllegalAccessException ignored) {
            } catch (InvocationTargetException ignored) {
            }
        }
        mLogger.warn("Failed to create processor for:" + clzName);
        return null;
    }

    void inject(@NonNull final Object target) {
        Class clz = Preconditions.checkNotNull(target).getClass();
        for (final Field field : clz.getDeclaredFields()) {
            Annotation[] annotations = field.getAnnotations();
            for (Annotation a : annotations) {
                Class<? extends Annotation> type = a.annotationType();
                final FieldProcessor fieldProcessor = mOFs.get(type);
                if (fieldProcessor == null) {
                    mLogger.debug("No processor found for:" + a + " of type:" + type);
                    continue;
                }
                if (fieldProcessor.asyncModeAllowed()) {
                    AsyncTask.execute(new Runnable() {
                        @Override
                        public void run() {
                            fieldProcessor.process(target, field);
                        }
                    });
                } else {
                    fieldProcessor.process(target, field);
                }
            }
        }
        for (Annotation a : clz.getAnnotations()) {
            Class<? extends Annotation> type = a.annotationType();
            TypeProcessor typeProcessor = mOTs.get(type);
            if (typeProcessor == null) {
                mLogger.debug("No processor found for:" + a + " of type:" + type);
                continue;
            }
            typeProcessor.process(target, clz);
        }
    }

    void setPrebuiltProcessorsXmlRes(@XmlRes int prebuiltProcessorsXmlRes) {
        this.prebuiltProcessorsXmlRes = prebuiltProcessorsXmlRes;
        publishPrebuiltProcessors();
    }
}
