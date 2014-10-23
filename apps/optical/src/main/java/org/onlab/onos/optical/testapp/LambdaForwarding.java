package org.onlab.onos.optical.testapp;

import static org.slf4j.LoggerFactory.getLogger;

import java.util.HashMap;
import java.util.Map;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onlab.onos.ApplicationId;
import org.onlab.onos.CoreService;
import org.onlab.onos.net.Device;
import org.onlab.onos.net.DeviceId;
import org.onlab.onos.net.PortNumber;
import org.onlab.onos.net.device.DeviceEvent;
import org.onlab.onos.net.device.DeviceListener;
import org.onlab.onos.net.device.DeviceService;
import org.onlab.onos.net.flow.DefaultFlowRule;
import org.onlab.onos.net.flow.DefaultTrafficSelector;
import org.onlab.onos.net.flow.DefaultTrafficTreatment;
import org.onlab.onos.net.flow.FlowRule;
import org.onlab.onos.net.flow.FlowRuleService;
import org.onlab.onos.net.flow.TrafficSelector;
import org.onlab.onos.net.flow.TrafficTreatment;
import org.slf4j.Logger;

/**
 * Sample reactive forwarding application.
 */
@Component(immediate = true)
public class LambdaForwarding {

    private final Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected FlowRuleService flowRuleService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;

    private ApplicationId appId;

    private final InternalDeviceListener listener = new InternalDeviceListener();

    private final Map<DeviceId, Integer> uglyMap = new HashMap<>();

    @Activate
    public void activate() {
        appId = coreService.registerApplication("org.onlab.onos.fwd");

        uglyMap.put(DeviceId.deviceId("of:0000ffffffffff01"), 1);
        uglyMap.put(DeviceId.deviceId("of:0000ffffffffff02"), 2);
        uglyMap.put(DeviceId.deviceId("of:0000ffffffffff03"), 3);

        deviceService.addListener(listener);

        for (Device d : deviceService.getDevices()) {
            pushRules(d);
        }


        log.info("Started with Application ID {}", appId.id());
    }

    @Deactivate
    public void deactivate() {
        flowRuleService.removeFlowRulesById(appId);

        log.info("Stopped");
    }


    private void pushRules(Device device) {

        TrafficSelector.Builder sbuilder = DefaultTrafficSelector.builder();
        TrafficTreatment.Builder tbuilder = DefaultTrafficTreatment.builder();
        int inport;
        int outport;
        short lambda = 10;
        byte sigType = 1;
        Integer switchNumber = uglyMap.get(device.id());
        if (switchNumber == null) {
            return;
        }

        switch (switchNumber) {
        case 1:
            inport = 10;
            outport = 20;
            sbuilder.matchInport(PortNumber.portNumber(inport));
            tbuilder.setOutput(PortNumber.portNumber(outport)).setLambda(lambda);
            break;
        case 2:
            inport = 21;
            outport = 11;
            sbuilder.matchLambda(lambda).
                    matchInport(PortNumber.portNumber(inport)); // match sigtype
            tbuilder.setOutput(PortNumber.portNumber(outport));
            break;
        case 3:
            inport = 30;
            outport = 31;
            sbuilder.matchLambda(lambda).
                    matchInport(PortNumber.portNumber(inport));
            tbuilder.setOutput(PortNumber.portNumber(outport)).setLambda(lambda);
            break;
        default:
        }

        TrafficTreatment treatement = tbuilder.build();
        TrafficSelector selector = sbuilder.build();

        FlowRule f = new DefaultFlowRule(device.id(), selector,
                treatement, 100, appId, 600, false);

        flowRuleService.applyFlowRules(f);



    }

    public class InternalDeviceListener implements DeviceListener {

        @Override
        public void event(DeviceEvent event) {
            switch (event.type()) {
            case DEVICE_ADDED:
                pushRules(event.subject());
                break;
            case DEVICE_AVAILABILITY_CHANGED:
                break;
            case DEVICE_MASTERSHIP_CHANGED:
                break;
            case DEVICE_REMOVED:
                break;
            case DEVICE_SUSPENDED:
                break;
            case DEVICE_UPDATED:
                break;
            case PORT_ADDED:
                break;
            case PORT_REMOVED:
                break;
            case PORT_UPDATED:
                break;
            default:
                break;

            }

        }

    }


}


