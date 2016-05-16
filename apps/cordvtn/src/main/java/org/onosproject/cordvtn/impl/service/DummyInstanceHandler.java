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
package org.onosproject.cordvtn.impl.service;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;

import org.apache.felix.scr.annotations.Deactivate;
import org.onosproject.cordvtn.api.Instance;
import org.onosproject.cordvtn.api.InstanceHandler;
import org.onosproject.cordvtn.impl.CordVtnInstanceHandler;
import org.onosproject.xosclient.api.VtnService;

import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;
import static org.onlab.util.Tools.groupedThreads;

/**
 * Provides network connectivity for dummy service instances.
 */
@Component(immediate = true)
public class DummyInstanceHandler extends CordVtnInstanceHandler implements InstanceHandler {

    @Activate
    protected void activate() {
        serviceType = VtnService.ServiceType.DUMMY;
        eventExecutor = newSingleThreadScheduledExecutor(groupedThreads("onos/cordvtn-dummy", "event-handler"));
        super.activate();
    }

    @Deactivate
    protected void deactivate() {
        super.deactivate();
    }

    @Override
    public void instanceDetected(Instance instance) {
        super.instanceDetected(instance);
    }

    @Override
    public void instanceRemoved(Instance instance) {
        super.instanceRemoved(instance);
    }
}
