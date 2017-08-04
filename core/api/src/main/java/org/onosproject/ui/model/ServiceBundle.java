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

package org.onosproject.ui.model;

import org.onosproject.cluster.ClusterService;
import org.onosproject.mastership.MastershipService;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.host.HostService;
import org.onosproject.net.intent.IntentService;
import org.onosproject.net.link.LinkService;
import org.onosproject.net.region.RegionService;
import org.onosproject.ui.UiTopoLayoutService;

/**
 * A bundle of services to pass to elements that might need a reference to them.
 */
public interface ServiceBundle {

    /**
     * Reference to a UI Topology Layout service implementation.
     *
     * @return layout service
     */
    UiTopoLayoutService layout();

    /**
     * Reference to a cluster service implementation.
     *
     * @return cluster service
     */
    ClusterService cluster();

    /**
     * Reference to a mastership service implementation.
     *
     * @return mastership service
     */
    MastershipService mastership();

    /**
     * Reference to a region service implementation.
     *
     * @return region service
     */
    RegionService region();

    /**
     * Reference to a device service implementation.
     *
     * @return device service
     */
    DeviceService device();

    /**
     * Reference to a link service implementation.
     *
     * @return link service
     */
    LinkService link();

    /**
     * Reference to a host service implementation.
     *
     * @return host service
     */
    HostService host();

    /**
     * Reference to a intent service implementation.
     *
     * @return intent service
     */
    IntentService intent();

    /**
     * Reference to a flow service implementation.
     *
     * @return flow service
     */
    FlowRuleService flow();
}
