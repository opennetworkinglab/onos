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

package org.onosproject.netcfgmonitor;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.onosproject.net.config.NetworkConfigEvent;
import org.onosproject.net.config.NetworkConfigListener;
import org.onosproject.net.config.NetworkConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Network configuration monitor.
 */
@Component(immediate = true)
public class NetCfgEventMonitor {

    private Logger log = LoggerFactory.getLogger(getClass());

    @Reference
    protected NetworkConfigService networkConfigService;

    private final NetworkConfigListener listener = new InternalListener();

    @Activate
    protected void activate() {
        networkConfigService.addListener(listener);
    }

    @Deactivate
    protected void deactivate() {
        networkConfigService.removeListener(listener);
    }

    private class InternalListener implements NetworkConfigListener {
        @Override
        public void event(NetworkConfigEvent event) {
            log.info("Received event {}", event);
        }
    }
}
