/*
 * Copyright 2016-present Open Networking Laboratory
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.onosproject.yangutils.linker;

import java.util.Map;
import org.onosproject.yangutils.datamodel.YangReferenceResolver;

/**
 * Abstraction of entity which provides linking service of YANG files.
 */
public interface YangLinker {

    /**
     * Resolve the import and include dependencies for a given resolution
     * information.
     *
     * @param fileMapEntry map entry for which resolution is to be done
     * @param yangFilesMap map of dependent file and resolution information*/
    void resolveDependencies(Map.Entry<String, YangReferenceResolver> fileMapEntry, Map<String,
            YangReferenceResolver> yangFilesMap);
}
