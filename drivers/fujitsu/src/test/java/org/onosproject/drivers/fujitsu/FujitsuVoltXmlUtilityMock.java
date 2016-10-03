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
 * Mock FujitsuVoltXmlUtility.
 * This is to avoid using the same names/definitions
 * in FujitsuVoltXmlUtility in test codes - not tied to actual codes.
 */
final class FujitsuVoltXmlUtilityMock {

    public static final String TEST_COLON = ":";
    public static final String TEST_DOT = ".";
    public static final String TEST_HYPHEN = "-";
    public static final String TEST_SLASH = "/";
    public static final String TEST_SPACE = " ";
    public static final String TEST_NEW_LINE = "\n";
    public static final String TEST_ANGLE_LEFT = "<";
    public static final String TEST_ANGLE_RIGHT = ">";

    public static final String TEST_REPORT_ALL = "report-all";
    public static final String TEST_RUNNING = "running";

    public static final String TEST_VOLT_NE_NAMESPACE =
            "xmlns=\"http://fujitsu.com/ns/volt/1.1\"";
    public static final String TEST_VOLT_NE = "volt-ne";
    public static final String TEST_PONLINK_ID = "ponlink-id";
    public static final String TEST_ONU_ID = "onu-id";
    public static final String TEST_ROOT = "fakeroot";

    public static final String TEST_VOLT_NE_OPEN = TEST_ANGLE_LEFT + TEST_VOLT_NE + TEST_SPACE;
    public static final String TEST_VOLT_NE_CLOSE = TEST_ANGLE_LEFT + TEST_SLASH + TEST_VOLT_NE + TEST_ANGLE_RIGHT;

    public static final String TEST_BASE_NAMESPACE =
            " xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0\">\n";

    public static final String TEST_DUPLICATE_SPACES_REGEX = " +";
    public static final String TEST_WHITESPACES_REGEX = "\\s+";
    public static final String TEST_EMPTY_STRING = "";

    public static final String TEST_VOLT_NAMESPACE = TEST_VOLT_NE_OPEN +
            TEST_VOLT_NE_NAMESPACE;

    public static final int FIRST_PART = 0;
    public static final int SECOND_PART = 1;
    public static final int THIRD_PART = 2;
    public static final int ZERO = 0;
    public static final int ONE = 1;
    public static final int THREE = 3;

    private FujitsuVoltXmlUtilityMock() {
    }

    /**
     * Builds XML start tag with name provided.
     *
     * @param name tag name
     * @return string
     */
    public static String startTag(String name) {
        return startTag(name, true);
    }

    /**
     * Builds XML end tag with name provided.
     *
     * @param name tag name
     * @return string
     */
    public static String endTag(String name) {
        return endTag(name, true);
    }

    /**
     * Builds XML empty tag with name provided.
     *
     * @param name tag name
     * @return string
     */
    public static String emptyTag(String name) {
        return emptyTag(name, true);
    }

    /**
     * Builds XML start tag with name provided.
     *
     * @param name tag name
     * @param addNewLine option to add new line character after tag
     * @return string
     */
    public static String startTag(String name, boolean addNewLine) {
        if (addNewLine) {
            return (TEST_ANGLE_LEFT + name + TEST_ANGLE_RIGHT + TEST_NEW_LINE);
        } else {
            return (TEST_ANGLE_LEFT + name + TEST_ANGLE_RIGHT);
        }
    }

    /**
     * Builds XML end tag with name provided.
     *
     * @param name tag name
     * @param addNewLine option to add new line character after tag
     * @return string
     */
    public static String endTag(String name, boolean addNewLine) {
        if (addNewLine) {
            return (TEST_ANGLE_LEFT + TEST_SLASH + name + TEST_ANGLE_RIGHT + TEST_NEW_LINE);
        } else {
            return (TEST_ANGLE_LEFT + TEST_SLASH + name + TEST_ANGLE_RIGHT);
        }
    }

    /**
     * Builds XML empty element tag with name provided.
     *
     * @param name tag name
     * @param addNewLine option to add new line character after tag
     * @return string
     */
    public static String emptyTag(String name, boolean addNewLine) {
        if (addNewLine) {
            return (TEST_ANGLE_LEFT + name + TEST_SLASH + TEST_ANGLE_RIGHT + TEST_NEW_LINE);
        } else {
            return (TEST_ANGLE_LEFT + name + TEST_SLASH + TEST_ANGLE_RIGHT);
        }
    }

}
