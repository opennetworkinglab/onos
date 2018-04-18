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

package org.onosproject.layout;

import org.onosproject.net.config.basics.BasicDeviceConfig;
import org.onosproject.net.config.basics.BasicHostConfig;

/**
 * Default force layout leaving the client to automatically position nodes.
 */
public class DefaultForceLayout extends LayoutAlgorithm {

    @Override
    public void apply() {
        hostService.getHosts()
                .forEach(h -> netConfigService.addConfig(h.id(), BasicHostConfig.class)
                        .gridX(null).gridY(null).locType(null).apply());
        deviceService.getDevices()
                .forEach(d -> netConfigService.addConfig(d.id(), BasicDeviceConfig.class)
                        .gridX(null).gridY(null).locType(null).apply());
    }
}
