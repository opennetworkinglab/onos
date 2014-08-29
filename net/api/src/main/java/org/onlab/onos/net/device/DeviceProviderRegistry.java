package org.onlab.onos.net.device;

import org.onlab.onos.net.provider.ProviderRegistry;

/**
 * Abstraction of a device provider registry.
 */
public interface DeviceProviderRegistry
        extends ProviderRegistry<DeviceProvider, DeviceProviderService> {
}
