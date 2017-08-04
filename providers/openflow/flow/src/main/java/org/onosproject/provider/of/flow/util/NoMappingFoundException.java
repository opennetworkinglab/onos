/*
 * Copyright 2016-present Open Networking Foundation
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
package org.onosproject.provider.of.flow.util;

/**
 * Thrown to indicate that no mapping for the input value is found.
 */
public class NoMappingFoundException extends RuntimeException {
    /**
     * Creates an instance with the specified values.
     *
     * @param input input value of mapping causing this exception
     * @param output the desired class which the input value is mapped to
     */
    public NoMappingFoundException(Object input, Class<?> output) {
        super(String.format("No mapping found for %s when converting to %s", input, output.getName()));
    }
}
