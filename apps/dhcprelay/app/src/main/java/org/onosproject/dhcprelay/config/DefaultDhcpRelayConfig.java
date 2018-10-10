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
 *
 */
package org.onosproject.dhcprelay.config;

import com.google.common.collect.Lists;
import org.onosproject.core.ApplicationId;
import org.onosproject.net.config.Config;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * DHCP Relay Config class for default use case (directly connected hosts).
 */
public class DefaultDhcpRelayConfig extends Config<ApplicationId> {
    public static final String KEY = "default";

    @Override
    public boolean isValid() {
        // check if all configs are valid
        AtomicBoolean valid = new AtomicBoolean(true);
        array.forEach(config -> valid.compareAndSet(true, DhcpServerConfig.isValid(config)));
        return valid.get();
    }

    public List<DhcpServerConfig> dhcpServerConfigs() {
        List<DhcpServerConfig> configs = Lists.newArrayList();
        array.forEach(node -> configs.add(new DhcpServerConfig(node)));
        return configs;
    }
}
