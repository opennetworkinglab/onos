/*
 * Copyright 2016-present Open Networking Laboratory
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
package org.onosproject.roadm;

import org.onlab.util.Frequency;
import org.onosproject.net.Annotations;

import static com.google.common.base.Strings.isNullOrEmpty;

/**
 * Roadm utilities.
 */
public final class RoadmUtil {

    public static final String DEV_ID = "devId";
    public static final String VALID = "valid";
    public static final String MESSAGE = "message";
    public static final String NA = "N/A";
    public static final String UNKNOWN = "Unknown";
    public static final String NO_ROWS_MESSAGE = "No items found";
    // Optical protection switch operations.
    // There are 3 operations for protection switch now: AUTOMATIC, FORCE, MANUAL.
    public static final String OPS_OPT_AUTO = "AUTOMATIC";
    public static final String OPS_OPT_FORCE = "FORCE";
    public static final String OPS_OPT_MANUAL = "MANUAL";

    private RoadmUtil() {
    }

    /**
     * Formats Hz to GHz.
     *
     * @param value Hz in string format
     * @return GHz in string format
     */
    public static String asGHz(Frequency value) {
        return value == null ? UNKNOWN : String.valueOf(value.asGHz());
    }

    /**
     * Formats Hz to THz.
     *
     * @param value Hz in string format
     * @return THz in string format
     */
    public static String asTHz(Frequency value) {
        return value == null ? UNKNOWN : String.valueOf(value.asTHz());
    }

    /**
     * Gives a default value if the string is null or empty.
     *
     * @param value the string value
     * @param defaultValue default value if null or empty
     * @return processed string
     */
    public static String defaultString(String value, String defaultValue) {
        return isNullOrEmpty(value) ? defaultValue : value;
    }

    /**
     * Gives a default value if the object is null.
     *
     * @param object the object
     * @param defaultValue default value if null
     * @return processed string
     */
    public static String objectToString(Object object, String defaultValue) {
        return object == null ? defaultValue : String.valueOf(object);
    }

    /**
     * Gets value from annotations, if not exists, return default value.
     *
     * @param annotations the annotations
     * @param key key value
     * @param defaultValue default value
     * @return value in string format
     */
    public static String getAnnotation(Annotations annotations, String key, String defaultValue) {
        return defaultString(annotations.value(key), defaultValue);
    }

    /**
     * Gets value from annotations, default value is NA.
     *
     * @param annotations the annotations
     * @param key key value
     * @return value in string format
     */
    public static String getAnnotation(Annotations annotations, String key) {
        return getAnnotation(annotations, key, NA);
    }
}