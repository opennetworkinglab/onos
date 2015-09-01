/*
 * Copyright 2015 Open Networking Laboratory
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
package org.onosproject.incubator.net.domain;

import com.google.common.annotations.Beta;

/**
 * The abstract base class for the resource that satisfies an intent primitive.
 */
@Beta
public class IntentResource {

    private final IntentPrimitive primitive;
    private final long tunnelId;
    private final IntentDomainId domainId;

    // TODO add other common fields
    //String ingressTag;
    //String egressTag;
    //etc.

    public IntentResource(IntentPrimitive primitive, long tunnelId, IntentDomainId domainId) {
        this.primitive = primitive;
        this.tunnelId = tunnelId;
        this.domainId = domainId;
    }

    /**
     * Returns the intent primitive associated with this resource as creation.
     *
     * @return this resource's intent primitive
     */
    public IntentPrimitive primitive() {
        return primitive;
    }

    /**
     * Returns the tunnel ID associated with this resource as creation.
     *
     * @return this resource's tunnel ID
     */
    public long tunnelId() {
        return tunnelId;
    }

    /**
     * Returns the domain ID associated with this resource as creation.
     *
     * @return this resource's domain ID
     */
    public IntentDomainId domainId() {
        return domainId;
    }

}
