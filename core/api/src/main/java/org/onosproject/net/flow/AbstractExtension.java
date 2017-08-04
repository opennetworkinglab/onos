/*
 * Copyright 2015-present Open Networking Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onosproject.net.flow;

import org.onosproject.net.flow.instructions.ExtensionPropertyException;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * Abstract implementation of the set/get property methods of Extension.
 */
public abstract class AbstractExtension implements Extension {

    private static final String INVALID_KEY = "Invalid property key: ";
    private static final String INVALID_TYPE = "Given type does not match field type: ";

    @Override
    public <T> void setPropertyValue(String key, T value) throws
            ExtensionPropertyException {
        Class<?> clazz = this.getClass();
        try {
            Field field = clazz.getDeclaredField(key);
            field.setAccessible(true);
            field.set(this, value);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new ExtensionPropertyException(INVALID_KEY + key);
        }
    }

    @Override
    public <T> T getPropertyValue(String key) throws ExtensionPropertyException {
        Class<?> clazz = this.getClass();
        try {
            Field field = clazz.getDeclaredField(key);
            field.setAccessible(true);
            @SuppressWarnings("unchecked")
            T result = (T) field.get(this);
            return result;
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new ExtensionPropertyException(INVALID_KEY + key);
        } catch (ClassCastException e) {
            throw new ExtensionPropertyException(INVALID_TYPE + key);
        }
    }

    @Override
    public List<String> getProperties() {
        Class<?> clazz = this.getClass();

        List<String> fields = new ArrayList<>();

        for (Field field : clazz.getDeclaredFields()) {
            fields.add(field.getName());
        }

        return fields;
    }
}
