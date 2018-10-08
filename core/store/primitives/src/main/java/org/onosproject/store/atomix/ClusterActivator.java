/*
 * Copyright 2018-present Open Networking Foundation
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

package org.onosproject.store.atomix;

import org.onosproject.component.ComponentService;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;

import static org.slf4j.LoggerFactory.getLogger;

@Component(immediate = true, service = ClusterActivator.class)
public class ClusterActivator {
    private final Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    private ComponentService componentService;

    @Activate
    public void activate() {
        log.info("Started");
    }

    /**
     * Resources needed by the cluster are now available and the Atomix
     * cluster can be formed.
     */
    public void activateCluster() {
        componentService.activate(null, "org.onosproject.store.atomix.impl.AtomixManager");
    }
}
