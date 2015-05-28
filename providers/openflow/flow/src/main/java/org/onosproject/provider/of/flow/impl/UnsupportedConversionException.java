/*
 * Copyright 2015 Open Networking Laboratory
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
package org.onosproject.provider.of.flow.impl;

/**
 * Thrown to indicate that unsupported conversion occurs.
 */
public class UnsupportedConversionException extends RuntimeException {
    /**
     * Creates an instance with the specified values.
     *
     * @param input input value of conversion causing this exception
     * @param output the desired class which the input value is converted to
     */
    public UnsupportedConversionException(Object input, Class<?> output) {
        super(String.format("No mapping found for %s when converting to %s", input, output.getName()));
    }
}
