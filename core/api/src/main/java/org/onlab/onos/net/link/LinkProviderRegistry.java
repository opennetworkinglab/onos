package org.onlab.onos.net.link;

import org.onlab.onos.net.provider.ProviderRegistry;

/**
 * Abstraction of an infrastructure link provider registry.
 */
public interface LinkProviderRegistry
        extends ProviderRegistry<LinkProvider, LinkProviderService> {
}
