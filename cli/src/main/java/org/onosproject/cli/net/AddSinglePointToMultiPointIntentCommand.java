package org.onlab.onos.cli.net;

import static org.onlab.onos.net.DeviceId.deviceId;
import static org.onlab.onos.net.PortNumber.portNumber;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.onlab.onos.net.ConnectPoint;
import org.onlab.onos.net.DeviceId;
import org.onlab.onos.net.PortNumber;
import org.onlab.onos.net.flow.DefaultTrafficTreatment;
import org.onlab.onos.net.flow.TrafficSelector;
import org.onlab.onos.net.flow.TrafficTreatment;
import org.onlab.onos.net.intent.Constraint;
import org.onlab.onos.net.intent.IntentService;
import org.onlab.onos.net.intent.SinglePointToMultiPointIntent;


@Command(scope = "onos", name = "add-single-to-multi-intent",
        description = "Installs connectivity intent between multiple egress devices and a single ingress device")
public class AddSinglePointToMultiPointIntentCommand extends ConnectivityIntentCommand {
    @Argument(index = 0, name = "egressDevices ingressDevice",
            description = "egress Device/Port...egress Device/Port ingressDevice/port",
            required = true, multiValued = true)
    String[] deviceStrings = null;

    @Override
    protected void execute() {
        IntentService service = get(IntentService.class);

        if (deviceStrings.length < 2) {
            return;
        }

        String ingressDeviceString = deviceStrings[deviceStrings.length - 1];
        DeviceId ingressDeviceId = deviceId(getDeviceId(ingressDeviceString));
        PortNumber ingressPortNumber = portNumber(getPortNumber(ingressDeviceString));
        ConnectPoint ingressPoint = new ConnectPoint(ingressDeviceId,
                                                     ingressPortNumber);

        Set<ConnectPoint> egressPoints = new HashSet<>();
        for (int index = 0; index < deviceStrings.length - 1; index++) {
            String egressDeviceString = deviceStrings[index];
            DeviceId egressDeviceId = deviceId(getDeviceId(egressDeviceString));
            PortNumber egressPortNumber = portNumber(getPortNumber(egressDeviceString));
            ConnectPoint egress = new ConnectPoint(egressDeviceId,
                                                   egressPortNumber);
            egressPoints.add(egress);
        }

        TrafficSelector selector = buildTrafficSelector();
        TrafficTreatment treatment = DefaultTrafficTreatment.builder().build();
        List<Constraint> constraints = buildConstraints();

        SinglePointToMultiPointIntent intent = new SinglePointToMultiPointIntent(
                                                                                 appId(),
                                                                                 selector,
                                                                                 treatment,
                                                                                 ingressPoint,
                                                                                 egressPoints,
                                                                                 constraints);
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
