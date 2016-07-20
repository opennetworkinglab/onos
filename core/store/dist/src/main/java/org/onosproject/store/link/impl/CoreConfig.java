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
package org.onosproject.store.link.impl;

import org.onosproject.core.ApplicationId;
import org.onosproject.net.config.Config;

public class CoreConfig extends Config<ApplicationId> {

    private static final String LINK_DISCOVERY_MODE = "linkDiscoveryMode";

    protected static final String DEFAULT_LINK_DISCOVERY_MODE = "PERMISSIVE";

    /**
     * Returns the link discovery mode.
     *
     * @return link discovery mode
     */
    public ECLinkStore.LinkDiscoveryMode linkDiscoveryMode() {
        return ECLinkStore.LinkDiscoveryMode
                .valueOf(get(LINK_DISCOVERY_MODE, DEFAULT_LINK_DISCOVERY_MODE));
    }
}

