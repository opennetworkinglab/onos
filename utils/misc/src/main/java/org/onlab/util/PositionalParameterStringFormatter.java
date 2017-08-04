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
package org.onlab.util;

/**
 * Allows slf4j style formatting of parameters into a string.
 */
public final class PositionalParameterStringFormatter {

    /**
     * Hide default constructor.
     */
    private PositionalParameterStringFormatter() {
    }

    /**
     * Formats a string using slf4j style positional parameter replacement.
     * Instances of "{}" in the source string are replaced in order by the
     * specified parameter values as strings.
     *
     * @param source original string to format
     * @param parameters list of parameters that will be substituted
     * @return formatted string
     */
    public static String format(String source, Object... parameters) {
        String current = source;
        for (Object parameter : parameters) {
            if (!current.contains("{}")) {
                return current;
            }
            current = current.replaceFirst("\\{\\}", String.valueOf(parameter));
        }
        return current;
    }
}
