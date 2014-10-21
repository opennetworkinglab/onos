package org.onlab.onos.cli.net;

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.onlab.onos.cli.AbstractShellCommand;
import org.onlab.onos.net.ConnectPoint;
import org.onlab.onos.net.DeviceId;
import org.onlab.onos.net.PortNumber;
import org.onlab.onos.net.flow.DefaultTrafficSelector;
import org.onlab.onos.net.flow.DefaultTrafficTreatment;
import org.onlab.onos.net.flow.TrafficSelector;
import org.onlab.onos.net.flow.TrafficTreatment;
import org.onlab.onos.net.intent.Intent;
import org.onlab.onos.net.intent.IntentService;
import org.onlab.onos.net.intent.MultiPointToSinglePointIntent;
import org.onlab.packet.Ethernet;

import java.util.HashSet;
import java.util.Set;

import static org.onlab.onos.net.DeviceId.deviceId;
import static org.onlab.onos.net.PortNumber.portNumber;

/**
 * Installs point-to-point connectivity intents.
 */
@Command(scope = "onos", name = "add-multi-to-single-intent",
         description = "Installs point-to-point connectivity intent")
public class AddMultiPointToSinglePointIntentCommand extends AbstractShellCommand {

    @Argument(index = 0, name = "ingressDevices",
              description = "Ingress Device/Port Description",
              required = true, multiValued = true)
    String[] deviceStrings = null;

    @Override
    protected void execute() {
        IntentService service = get(IntentService.class);

        if (deviceStrings.length < 2) {
            return;
        }

        String egressDeviceString = deviceStrings[deviceStrings.length - 1];
        DeviceId egressDeviceId = deviceId(getDeviceId(egressDeviceString));
        PortNumber egressPortNumber = portNumber(getPortNumber(egressDeviceString));
        ConnectPoint egress = new ConnectPoint(egressDeviceId, egressPortNumber);
        Set<ConnectPoint> ingressPoints = new HashSet<>();

        for (int index = 0; index < deviceStrings.length - 1; index++) {
            String ingressDeviceString = deviceStrings[index];
            DeviceId ingressDeviceId = deviceId(getDeviceId(ingressDeviceString));
            PortNumber ingressPortNumber = portNumber(getPortNumber(ingressDeviceString));
            ConnectPoint ingress = new ConnectPoint(ingressDeviceId, ingressPortNumber);
            ingressPoints.add(ingress);
        }

        TrafficSelector selector = DefaultTrafficSelector.builder()
                .matchEthType(Ethernet.TYPE_IPV4)
                .build();
        TrafficTreatment treatment = DefaultTrafficTreatment.builder().build();

        Intent intent = new MultiPointToSinglePointIntent(appId(), selector, treatment,
                                                          ingressPoints, egress);
        service.submit(intent);
    }

    /**
     * Extracts the port number portion of the ConnectPoint.
     *
     * @param deviceString string representing the device/port
     * @return port number as a string, empty string if the port is not found
     */
    private String getPortNumber(String deviceString) {
        int slash = deviceString.indexOf('/');
        if (slash <= 0) {
            return "";
        }
        return deviceString.substring(slash + 1, deviceString.length());
    }

    /**
     * Extracts the device ID portion of the ConnectPoint.
     *
     * @param deviceString string representing the device/port
     * @return device ID string
     */
    private String getDeviceId(String deviceString) {
        int slash = deviceString.indexOf('/');
        if (slash <= 0) {
            return "";
        }
        return deviceString.substring(0, slash);
    }
}
