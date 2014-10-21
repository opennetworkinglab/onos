package org.onlab.onos.net.intent;

import java.util.Set;

import org.onlab.onos.ApplicationId;
import org.onlab.onos.TestApplicationId;
import org.onlab.onos.net.ConnectPoint;
import org.onlab.onos.net.DeviceId;
import org.onlab.onos.net.PortNumber;
import org.onlab.onos.net.flow.DefaultTrafficSelector;
import org.onlab.onos.net.flow.DefaultTrafficTreatment;
import org.onlab.onos.net.flow.TrafficSelector;
import org.onlab.onos.net.flow.TrafficTreatment;

/**
 * Base facilities to test various connectivity tests.
 */
public abstract class ConnectivityIntentTest extends IntentTest {

    public static final ApplicationId APPID = new TestApplicationId("foo");

    public static final IntentId IID = new IntentId(123);
    public static final TrafficSelector MATCH = DefaultTrafficSelector.builder().build();
    public static final TrafficTreatment NOP = DefaultTrafficTreatment.builder().build();

    public static final ConnectPoint P1 = new ConnectPoint(DeviceId.deviceId("111"), PortNumber.portNumber(0x1));
    public static final ConnectPoint P2 = new ConnectPoint(DeviceId.deviceId("222"), PortNumber.portNumber(0x2));
    public static final ConnectPoint P3 = new ConnectPoint(DeviceId.deviceId("333"), PortNumber.portNumber(0x3));

    public static final Set<ConnectPoint> PS1 = itemSet(new ConnectPoint[]{P1, P3});
    public static final Set<ConnectPoint> PS2 = itemSet(new ConnectPoint[]{P2, P3});
}
