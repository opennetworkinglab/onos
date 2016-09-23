/*
 * Copyright 2015-present Open Networking Laboratory
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

package org.onosproject.store.serializers;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import org.onlab.osgi.DefaultServiceDirectory;
import org.onlab.util.ItemNotFoundException;
import org.onosproject.net.DeviceId;
import org.onosproject.net.behaviour.ExtensionSelectorResolver;
import org.onosproject.net.driver.DefaultDriverData;
import org.onosproject.net.driver.DefaultDriverHandler;
import org.onosproject.net.driver.DriverHandler;
import org.onosproject.net.driver.DriverService;
import org.onosproject.net.flow.criteria.Criteria;
import org.onosproject.net.flow.criteria.ExtensionCriterion;
import org.onosproject.net.flow.criteria.ExtensionSelector;
import org.onosproject.net.flow.criteria.ExtensionSelectorType;
import org.onosproject.net.flow.criteria.UnresolvedExtensionSelector;

/**
 * Serializer for extension criteria.
 */
public class ExtensionCriterionSerializer extends Serializer<ExtensionCriterion> {

    /**
     * Constructs a extension criterion serializer.
     */
    public ExtensionCriterionSerializer() {
        super(false, true);
    }

    @Override
    public void write(Kryo kryo, Output output, ExtensionCriterion object) {
        kryo.writeClassAndObject(output, object.extensionSelector().type());
        kryo.writeClassAndObject(output, object.deviceId());
        kryo.writeClassAndObject(output, object.extensionSelector().serialize());
    }

    @Override
    public ExtensionCriterion read(Kryo kryo, Input input,
            Class<ExtensionCriterion> type) {
        ExtensionSelectorType exType = (ExtensionSelectorType) kryo.readClassAndObject(input);
        DeviceId deviceId = (DeviceId) kryo.readClassAndObject(input);
        DriverService driverService = DefaultServiceDirectory.getService(DriverService.class);
        byte[] bytes = (byte[]) kryo.readClassAndObject(input);
        ExtensionSelector selector;

        try {
            DriverHandler handler = new DefaultDriverHandler(
                    new DefaultDriverData(driverService.getDriver(deviceId), deviceId));
            ExtensionSelectorResolver resolver = handler.behaviour(ExtensionSelectorResolver.class);
            selector = resolver.getExtensionSelector(exType);
            selector.deserialize(bytes);
        } catch (ItemNotFoundException | IllegalArgumentException e) {
            selector = new UnresolvedExtensionSelector(bytes, exType);
        }

        return Criteria.extension(selector, deviceId);
    }
}
