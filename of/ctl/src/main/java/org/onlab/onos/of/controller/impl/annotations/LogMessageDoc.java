/**
 *    Copyright 2012, Big Switch Networks, Inc.
 *    Originally created by David Erickson, Stanford University
 *
 *    Licensed under the Apache License, Version 2.0 (the "License"); you may
 *    not use this file except in compliance with the License. You may obtain
 *    a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *    WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *    License for the specific language governing permissions and limitations
 *    under the License.
 **/

package org.onlab.onos.of.controller.impl.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * Annotation used to document log messages.  This can be used to generate
 * documentation on syslog output.
 *
 */
@Target({ ElementType.TYPE, ElementType.METHOD })
public @interface LogMessageDoc {
    public static final String NO_ACTION = "No action is required.";
    public static final String UNKNOWN_ERROR = "An unknown error occured";
    public static final String GENERIC_ACTION =
            "Examine the returned error or exception and take " +
                    "appropriate action.";
    public static final String CHECK_SWITCH =
            "Check the health of the indicated switch.  " +
                    "Test and troubleshoot IP connectivity.";
    public static final String CHECK_CONTROLLER =
            "Verify controller system health, CPU usage, and memory.  " +
                    "Rebooting the controller node may help if the controller " +
                    "node is in a distressed state.";
    public static final String REPORT_CONTROLLER_BUG =
            "This is likely a defect in the controller.  Please report this " +
                    "issue.  Restarting the controller or switch may help to " +
                    "alleviate.";
    public static final String REPORT_SWITCH_BUG =
            "This is likely a defect in the switch.  Please report this " +
                    "issue.  Restarting the controller or switch may help to " +
                    "alleviate.";

    /**
     * The log level for the log message.
     *
     * @return the log level as a tring
     */
    String level() default "INFO";

    /**
     * The message that will be printed.
     *
     * @return the message
     */
    String message() default UNKNOWN_ERROR;

    /**
     * An explanation of the meaning of the log message.
     *
     * @return the explanation
     */
    String explanation() default UNKNOWN_ERROR;

    /**
     * The recommendated action associated with the log message.
     *
     * @return the recommendation
     */
    String recommendation() default NO_ACTION;
}
