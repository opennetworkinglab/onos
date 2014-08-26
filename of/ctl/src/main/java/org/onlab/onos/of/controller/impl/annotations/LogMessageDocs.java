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
 * documentation on syslog output.  This version allows multiple log messages
 * to be documentated on an interface.
 *
 */
@Target({ ElementType.TYPE, ElementType.METHOD })
public @interface LogMessageDocs {
    /**
     * A list of {@link LogMessageDoc} elements.
     *
     * @return the list of log message doc
     */
    LogMessageDoc[] value();
}
