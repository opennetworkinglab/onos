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

package org.onosproject.drivers.fujitsu;

/**
 * Defines common XML constants and methods for Fujitsu vOLT.
 */
public final class FujitsuVoltXmlUtility {

    public static final String COLON = ":";
    public static final String HYPHEN = "-";
    public static final String SLASH = "/";
    public static final String SPACE = " ";
    public static final String NEW_LINE = "\n";
    public static final String ANGLE_LEFT = "<";
    public static final String ANGLE_RIGHT = ">";

    public static final String REPORT_ALL = "report-all";
    public static final String EDIT_CONFIG = "edit-config";
    public static final String RUNNING = "running";

    public static final String VOLT_NE_NAMESPACE =
            "xmlns=\"http://fujitsu.com/ns/volt/1.1\"";
    public static final String VOLT_NE = "volt-ne";
    public static final String PONLINK_ID = "ponlink-id";
    public static final String ONU_ID = "onu-id";

    public static final String VOLT_NE_OPEN = ANGLE_LEFT + VOLT_NE + SPACE;
    public static final String VOLT_NE_CLOSE = ANGLE_LEFT + SLASH + VOLT_NE + ANGLE_RIGHT;

    public static final int FIRST_PART = 0;
    public static final int SECOND_PART = 1;
    public static final int THIRD_PART = 2;
    public static final int ZERO = 0;
    public static final int ONE = 1;
    public static final int TWO = 2;
    public static final int THREE = 3;

    private FujitsuVoltXmlUtility() {
        // Preventing any allocation
    }

    /**
     * Builds XML start tag with name provided.
     *
     * @param name tag name
     * @return string
     */
    public static String buildStartTag(String name) {
        return buildStartTag(name, true);
    }

    /**
     * Builds XML end tag with name provided.
     *
     * @param name tag name
     * @return string
     */
    public static String buildEndTag(String name) {
        return buildEndTag(name, true);
    }

    /**
     * Builds XML empty tag with name provided.
     *
     * @param name tag name
     * @return string
     */
    public static String buildEmptyTag(String name) {
        return buildEmptyTag(name, true);
    }

    /**
     * Builds XML start tag with name provided.
     *
     * @param name tag name
     * @param addNewLine option to add new line character after tag
     * @return string
     */
    public static String buildStartTag(String name, boolean addNewLine) {
        if (addNewLine) {
            return (ANGLE_LEFT + name + ANGLE_RIGHT + NEW_LINE);
        } else {
            return (ANGLE_LEFT + name + ANGLE_RIGHT);
        }
    }

    /**
     * Builds XML end tag with name provided.
     *
     * @param name tag name
     * @param addNewLine option to add new line character after tag
     * @return string
     */
    public static String buildEndTag(String name, boolean addNewLine) {
        if (addNewLine) {
            return (ANGLE_LEFT + SLASH + name + ANGLE_RIGHT + NEW_LINE);
        } else {
            return (ANGLE_LEFT + SLASH + name + ANGLE_RIGHT);
        }
    }

    /**
     * Builds XML empty element tag with name provided.
     *
     * @param name tag name
     * @param addNewLine option to add new line character after tag
     * @return string
     */
    public static String buildEmptyTag(String name, boolean addNewLine) {
        if (addNewLine) {
            return (ANGLE_LEFT + name + SLASH + ANGLE_RIGHT + NEW_LINE);
        } else {
            return (ANGLE_LEFT + name + SLASH + ANGLE_RIGHT);
        }
    }

}
