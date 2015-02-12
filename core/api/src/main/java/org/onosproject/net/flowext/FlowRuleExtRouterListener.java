package org.onosproject.net.flowext;

/**
 * The monitor module of router.
 */
public interface FlowRuleExtRouterListener {

    /**
     * Notify monitor the router has down its work.
     *
     * @param batchOperation batch of flow rules.
     *           A batch can contain flow rules for a single device only.
     * @return Future response indicating success/failure of the batch operation
     * all the way down to the device.
     */
    void notify(FlowRuleBatchExtEvent event);
}
