package org.onlab.onos.provider.of.flow.impl;

import static org.slf4j.LoggerFactory.getLogger;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onlab.onos.net.DeviceId;
import org.onlab.onos.net.flow.FlowEntry;
import org.onlab.onos.net.flow.FlowRule;
import org.onlab.onos.net.flow.FlowRuleProvider;
import org.onlab.onos.net.flow.FlowRuleProviderRegistry;
import org.onlab.onos.net.flow.FlowRuleProviderService;
import org.onlab.onos.net.provider.AbstractProvider;
import org.onlab.onos.net.provider.ProviderId;
import org.onlab.onos.net.topology.TopologyService;
import org.onlab.onos.of.controller.OpenFlowController;
import org.slf4j.Logger;

/**
 * Provider which uses an OpenFlow controller to detect network
 * end-station hosts.
 */
@Component(immediate = true)
public class OpenFlowRuleProvider extends AbstractProvider implements FlowRuleProvider {

    private final Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected FlowRuleProviderRegistry providerRegistry;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected OpenFlowController controller;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected TopologyService topologyService;

    private FlowRuleProviderService providerService;

    /**
     * Creates an OpenFlow host provider.
     */
    public OpenFlowRuleProvider() {
        super(new ProviderId("org.onlab.onos.provider.openflow"));
    }

    @Activate
    public void activate() {
        providerService = providerRegistry.register(this);
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        providerRegistry.unregister(this);
        providerService = null;

        log.info("Stopped");
    }
    @Override
    public void applyFlowRule(FlowRule... flowRules) {
        // TODO Auto-generated method stub

    }

    @Override
    public void removeFlowRule(FlowRule... flowRules) {
        // TODO Auto-generated method stub

    }

    @Override
    public Iterable<FlowEntry> getFlowMetrics(DeviceId deviceId) {
        // TODO Auto-generated method stub
        return null;
    }


    //TODO: InternalFlowRuleProvider listening to stats and error and flowremoved.
    // possibly barriers as well. May not be internal at all...


}
