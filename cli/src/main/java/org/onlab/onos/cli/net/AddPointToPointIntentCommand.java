package org.onlab.onos.cli.net;

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.onlab.onos.net.ConnectPoint;
import org.onlab.onos.net.DeviceId;
import org.onlab.onos.net.PortNumber;
import org.onlab.onos.net.flow.TrafficSelector;
import org.onlab.onos.net.flow.TrafficTreatment;
import org.onlab.onos.net.intent.Intent;
import org.onlab.onos.net.intent.IntentService;
import org.onlab.onos.net.intent.PointToPointIntent;

import static org.onlab.onos.net.flow.DefaultTrafficTreatment.builder;

import static org.onlab.onos.net.DeviceId.deviceId;
import static org.onlab.onos.net.PortNumber.portNumber;

/**
 * Installs point-to-point connectivity intents.
 */
@Command(scope = "onos", name = "add-point-intent",
         description = "Installs point-to-point connectivity intent")
public class AddPointToPointIntentCommand extends ConnectivityIntentCommand {

    @Argument(index = 0, name = "ingressDevice",
              description = "Ingress Device/Port Description",
              required = true, multiValued = false)
    String ingressDeviceString = null;

    @Argument(index = 1, name = "egressDevice",
              description = "Egress Device/Port Description",
              required = true, multiValued = false)
    String egressDeviceString = null;

    @Override
    protected void execute() {
        IntentService service = get(IntentService.class);

        DeviceId ingressDeviceId = deviceId(getDeviceId(ingressDeviceString));
        PortNumber ingressPortNumber = portNumber(getPortNumber(ingressDeviceString));
        ConnectPoint ingress = new ConnectPoint(ingressDeviceId, ingressPortNumber);

        DeviceId egressDeviceId = deviceId(getDeviceId(egressDeviceString));
        PortNumber egressPortNumber = portNumber(getPortNumber(egressDeviceString));
        ConnectPoint egress = new ConnectPoint(egressDeviceId, egressPortNumber);

        TrafficSelector selector = buildTrafficSelector();
        TrafficTreatment treatment = builder().build();

        Intent intent = new PointToPointIntent(appId(), selector, treatment,
                                               ingress, egress);
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
