package org.onlab.onos.net.flow;

import java.util.concurrent.Future;

import org.onlab.onos.ApplicationId;
import org.onlab.onos.net.intent.BatchOperation;
import org.onlab.onos.net.provider.Provider;

/**
 * Abstraction of a flow rule provider.
 */
public interface FlowRuleProvider extends Provider {

    static final int POLL_INTERVAL = 5;

    /**
     * Instructs the provider to apply the specified flow rules to their
     * respective devices.
     * @param flowRules one or more flow rules
     * throws SomeKindOfException that indicates which ones were applied and
     *                  which ones failed
     */
    void applyFlowRule(FlowRule... flowRules);

    /**
     * Instructs the provider to remove the specified flow rules to their
     * respective devices.
     * @param flowRules one or more flow rules
     * throws SomeKindOfException that indicates which ones were applied and
     *                  which ones failed
     */
    void removeFlowRule(FlowRule... flowRules);

    /**
     * Removes rules by their id.
     * @param id the id to remove
     */
    void removeRulesById(ApplicationId id, FlowRule... flowRules);

    /**
     * Installs a batch of flow rules. Each flowrule is associated to an
     * operation which results in either addition, removal or modification.
     * @param batch a batch of flow rules
     * @return a future indicating the status of this execution
     */
    Future<CompletedBatchOperation> executeBatch(BatchOperation<FlowRuleBatchEntry> batch);

}
