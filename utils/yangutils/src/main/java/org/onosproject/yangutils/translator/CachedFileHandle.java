/*
 * Copyright 2016 Open Networking Laboratory
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

package org.onosproject.yangutils.translator;

import java.io.IOException;

import org.onosproject.yangutils.datamodel.YangType;

/**
 * Cached java file handle, which supports the addition of member attributes and
 * methods.
 */
public interface CachedFileHandle {

    /**
     * Add a new attribute to the file(s).
     *
     * @param attrType data type of the added attribute
     * @param name name of the attribute
     * @param isListAttr if the current added attribute needs to be maintained
     *            in a list
     */
    void addAttributeInfo(YangType<?> attrType, String name, boolean isListAttr);

    /**
     * Flushes the cached contents to the target file, frees used resources.
     *
     * @throws IOException when failes to generated java files
     */
    void close() throws IOException;

    /**
     * Sets directory package path for code generation.
     *
     * @param filePath directory package path for code generation
     */
    void setRelativeFilePath(String filePath);

    /**
     * Gets directory package path for code generation.
     *
     * @return directory package path for code generation
     */
    String getRelativeFilePath();
}
