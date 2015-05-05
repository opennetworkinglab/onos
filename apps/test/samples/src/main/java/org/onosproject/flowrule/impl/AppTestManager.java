package org.onosproject.flowrule.impl;

import static org.slf4j.LoggerFactory.getLogger;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.flowrule.AppTestService;
import org.onosproject.flowrule.dispatch.FlowRuleTest;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.flow.FlowRuleService;
import org.slf4j.Logger;

/**
 * Test for a application.
 */
@Component(immediate = true)
@Service
public class AppTestManager implements AppTestService {

    private static final String APP_TEST = "org.onosproject.apptest";
    private final Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;
    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected FlowRuleService flowRuleService;
    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;
    private ApplicationId appId;
    FlowRuleTest flowRule;

    @Activate
    protected void activate() {
        log.info("APP-TEST started");
        appId = coreService.registerApplication(APP_TEST);
        flowRule = new FlowRuleTest(flowRuleService, deviceService, appId);
        flowRule.applyFlowRules();
    }

    @Deactivate
    protected void deactivate() {
        flowRule.removeFlowRules();
        log.info("APP-TEST Stopped");
    }
}
