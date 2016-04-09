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
 * Base implementation of a network model entity.
 */
public abstract class AbstractModel extends AbstractAnnotated implements Provided {

    private final ProviderId providerId;

    // For serialization
    public AbstractModel() {
        providerId = null;
    }

    /**
     * Creates a model entity attributed to the specified provider and
     * optionally annotated.
     *
     * @param providerId  identity of the provider
     * @param annotations optional key/value annotations
     */
    protected AbstractModel(ProviderId providerId, Annotations... annotations) {
        super(annotations);
        this.providerId = providerId;
    }

    @Override
    public ProviderId providerId() {
        return providerId;
    }

}
