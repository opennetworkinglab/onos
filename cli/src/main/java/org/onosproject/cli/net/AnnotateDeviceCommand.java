/*
 * Copyright 2015 Open Networking Laboratory
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
package org.onosproject.cli.net;

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.MastershipRole;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DefaultDeviceDescription;
import org.onosproject.net.device.DeviceDescription;
import org.onosproject.net.device.DeviceProvider;
import org.onosproject.net.device.DeviceProviderRegistry;
import org.onosproject.net.device.DeviceProviderService;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.provider.AbstractProvider;
import org.onosproject.net.provider.ProviderId;

/**
 * Annotates network device model.
 */
@Command(scope = "onos", name = "annotate-device",
        description = "Annotates network model entities")
public class AnnotateDeviceCommand extends AbstractShellCommand {

    static final ProviderId PID = new ProviderId("cli", "org.onosproject.cli", true);

    @Argument(index = 0, name = "uri", description = "Device ID",
            required = true, multiValued = false)
    String uri = null;

    @Argument(index = 1, name = "key", description = "Annotation key",
            required = true, multiValued = false)
    String key = null;

    @Argument(index = 2, name = "value",
            description = "Annotation value (null to remove)",
            required = false, multiValued = false)
    String value = null;

    @Override
    protected void execute() {
        DeviceService service = get(DeviceService.class);
        Device device = service.getDevice(DeviceId.deviceId(uri));

        DeviceProviderRegistry registry = get(DeviceProviderRegistry.class);
        DeviceProvider provider = new AnnotationProvider();
        try {
            DeviceProviderService providerService = registry.register(provider);
            providerService.deviceConnected(device.id(), description(device, key, value));
        } finally {
            registry.unregister(provider);
        }
    }

    private DeviceDescription description(Device device, String key, String value) {
        DefaultAnnotations.Builder builder = DefaultAnnotations.builder();
        if (value != null) {
            builder.set(key, value);
        } else {
            builder.remove(key);
        }
        return new DefaultDeviceDescription(device.id().uri(), device.type(),
                                            device.manufacturer(), device.hwVersion(),
                                            device.swVersion(), device.serialNumber(),
                                            device.chassisId(), builder.build());
    }

    // Token provider entity
    private static final class AnnotationProvider
            extends AbstractProvider implements DeviceProvider {
        private AnnotationProvider() {
            super(PID);
        }

        @Override
        public void triggerProbe(DeviceId deviceId) {
        }

        @Override
        public void roleChanged(DeviceId deviceId, MastershipRole newRole) {
        }

        @Override
        public boolean isReachable(DeviceId deviceId) {
            return false;
        }

        @Override
        public void enablePort(DeviceId deviceId, PortNumber portNumber) {
        }

        @Override
        public void disablePort(DeviceId deviceId, PortNumber portNumber) {
        }
    }
}
