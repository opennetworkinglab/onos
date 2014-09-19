package org.onlab.onos.provider.of.flow.impl;

import static org.slf4j.LoggerFactory.getLogger;

import java.util.List;
import java.util.Map;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onlab.onos.net.flow.FlowRule;
import org.onlab.onos.net.flow.FlowRuleProvider;
import org.onlab.onos.net.flow.FlowRuleProviderRegistry;
import org.onlab.onos.net.flow.FlowRuleProviderService;
import org.onlab.onos.net.provider.AbstractProvider;
import org.onlab.onos.net.provider.ProviderId;
import org.onlab.onos.net.topology.TopologyService;
import org.onlab.onos.openflow.controller.Dpid;
import org.onlab.onos.openflow.controller.OpenFlowController;
import org.onlab.onos.openflow.controller.OpenFlowEventListener;
import org.onlab.onos.openflow.controller.OpenFlowSwitch;
import org.onlab.onos.openflow.controller.OpenFlowSwitchListener;
import org.projectfloodlight.openflow.protocol.OFFlowRemoved;
import org.projectfloodlight.openflow.protocol.OFFlowStatsEntry;
import org.projectfloodlight.openflow.protocol.OFFlowStatsReply;
import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFPortStatus;
import org.projectfloodlight.openflow.protocol.OFStatsReply;
import org.projectfloodlight.openflow.protocol.OFStatsType;
import org.slf4j.Logger;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

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

    private final InternalFlowProvider listener = new InternalFlowProvider();

    /**
     * Creates an OpenFlow host provider.
     */
    public OpenFlowRuleProvider() {
        super(new ProviderId("of", "org.onlab.onos.provider.openflow"));
    }

    @Activate
    public void activate() {
        providerService = providerRegistry.register(this);
        controller.addListener(listener);
        controller.addEventListener(listener);
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
        for (int i = 0; i < flowRules.length; i++) {
            applyRule(flowRules[i]);
        }
    }

    private void applyRule(FlowRule flowRule) {
        OpenFlowSwitch sw = controller.getSwitch(Dpid.dpid(flowRule.deviceId().uri()));
        sw.sendMsg(new FlowModBuilder(flowRule, sw.factory()).buildFlowMod());
    }



    @Override
    public void removeFlowRule(FlowRule... flowRules) {
        // TODO Auto-generated method stub

    }


    //TODO: InternalFlowRuleProvider listening to stats and error and flowremoved.
    // possibly barriers as well. May not be internal at all...
    private class InternalFlowProvider
    implements OpenFlowSwitchListener, OpenFlowEventListener {

        private final Map<Dpid, FlowStatsCollector> collectors = Maps.newHashMap();

        @Override
        public void switchAdded(Dpid dpid) {
            FlowStatsCollector fsc = new FlowStatsCollector(controller.getSwitch(dpid), 1);
            fsc.start();
            collectors.put(dpid, fsc);
        }

        @Override
        public void switchRemoved(Dpid dpid) {
            collectors.remove(dpid).stop();
        }

        @Override
        public void portChanged(Dpid dpid, OFPortStatus status) {
            //TODO: Decide whether to evict flows internal store.
        }

        @Override
        public void handleMessage(Dpid dpid, OFMessage msg) {
            switch (msg.getType()) {
            case FLOW_REMOVED:
                //TODO: make this better
                OFFlowRemoved removed = (OFFlowRemoved) msg;

                FlowRule fr = new FlowRuleBuilder(dpid, removed).build();
                providerService.flowRemoved(fr);
                break;
            case STATS_REPLY:
                pushFlowMetrics(dpid, (OFStatsReply) msg);
                break;
            case BARRIER_REPLY:
            case ERROR:
            default:
                log.warn("Unhandled message type: {}", msg.getType());
            }

        }

        private void pushFlowMetrics(Dpid dpid, OFStatsReply stats) {
            if (stats.getStatsType() != OFStatsType.FLOW) {
                return;
            }
            final OFFlowStatsReply replies = (OFFlowStatsReply) stats;
            final List<FlowRule> entries = Lists.newLinkedList();
            for (OFFlowStatsEntry reply : replies.getEntries()) {
                entries.add(new FlowRuleBuilder(dpid, reply).build());
            }
            log.debug("sending flowstats to core {}", entries);
            providerService.pushFlowMetrics(entries);
        }

    }


}
