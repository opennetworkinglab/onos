/*
 * Copyright 2015-present Open Networking Laboratory
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
package org.onosproject.cli.net;

import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.onlab.packet.Ethernet;
import org.onlab.packet.MacAddress;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.IntentEvent;
import org.onosproject.net.intent.IntentEvent.Type;
import org.onosproject.net.intent.IntentListener;
import org.onosproject.net.intent.IntentService;
import org.onosproject.net.intent.Key;
import org.onosproject.net.intent.PointToPointIntent;

import com.google.common.collect.Lists;

import static org.onlab.util.Tools.delay;
import static org.onosproject.net.DeviceId.deviceId;
import static org.onosproject.net.PortNumber.portNumber;

/**
 * Installs point-to-point connectivity intents.
 */
@Command(scope = "onos", name = "cycle-intents",
         description = "Installs random intents to test throughput")
public class IntentCycleCommand extends AbstractShellCommand
        implements IntentListener {

    @Argument(index = 0, name = "ingressDevice",
              description = "Ingress Device/Port Description",
              required = true, multiValued = false)
    String ingressDeviceString = null;

    @Argument(index = 1, name = "egressDevice",
              description = "Egress Device/Port Description",
              required = true, multiValued = false)
    String egressDeviceString = null;

    @Argument(index = 2, name = "numberOfIntents",
            description = "Number of intents to install/withdraw",
            required = true, multiValued = false)
    String numberOfIntents = null;

    @Argument(index = 3, name = "keyOffset",
            description = "Starting point for first key (default: 1)",
            required = false, multiValued = false)
    String keyOffsetStr = null;

    private IntentService service;
    private CountDownLatch latch;
    private volatile long start, end;
    private int count;
    private int keyOffset;
    private long submitCounter = 0;
    private AtomicLong eventCounter = new AtomicLong(0);
    private boolean add;

    @Override
    protected void execute() {
        service = get(IntentService.class);


        DeviceId ingressDeviceId = deviceId(getDeviceId(ingressDeviceString));
        PortNumber ingressPortNumber = portNumber(getPortNumber(ingressDeviceString));
        ConnectPoint ingress = new ConnectPoint(ingressDeviceId, ingressPortNumber);

        DeviceId egressDeviceId = deviceId(getDeviceId(egressDeviceString));
        PortNumber egressPortNumber = portNumber(getPortNumber(egressDeviceString));
        ConnectPoint egress = new ConnectPoint(egressDeviceId, egressPortNumber);

        count = Integer.parseInt(numberOfIntents);
        keyOffset = (keyOffsetStr != null) ? Integer.parseInt(keyOffsetStr) : 1;

        service.addListener(this);

        List<Intent> operations = generateIntents(ingress, egress);

        add = true;
        start = System.currentTimeMillis();
        while (start + 10000 > System.currentTimeMillis()) {
            submitIntents(operations);
        }
        delay(5000);
        printResults();

        add = false;
        submitIntents(operations);

        service.removeListener(this);
    }

    private List<Intent> generateIntents(ConnectPoint ingress, ConnectPoint egress) {
        TrafficSelector.Builder selectorBldr = DefaultTrafficSelector.builder()
                .matchEthType(Ethernet.TYPE_IPV4);
        TrafficTreatment treatment = DefaultTrafficTreatment.emptyTreatment();

        List<Intent> intents = Lists.newArrayList();
        for (int i = 0; i < count; i++) {
            TrafficSelector selector = selectorBldr
                    .matchEthSrc(MacAddress.valueOf(i + keyOffset))
                    .build();
            intents.add(
                    PointToPointIntent.builder()
                        .appId(appId())
                        .key(Key.of(i + keyOffset, appId()))
                        .selector(selector)
                        .treatment(treatment)
                        .ingressPoint(ingress)
                        .egressPoint(egress)
                        .build());


        }
        return intents;
    }

    private void submitIntents(List<Intent> intents) {
        for (Intent intent : intents) {
            if (add) {
                submitCounter++;
                service.submit(intent);
            } else {
                service.withdraw(intent);
            }
        }
    }

    private void printResults() {
        //long delta = end - start;
        //String text = add ? "install" : "withdraw";
        print("count: %s / %s", eventCounter, Long.valueOf(submitCounter));
        //print("Time to %s %d intents: %d ms", text, count, delta);
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

    private static final EnumSet<Type> IGNORE_EVENT
            = EnumSet.of(Type.INSTALL_REQ, Type.WITHDRAW_REQ);
    @Override
    public synchronized void event(IntentEvent event) {
        if (!appId().equals(event.subject().appId())) {
            // not my event, ignore
            return;
        }
        Type expected = add ? Type.INSTALLED : Type.WITHDRAWN;
        if (event.type() == expected) {
            eventCounter.getAndIncrement();
        } else if (IGNORE_EVENT.contains(event.type())) {
            log.info("Unexpected intent event: {}", event);
        }
    }
}
