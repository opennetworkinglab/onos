package org.onlab.onos.net.flow;

import org.onlab.onos.net.DeviceId;
import org.onlab.onos.net.provider.ProviderService;

/**
 * Service through which flow rule providers can inject information into
 * the core.
 */
public interface FlowRuleProviderService extends ProviderService<FlowRuleProvider> {

    /**
     * Signals that a flow rule that was previously installed has been removed.
     *
     * @param flowRule information about the removed flow
     */
    void flowRemoved(FlowEntry flowEntry);

    /**
     * Pushes the collection of flow entries currently applied on the given
     * device.
     *
     * @param flowRules collection of flow rules
     */
    void pushFlowMetrics(DeviceId deviceId, Iterable<FlowEntry> flowEntries);



}
