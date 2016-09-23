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
import org.onosproject.net.behaviour.ExtensionTreatmentResolver;
import org.onosproject.net.driver.DefaultDriverData;
import org.onosproject.net.driver.DefaultDriverHandler;
import org.onosproject.net.driver.DriverHandler;
import org.onosproject.net.driver.DriverService;
import org.onosproject.net.flow.instructions.ExtensionTreatment;
import org.onosproject.net.flow.instructions.ExtensionTreatmentType;
import org.onosproject.net.flow.instructions.Instructions;
import org.onosproject.net.flow.instructions.UnresolvedExtensionTreatment;

/**
 * Serializer for extension instructions.
 */
public class ExtensionInstructionSerializer extends
        Serializer<Instructions.ExtensionInstructionWrapper> {

    /**
     * Constructs a extension instruction serializer.
     */
    public ExtensionInstructionSerializer() {
        super(false, true);
    }

    @Override
    public void write(Kryo kryo, Output output, Instructions.ExtensionInstructionWrapper object) {
        kryo.writeClassAndObject(output, object.extensionInstruction().type());
        kryo.writeClassAndObject(output, object.deviceId());
        kryo.writeClassAndObject(output, object.extensionInstruction().serialize());
    }

    @Override
    public Instructions.ExtensionInstructionWrapper read(Kryo kryo, Input input,
                                                         Class<Instructions.ExtensionInstructionWrapper> type) {
        ExtensionTreatmentType exType = (ExtensionTreatmentType) kryo.readClassAndObject(input);
        DeviceId deviceId = (DeviceId) kryo.readClassAndObject(input);
        DriverService driverService = DefaultServiceDirectory.getService(DriverService.class);
        byte[] bytes = (byte[]) kryo.readClassAndObject(input);
        ExtensionTreatment instruction;

        try {
            DriverHandler handler = new DefaultDriverHandler(
                    new DefaultDriverData(driverService.getDriver(deviceId), deviceId));
            ExtensionTreatmentResolver resolver = handler.behaviour(ExtensionTreatmentResolver.class);
            instruction = resolver.getExtensionInstruction(exType);
            instruction.deserialize(bytes);
        } catch (ItemNotFoundException | IllegalArgumentException e) {
            instruction = new UnresolvedExtensionTreatment(bytes, exType);
        }

        return Instructions.extension(instruction, deviceId);
    }
}
