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

package dev.nick.android.injection.processors;

import android.content.Context;
import android.support.annotation.NonNull;

import com.google.common.base.Preconditions;

import java.lang.reflect.Field;

import dev.nick.android.injection.BuildConfig;
import dev.nick.android.injection.annotation.common.Component;
import dev.nick.android.injection.utils.ReflectionUtils;
import lombok.Getter;

public abstract class FieldProcessor extends BaseProcessor<Object, Field> {

    @Getter
    private boolean strictModeEnabled;

    public FieldProcessor(Context appContext) {
        super(appContext);
        strictModeEnabled = BuildConfig.DEBUG;
    }

    @Override
    public boolean process(@NonNull Object container, @NonNull Field field) {
        in();
        ReflectionUtils.makeAccessible(field);
        checkType(field);
        Object fieldObject = ReflectionUtils.getField(field, container);
        if (fieldObject != null && !ReflectionUtils.isBaseDataType(field.getType())) {
            report("Ignored for none null field.");
            out();
            return true;
        }
        ReflectionUtils.setField(field, container, parseField(field));
        out();
        return true;
    }

    protected Object parseField(Field field) {
        return null;
    }

    protected Class<?> expectedType() {
        return null;
    }

    protected void checkType(Field field) {
        if (strictModeEnabled && expectedType() != null)
            Preconditions.checkState(field.getType() == expectedType(), "Expected type is:" + expectedType());
    }

    protected int checkId(int id) {
        Preconditions.checkState(id > 0, "Bad id:" + id);
        return id;
    }

    public boolean asyncModeAllowed() {
        return false;
    }

    @NonNull
    @Override
    public Component.Android scope() {
        return Component.Android.NoLimit;
    }
}
