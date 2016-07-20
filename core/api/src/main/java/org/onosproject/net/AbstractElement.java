/*
 * Copyright 2014-present Open Networking Laboratory
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
package org.onosproject.net;

import org.onosproject.net.provider.ProviderId;

/**
 * Base implementation of network elements, i.e. devices or hosts.
 */
public abstract class AbstractElement extends AbstractProjectableModel implements Element {

    protected final ElementId id;

    // For serialization
    public AbstractElement() {
        id = null;
    }

    /**
     * Creates a network element attributed to the specified provider.
     *
     * @param providerId  identity of the provider
     * @param id          element identifier
     * @param annotations optional key/value annotations
     */
    protected AbstractElement(ProviderId providerId, ElementId id,
                              Annotations... annotations) {
        super(providerId, annotations);
        this.id = id;
    }

}
