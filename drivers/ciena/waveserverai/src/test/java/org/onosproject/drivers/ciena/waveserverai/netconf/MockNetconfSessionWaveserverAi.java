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
package org.onosproject.drivers.ciena.waveserverai.netconf;

import java.util.concurrent.atomic.AtomicInteger;

import org.onosproject.netconf.NetconfDeviceInfo;
import org.onosproject.netconf.NetconfException;
import org.onosproject.netconf.NetconfSessionAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MockNetconfSessionWaveserverAi extends NetconfSessionAdapter {
    private static final Logger log = LoggerFactory
            .getLogger(MockNetconfSessionWaveserverAi.class);

    private NetconfDeviceInfo deviceInfo;

    private final AtomicInteger messageIdInteger = new AtomicInteger(0);

    public MockNetconfSessionWaveserverAi(NetconfDeviceInfo deviceInfo) throws NetconfException {
        this.deviceInfo = deviceInfo;
    }
}
