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
import android.content.res.Resources;
import android.support.annotation.ArrayRes;
import android.support.annotation.NonNull;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;

import dev.nick.android.injection.annotation.binding.BindLongArray;

class LongArrProcessor extends FieldProcessor {

    LongArrProcessor(Context appContext) {
        super(appContext);
    }

    static long[] getLongIntArray(@lombok.NonNull Resources r, @ArrayRes int resId) {
        int[] ar = r.getIntArray(resId);
        if (ar == null) {
            return null;
        }
        long[] out = new long[ar.length];
        for (int i = 0; i < ar.length; i++) {
            out[i] = ar[i];
        }
        return out;
    }

    @Override
    protected Object parseField(Field field) {
        BindLongArray longArray = field.getAnnotation(BindLongArray.class);
        int id = longArray.value();
        return getLongIntArray(getContext().getResources(), checkId(id));
    }

    @NonNull
    @Override
    public Class<? extends Annotation> targetAnnotation() {
        return BindLongArray.class;
    }
}
