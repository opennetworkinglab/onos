package org.onlab.onos.net.packet;

import org.onlab.onos.net.provider.ProviderRegistry;

/**
 * Abstraction of an infrastructure packet provider registry.
 */
public interface PacketProviderRegistry
extends ProviderRegistry<PacketProvider, PacketProviderService> {
}
