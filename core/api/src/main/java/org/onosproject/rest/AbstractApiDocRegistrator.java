/*
 * Copyright 2015-present Open Networking Laboratory
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
package org.onosproject.rest;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;

/**
 * Self-registering REST API provider.
 */
@Component(immediate = true, componentAbstract = true)
public abstract class AbstractApiDocRegistrator {

    protected final ApiDocProvider provider;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ApiDocService service;

    /**
     * Creates registrator for the specified REST API doc provider.
     *
     * @param provider REST API provider
     */
    protected AbstractApiDocRegistrator(ApiDocProvider provider) {
        this.provider = provider;
    }

    @Activate
    protected void activate() {
        service.register(provider);
    }

    @Deactivate
    protected void deactivate() {
        service.unregister(provider);
    }
}
