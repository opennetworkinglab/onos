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
package org.onosproject.drivers.netconf;

import org.onlab.packet.ChassisId;
import org.onosproject.net.Annotations;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.device.DeviceDescription;
import org.onosproject.net.driver.Behaviour;
import org.onosproject.net.provider.ProviderId;

public class MockDevice implements Device {

    private final DeviceDescription desc;
    private final DeviceId id;

    public MockDevice(DeviceId id, DeviceDescription desc) {
        this.desc = desc;
        this.id = id;
    }

    @Override
    public Annotations annotations() {
        return desc.annotations();
    }

    @Override
    public ProviderId providerId() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public <B extends Behaviour> B as(Class<B> projectionClass) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public <B extends Behaviour> boolean is(Class<B> projectionClass) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public DeviceId id() {
        return id;
    }

    @Override
    public Type type() {
        return desc.type();
    }

    @Override
    public String manufacturer() {
        return desc.manufacturer();
    }

    @Override
    public String hwVersion() {
        return desc.hwVersion();
    }

    @Override
    public String swVersion() {
        return desc.swVersion();
    }

    @Override
    public String serialNumber() {
        return desc.serialNumber();
    }

    @Override
    public ChassisId chassisId() {
        return desc.chassisId();
    }

}
