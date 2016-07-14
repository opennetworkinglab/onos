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
package org.onosproject.incubator.net.domain;

import com.google.common.annotations.Beta;
import org.onosproject.core.ApplicationId;
import org.onosproject.net.ConnectPoint;


/**
 * The abstract base class for the resource that satisfies an intent primitive.
 */
@Beta
public abstract class IntentResource {

    private final IntentPrimitive primitive;

    private final ApplicationId appId;
    private final ConnectPoint ingress;
    private final ConnectPoint egress;

    //* QUESTIONABLE ADDITIONS *//

    // TODO add other common fields
    //String ingressTag;
    //String egressTag;
    //etc.

    public IntentResource(IntentPrimitive primitive, ApplicationId appId,
                          ConnectPoint ingress, ConnectPoint egress) {
        this.appId = appId;
        this.ingress = ingress;
        this.egress = egress;
        this.primitive = primitive;
    }

    //TODO when is same package tunnelID should be of type tunnelID and netTunnelId not long.


    /**
     * Returns the intent primitive associated with this resource at creation.
     *
     * @return this resource's intent primitive.
     */
    public IntentPrimitive primitive() {
        return primitive;
    }

    /**
     * Returns the application ID associated with this resource at creation.
     *
     * @return this resource's application ID.
     */
    public ApplicationId appId() {
        return appId;
    }

    /**
     * Returns the ingress connect point associated with this resource at creation.
     *
     * @return this resource's ingress connect point.
     */
    public ConnectPoint ingress() {
        return ingress;
    }

    /**
     * Returns the egress connect point associated with this resource at creation.
     *
     * @return this resource's connect point.
     */
    public ConnectPoint egress() {
        return egress;
    }
}
