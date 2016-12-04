/*
 * Copyright 2016-present Open Networking Laboratory
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

package org.onosproject.tetunnel.api.tunnel.path;

import org.onosproject.tetopology.management.api.node.TeNodeKey;
import org.onosproject.tetopology.management.api.node.TtpKey;

/**
 * Default implementation of a TE unnumbered link route object.
 */
public class DefaultTeRouteUnnumberedLink implements TeRouteUnnumberedLink {

    private final TeNodeKey node;
    private final TtpKey ttp;

    /**
     * Creates a default implementation of a TE unnumbered link route object.
     *
     * @param node key of TE node of the route subobject
     * @param ttp  key of TE termination point of the route subobject
     */
    public DefaultTeRouteUnnumberedLink(TeNodeKey node, TtpKey ttp) {
        this.node = node;
        this.ttp = ttp;
    }

    @Override
    public Type type() {
        return Type.UNNUMBERED_LINK;
    }

    @Override
    public TeNodeKey node() {
        return node;
    }

    @Override
    public TtpKey ttp() {
        return ttp;
    }
}
