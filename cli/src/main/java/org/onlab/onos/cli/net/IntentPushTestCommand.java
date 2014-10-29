/*
 * Copyright 2014 Open Networking Laboratory
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
import org.onlab.onos.net.intent.IntentEvent;
import org.onlab.onos.net.intent.IntentEvent.Type;
import org.onlab.onos.net.intent.IntentListener;
import org.onlab.onos.net.intent.IntentService;
import org.onlab.onos.net.intent.PointToPointIntent;
import org.onlab.packet.Ethernet;
import org.onlab.packet.MacAddress;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.onlab.onos.net.DeviceId.deviceId;
import static org.onlab.onos.net.PortNumber.portNumber;

/**
 * Installs point-to-point connectivity intents.
 */
@Command(scope = "onos", name = "push-test-intents",
         description = "Installs random intents to test throughput")
public class IntentPushTestCommand extends AbstractShellCommand
        implements IntentListener {

    @Argument(index = 0, name = "ingressDevice",
              description = "Ingress Device/Port Description",
              required = true, multiValued = false)
    String ingressDeviceString = null;

    @Argument(index = 1, name = "egressDevice",
              description = "Egress Device/Port Description",
              required = true, multiValued = false)
    String egressDeviceString = null;

    @Argument(index = 2, name = "count",
              description = "Number of intents to push",
              required = true, multiValued = false)
    String countString = null;


    private static long id = 0x7870001;

    private IntentService service;
    private CountDownLatch latch;
    private long start, end;

    @Override
    protected void execute() {
        service = get(IntentService.class);

        DeviceId ingressDeviceId = deviceId(getDeviceId(ingressDeviceString));
        PortNumber ingressPortNumber = portNumber(getPortNumber(ingressDeviceString));
        ConnectPoint ingress = new ConnectPoint(ingressDeviceId, ingressPortNumber);

        DeviceId egressDeviceId = deviceId(getDeviceId(egressDeviceString));
        PortNumber egressPortNumber = portNumber(getPortNumber(egressDeviceString));
        ConnectPoint egress = new ConnectPoint(egressDeviceId, egressPortNumber);

        TrafficSelector.Builder selector = DefaultTrafficSelector.builder()
                .matchEthType(Ethernet.TYPE_IPV4);
        TrafficTreatment treatment = DefaultTrafficTreatment.builder().build();

        int count = Integer.parseInt(countString);

        service.addListener(this);
        latch = new CountDownLatch(count);

        start = System.currentTimeMillis();
        for (int i = 0; i < count; i++) {
            TrafficSelector s = selector
                    .matchEthSrc(MacAddress.valueOf(i))
                    .build();
            Intent intent = new PointToPointIntent(appId(), s, treatment,
                                                   ingress, egress);
            service.submit(intent);
        }
        try {
            if (latch.await(10, TimeUnit.SECONDS)) {
                printResults(count);
            } else {
                print("I FAIL MISERABLY -> %d", latch.getCount());
            }
        } catch (InterruptedException e) {
            print(e.toString());
        }

        service.removeListener(this);
    }

    private void printResults(int count) {
        long delta = end - start;
        print("Time to install %d intents: %d ms", count, delta);
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

    @Override
    public void event(IntentEvent event) {
        if (event.type() == Type.INSTALLED) {
            end = event.time();
            if (latch != null) {
                latch.countDown();
            } else {
                log.warn("install event latch is null");
            }
        } else {
            log.info("I FAIL -> {}", event);
        }
    }
}
