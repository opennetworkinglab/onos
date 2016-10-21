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
 * Representation of a UnnumberedLink as a TE LSP route element.
 */
public interface TeRouteUnnumberedLink extends TeRouteSubobject {

    /**
     * Returns node of this route subobject.
     *
     * @return TE node key
     */
    TeNodeKey node();

    /**
     * Returns termination point of this route subobject.
     *
     * @return TE termination point key
     */
    TtpKey ttp();
}
