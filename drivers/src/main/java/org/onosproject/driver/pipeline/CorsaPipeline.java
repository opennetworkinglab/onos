package org.onosproject.driver.pipeline;

import static org.slf4j.LoggerFactory.getLogger;

import org.onlab.packet.Ethernet;
import org.onosproject.net.flow.DefaultFlowRule;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.FlowRuleOperations;
import org.onosproject.net.flow.FlowRuleOperationsContext;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.slf4j.Logger;

/**
 * Driver for Corsa TTP.
 *
 */
public class CorsaPipeline extends OVSCorsaPipeline {

    private final Logger log = getLogger(getClass());

    @Override
    protected void processVlanMplsTable(boolean install) {
        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
        TrafficTreatment.Builder treatment = DefaultTrafficTreatment
                .builder();
        FlowRuleOperations.Builder ops = FlowRuleOperations.builder();
        FlowRule rule;
        // corsa uses non-OF-standard way to match on presence of VLAN tags
        selector.matchEthType(Ethernet.TYPE_VLAN);
        treatment.transition(VLAN_TABLE);

        rule = DefaultFlowRule.builder()
                .forDevice(deviceId)
                .withSelector(selector.build())
                .withTreatment(treatment.build())
                .withPriority(CONTROLLER_PRIORITY)
                .fromApp(appId)
                .makePermanent()
                .forTable(VLAN_MPLS_TABLE).build();

        ops = install ? ops.add(rule) : ops.remove(rule);

        flowRuleService.apply(ops.build(new FlowRuleOperationsContext() {
            @Override
            public void onSuccess(FlowRuleOperations ops) {
                log.info("Provisioned vlan/mpls table");
            }

            @Override
            public void onError(FlowRuleOperations ops) {
                log.info(
                        "Failed to provision vlan/mpls table");
            }
        }));

    }

}
