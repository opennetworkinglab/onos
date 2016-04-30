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
package org.onosproject.yangutils.translator.tojava.javamodel;

import org.onosproject.yangutils.datamodel.YangUses;
import org.onosproject.yangutils.translator.tojava.JavaCodeGenerator;
import org.onosproject.yangutils.translator.tojava.utils.YangPluginConfig;

/**
 * Represents uses information extended to support java code generation.
 */
public class YangJavaUses
        extends YangUses
        implements JavaCodeGenerator {

    /**
     * Creates YANG java uses object.
     */
    public YangJavaUses() {
        super();
    }

    /**
     * Prepare the information for java code generation corresponding to YANG
     * uses info.
     *
     * @param yangPlugin YANG plugin config
     */
    @Override
    public void generateCodeEntry(YangPluginConfig yangPlugin) {
                /*Do nothing, the uses will copy the contents to the used location*/
    }

    /**
     * Create a java file using the YANG uses info.
     */
    @Override
    public void generateCodeExit() {
                /*Do nothing, the uses will copy the contents to the used location*/
    }
}
