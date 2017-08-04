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
package org.onosproject.net.utils;

import org.onlab.packet.ChassisId;
import org.onosproject.net.Annotations;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.driver.Behaviour;
import org.onosproject.net.provider.ProviderId;

import com.google.common.annotations.Beta;

/**
 * A Device which forwards all its method calls to another Device.
 */
@Beta
public interface ForwardingDevice extends Device {

    Device delegate();

    @Override
    default Annotations annotations() {
        return delegate().annotations();
    }

    @Override
    default ProviderId providerId() {
        return delegate().providerId();
    }

    @Override
    default <B extends Behaviour> B as(Class<B> projectionClass) {
        return delegate().as(projectionClass);
    }

    @Override
    default DeviceId id() {
        return delegate().id();
    }

    @Override
    default Type type() {
        return delegate().type();
    }

    @Override
    default <B extends Behaviour> boolean is(Class<B> projectionClass) {
        return delegate().is(projectionClass);
    }

    @Override
    default String manufacturer() {
        return delegate().manufacturer();
    }

    @Override
    default String hwVersion() {
        return delegate().hwVersion();
    }

    @Override
    default String swVersion() {
        return delegate().swVersion();
    }

    @Override
    default String serialNumber() {
        return delegate().serialNumber();
    }

    @Override
    default ChassisId chassisId() {
        return delegate().chassisId();
    }
}
