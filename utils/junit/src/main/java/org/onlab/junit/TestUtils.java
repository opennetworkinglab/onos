/*
 * Copyright 2014-present Open Networking Foundation
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
package org.onlab.junit;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;


/**
 * Utilities for testing.
 */
public final class TestUtils {

    /**
     * Sets the field, bypassing scope restriction.
     *
     * @param subject Object where the field belongs
     * @param fieldName name of the field to set
     * @param value value to set to the field.
     * @param <T> subject type
     * @param <U> value type
     * @throws TestUtilsException if there are reflection errors while setting
     * the field
     */
    public static <T, U> void setField(T subject, String fieldName, U value)
            throws TestUtilsException {
        @SuppressWarnings("unchecked")
        Class clazz;
        if (subject instanceof Class) {
            // Class was given, assuming intention is to deal with static field
            clazz = (Class) subject;
        } else {
            clazz = subject.getClass();
        }
        try {
            while (clazz != null) {
                try {
                    Field field = clazz.getDeclaredField(fieldName);
                    field.setAccessible(true);
                    field.set(subject, value);
                    break;
                } catch (NoSuchFieldException ex) {
                    if (clazz == clazz.getSuperclass()) {
                        break;
                    }
                    clazz = clazz.getSuperclass();
                }
            }
        } catch (SecurityException | IllegalArgumentException |
                 IllegalAccessException e) {
            throw new TestUtilsException("setField failed", e);
        }
    }

    /**
     * Gets the field, bypassing scope restriction.
     *
     * @param subject   Object where the field belongs
     * @param fieldName name of the field to get
     * @param <T>       subject type
     * @param <U>       fieldO value type
     * @return value of the field.
     * @throws TestUtilsException if there are reflection errors while getting
     *                            the field
     */
    public static <T, U> U getField(T subject, String fieldName)
            throws TestUtilsException {
        try {
            NoSuchFieldException exception = null;
            @SuppressWarnings("unchecked")
            Class clazz;
            if (subject instanceof Class) {
                // Class was given, assuming intention is to deal with static field
                clazz = (Class) subject;
            } else {
                clazz = subject.getClass();
            }
            while (clazz != null) {
                try {
                    Field field = clazz.getDeclaredField(fieldName);
                    field.setAccessible(true);

                    @SuppressWarnings("unchecked")
                    U result = (U) field.get(subject);
                    return result;
                } catch (NoSuchFieldException e) {
                    exception = e;
                    if (clazz == clazz.getSuperclass()) {
                        break;
                    }
                    clazz = clazz.getSuperclass();
                }
            }
            throw new TestUtilsException("Field not found. " + fieldName, exception);

        } catch (SecurityException |
                IllegalArgumentException | IllegalAccessException e) {
            throw new TestUtilsException("getField failed", e);
        }
    }

    /**
     * Calls the method, bypassing scope restriction.
     *
     * @param subject Object where the method belongs
     * @param methodName name of the method to call
     * @param paramTypes formal parameter type array
     * @param args arguments
     * @return return value or null if void
     * @param <T> subject type
     * @param <U> return value type
     * @throws TestUtilsException if there are reflection errors while calling
     * the method
     */
    public static <T, U> U callMethod(T subject, String methodName,
            Class<?>[] paramTypes, Object...args) throws TestUtilsException {

        try {
            @SuppressWarnings("unchecked")
            Class<T> clazz = (Class<T>) subject.getClass();
            final Method method;
            if (paramTypes == null || paramTypes.length == 0) {
                method = clazz.getDeclaredMethod(methodName);
            } else {
                method = clazz.getDeclaredMethod(methodName, paramTypes);
            }
            method.setAccessible(true);

            @SuppressWarnings("unchecked")
            U result = (U) method.invoke(subject, args);
            return result;
        } catch (NoSuchMethodException | SecurityException |
                IllegalAccessException | IllegalArgumentException |
                InvocationTargetException e) {
            throw new TestUtilsException("callMethod failed", e);
        }
    }

    /**
     * Calls the method, bypassing scope restriction.
     *
     * @param subject Object where the method belongs
     * @param methodName name of the method to call
     * @param paramType formal parameter type
     * @param arg argument
     * @return return value or null if void
     * @param <T> subject type
     * @param <U> return value type
     * @throws TestUtilsException if there are reflection errors while calling
     * the method
     */
    public static <T, U> U callMethod(T subject, String methodName,
            Class<?> paramType, Object arg) throws TestUtilsException {
        return callMethod(subject, methodName, new Class<?>[]{paramType}, arg);
    }

    /**
     * Triggers an allocation of an object of type T and forces a call to
     * the private constructor.
     *
     * @param constructor Constructor to call
     * @param <T> type of the object to create
     * @return created object of type T
     * @throws TestUtilsException if there are reflection errors while calling
     * the constructor
     */
    public static <T> T callConstructor(Constructor<T> constructor)
            throws TestUtilsException {
        try {
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (InstantiationException | IllegalAccessException |
                InvocationTargetException error) {
            throw new TestUtilsException("callConstructor failed", error);
        }
    }

    /**
     * Avoid instantiation.
     */
    private TestUtils() {}

    /**
     * Exception that can be thrown if problems are encountered while executing
     * the utility method. These are usually problems accessing fields/methods
     * through reflection. The original exception can be found by examining the
     * cause.
     */
    public static class TestUtilsException extends RuntimeException {

        private static final long serialVersionUID = 1L;

        /**
         * Constructs a new exception with the specified detail message and
         * cause.
         *
         * @param message the detail message
         * @param cause the original cause of this exception
         */
        public TestUtilsException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
