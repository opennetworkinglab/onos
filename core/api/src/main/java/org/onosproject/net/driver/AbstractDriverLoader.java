/*
 * Copyright 2016-present Open Networking Foundation
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
package org.onosproject.net.driver;

import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;

/**
 * Abstract bootstrapper for loading and registering driver definitions that
 * are dependent on the default driver definitions.
 */
public abstract class AbstractDriverLoader extends AbstractIndependentDriverLoader {

    // FIXME: This requirement should be removed and the driver extensions that
    // depend on the default drivers being loaded should be modified to instead
    // express the dependency using the application dependency mechanism.
    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected DefaultDriverProviderService defaultDriverProviderService;

    /**
     * Creates a new loader for resource with the specified path.
     *
     * @param path drivers definition XML resource path
     */
    protected AbstractDriverLoader(String path) {
        super(path);
    }

}
