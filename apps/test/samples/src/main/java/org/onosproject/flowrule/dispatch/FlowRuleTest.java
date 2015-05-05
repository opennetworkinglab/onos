package org.onosproject.flowrule.dispatch;

import static org.slf4j.LoggerFactory.getLogger;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import org.onosproject.core.ApplicationId;
import org.onosproject.net.DeviceId;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.flow.DefaultFlowRule;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.FlowRuleExtPayLoad;
import org.onosproject.net.flow.FlowRuleService;
import org.slf4j.Logger;

/**
 * third party flow rule test code.
 */
public class FlowRuleTest {

    private final Logger log = getLogger(getClass());
    protected FlowRuleService flowRuleService;
    protected DeviceService deviceService;
    private ApplicationId appId;
    private FlowRule[] flowSet = new DefaultFlowRule[10];
    private DeviceId deviceId;
    private static final String FILE_NAME = "/src/main/resource/org/onosproject/flowrule/resource/flowrule.txt";

    /**
     * Creates a flow rule test object.
     * @param flowRuleService service for injecting flow rules into the environment
     * @param deviceService service for interacting with the inventory of infrastructure devices
     * @param appId application identifier
     */
    public FlowRuleTest(FlowRuleService flowRuleService,
                        DeviceService deviceService, ApplicationId appId) {
        this.flowRuleService = flowRuleService;
        this.deviceService = deviceService;
        this.deviceId = deviceService.getAvailableDevices().iterator().next().id();
        this.appId = appId;
        loadFile();
    }

    private void loadFile() {
        String relativelyPath = System.getProperty("user.dir");
        File flowFile = new File(relativelyPath + FILE_NAME);
        BufferedReader br = null;
        try {
            FileReader in = new FileReader(flowFile);
            br = new BufferedReader(in);
            FlowRule rule = null;
            int i = 0;
            String flow = "";
            while ((flow = br.readLine()) != null) {
                rule = buildFlowRule(flow);
                flowSet[i++] = rule;
            }
        } catch (IOException e) {
            log.info("file does not exist.");
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    log.info("nothing");
                }
            }
        }
    }

    private FlowRule buildFlowRule(String flow) {
        FlowRuleExtPayLoad payLoad = FlowRuleExtPayLoad.flowRuleExtPayLoad(flow
                .getBytes());
        FlowRule flowRule = new DefaultFlowRule(deviceId, null, null, 0, appId,
                                                0, false, payLoad);
        return flowRule;
    }

    /**
     * Apply flow rules to specific devices.
     */
    public void applyFlowRules() {
        flowRuleService.applyFlowRules(flowSet);
    }

    /**
     * Remove flow rules from specific devices.
     */
    public void removeFlowRules() {
        flowRuleService.removeFlowRules(flowSet);
    }

}
