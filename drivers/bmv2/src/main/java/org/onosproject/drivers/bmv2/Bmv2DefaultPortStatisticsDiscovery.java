/*
 * Copyright 2017-present Open Networking Foundation
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

package org.onosproject.drivers.bmv2;

import com.google.common.collect.ImmutableList;
import org.onosproject.net.device.PortStatistics;
import org.onosproject.net.device.PortStatisticsDiscovery;
import org.onosproject.net.driver.AbstractHandlerBehaviour;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

/**
 * Implementation of the behaviour for discovering the port statistics of a Bmv2 device with the default.p4 program.
 */
public class Bmv2DefaultPortStatisticsDiscovery extends AbstractHandlerBehaviour implements PortStatisticsDiscovery {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Override
    public Collection<PortStatistics> discoverPortStatistics() {
        log.debug("Discovering Port Statistics for device {}", handler().data().deviceId());
        return ImmutableList.of();
    }
}
