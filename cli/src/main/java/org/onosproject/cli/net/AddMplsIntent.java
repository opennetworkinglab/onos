package org.onosproject.cli.net;

import static org.onosproject.net.DeviceId.deviceId;
import static org.onosproject.net.PortNumber.portNumber;

import java.util.List;
import java.util.Optional;

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.commands.Option;

import org.onlab.packet.MplsLabel;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.intent.Constraint;
import org.onosproject.net.intent.IntentService;
import org.onosproject.net.intent.MplsIntent;

@Command(scope = "onos", name = "add-mpls-intent", description = "Installs mpls connectivity intent")
public class AddMplsIntent extends ConnectivityIntentCommand {

    @Argument(index = 0, name = "ingressDevice",
            description = "Ingress Device/Port Description",
            required = true,
            multiValued = false)
    private String ingressDeviceString = null;

    @Option(name = "--ingressLabel",
            description = "Ingress Mpls label",
            required = false,
            multiValued = false)
    private String ingressLabelString = "";

    @Argument(index = 1, name = "egressDevice",
            description = "Egress Device/Port Description",
            required = true,
            multiValued = false)
    private String egressDeviceString = null;

    @Option(name = "--egressLabel",
            description = "Egress Mpls label",
            required = false,
            multiValued = false)
    private String egressLabelString = "";

    @Override
    protected void execute() {
        IntentService service = get(IntentService.class);

        DeviceId ingressDeviceId = deviceId(getDeviceId(ingressDeviceString));
        PortNumber ingressPortNumber = portNumber(getPortNumber(ingressDeviceString));
        ConnectPoint ingress = new ConnectPoint(ingressDeviceId,
                                                ingressPortNumber);
        Optional<MplsLabel> ingressLabel = Optional.empty();
        if (!ingressLabelString.isEmpty()) {
            ingressLabel = Optional
                    .ofNullable(MplsLabel.mplsLabel(parseInt(ingressLabelString)));
        }

        DeviceId egressDeviceId = deviceId(getDeviceId(egressDeviceString));
        PortNumber egressPortNumber = portNumber(getPortNumber(egressDeviceString));
        ConnectPoint egress = new ConnectPoint(egressDeviceId, egressPortNumber);

        Optional<MplsLabel> egressLabel = Optional.empty();
        if (!ingressLabelString.isEmpty()) {
            egressLabel = Optional
                    .ofNullable(MplsLabel.mplsLabel(parseInt(egressLabelString)));
        }

        TrafficSelector selector = buildTrafficSelector();
        TrafficTreatment treatment = buildTrafficTreatment();

        List<Constraint> constraints = buildConstraints();

        MplsIntent intent = new MplsIntent(appId(), selector, treatment,
                                           ingress, ingressLabel, egress,
                                           egressLabel, constraints);
        service.submit(intent);
    }

    /**
     * Extracts the port number portion of the ConnectPoint.
     *
     * @param deviceString string representing the device/port
     * @return port number as a string, empty string if the port is not found
     */
    public static String getPortNumber(String deviceString) {
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
    public static String getDeviceId(String deviceString) {
        int slash = deviceString.indexOf('/');
        if (slash <= 0) {
            return "";
        }
        return deviceString.substring(0, slash);
    }

    protected Integer parseInt(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException nfe) {
            return null;
        }
    }
}
