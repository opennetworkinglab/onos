package org.onlab.onos.net.flow;

import org.onlab.onos.net.provider.ProviderRegistry;

/**
 * Abstraction for a flow rule provider registry.
 */
public interface FlowRuleProviderRegistry
        extends ProviderRegistry<FlowRuleProvider, FlowRuleProviderService> {
}
