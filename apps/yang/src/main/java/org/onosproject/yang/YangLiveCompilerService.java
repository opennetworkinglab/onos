/*
 * Copyright 2017-present Open Networking Foundation
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

package org.onosproject.yang;

import com.google.common.annotations.Beta;

import java.io.IOException;
import java.io.InputStream;

/**
 * Runtime service for compiling YANG source files and packaging the compiled
 * artifacts into a ready-to-load application.
 */
@Beta
public interface YangLiveCompilerService {

    /**
     * Compiles the YANG source file(s) contained in the specified input
     * stream either as individual YANG source file, or as a JAR/ZIP collection
     * of YANG source files.
     *
     * @param modelId     model identifier
     * @param yangSources input stream containing a single YANG source file
     *                    or a JAR/ZIP archive containing collection of YANG
     *                    source files
     * @return input stream containing a packaged ONOS application JAR file
     * @throws IOException if issues arise when reading the yang sources stream
     */
    InputStream compileYangFiles(String modelId, InputStream yangSources) throws IOException;

}
