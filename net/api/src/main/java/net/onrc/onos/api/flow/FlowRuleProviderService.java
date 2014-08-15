package net.onrc.onos.api.flow;

import net.onrc.onos.api.ProviderService;

/**
 * Service through which flowrule providers can inject flowrule information into
 * the core.
 */
public interface FlowRuleProviderService extends ProviderService {

    /**
     * Signals that a flow that was previously installed has been removed.
     *
     * @param flowDescription information about the removed flow
     */
    void flowRemoved(FlowDescription flowDescription);

    /**
     * Signals that a flowrule is missing for some network traffic.
     *
     * @param flowDescription information about traffic in need of flow rule(s)
     */
    void flowMissing(FlowDescription flowDescription);

    /**
     * Signals that a flowrule has been added.
     *
     * TODO  think about if this really makes sense, e.g. if stats collection or
     * something can leverage it.
     *
     * @param flowDescription the rule that was added
     */
    void flowAdded(FlowDescription flowDescription);

}