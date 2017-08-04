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

/**
 * Auxiliary mechanism for resolving model IDs into class-loaders from where
 * the model was registered to allow access to model-specific resources.
 *
 * Note: Consider promoting this to ONOS YANG Tools repo.
 */
@Beta
public interface YangClassLoaderRegistry {

    /**
     * Get the class loader registered for the given model id.
     *
     * @param modelId model identifier
     * @return class loader registered for the model
     */
    ClassLoader getClassLoader(String modelId);

    /**
     * Register the class loader for the specified model.
     *
     * @param modelId     model identifier
     * @param classLoader class loader
     */
    void registerClassLoader(String modelId, ClassLoader classLoader);

    /**
     * Register the class loader for the specified model.
     *
     * @param modelId model identifier
     */
    void unregisterClassLoader(String modelId);
}
