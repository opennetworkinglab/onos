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
package org.onosproject.yangutils.translator.tojava;

/**
 * Represents the information of the java import data.
 */
public interface JavaImportDataContainer {

    /**
     * Returns the data of java imports to be included in generated file.
     *
     * @return data of java imports to be included in generated file
     */
    JavaImportData getJavaImportData();

    /**
     * Sets the data of java imports to be included in generated file.
     *
     * @param javaImportData data of java imports to be included in generated
     *            file
     */
    void setJavaImportData(JavaImportData javaImportData);
}
