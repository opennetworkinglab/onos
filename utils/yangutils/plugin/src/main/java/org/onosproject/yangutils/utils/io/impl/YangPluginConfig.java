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

package org.onosproject.yangutils.utils.io.impl;

/**
 * Representation of plugin configurations required for YANG utils.
 */
public final class YangPluginConfig {

    /**
     * Contains the code generation directory.
     */
    private String codeGenDir;

    /**
     * Contains information of naming conflicts that can be resolved.
     */
    private YangToJavaNamingConflictUtil conflictResolver;

    /**
     * Creates an object for YANG plugin config.
     */
    public YangPluginConfig() {
    }

    /**
     * Sets the path of the java code where it has to be generated.
     *
     * @param codeGenDir path of the directory
     */
    public void setCodeGenDir(String codeGenDir) {
        this.codeGenDir = codeGenDir;
    }

    /**
     * Returns the code generation directory path.
     *
     * @return code generation directory
     */
    public String getCodeGenDir() {
        return codeGenDir;
    }

    /**
     * Sets the object.
     *
     * @param conflictResolver object of the class
     */
    public void setConflictResolver(YangToJavaNamingConflictUtil conflictResolver) {
        this.conflictResolver = conflictResolver;
    }

    /**
     * Returns the object.
     *
     * @return object of the class
     */
    public YangToJavaNamingConflictUtil getConflictResolver() {
        return conflictResolver;
    }
}
