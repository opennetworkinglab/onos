/*
 * Copyright 2014-present Open Networking Foundation
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

import com.google.common.collect.Lists;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.support.completers.NullCompleter;
import org.onlab.packet.Ethernet;
import org.onlab.packet.MacAddress;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.FilteredConnectPoint;
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

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.onosproject.net.DeviceId.deviceId;
import static org.onosproject.net.PortNumber.portNumber;

/**
 * Installs bulk point-to-point connectivity intents between given ingress/egress devices.
 */
@Service
@Command(scope = "onos", name = "push-test-intents",
         description = "Installs random intents to test throughput")
public class IntentPushTestCommand extends AbstractShellCommand
        implements IntentListener {

    @Argument(index = 0, name = "ingressDevice",
              description = "Ingress Device/Port Description",
              required = true, multiValued = false)
    @Completion(ConnectPointCompleter.class)
    String ingressDeviceString = null;

    @Argument(index = 1, name = "egressDevice",
              description = "Egress Device/Port Description",
              required = true, multiValued = false)
    @Completion(ConnectPointCompleter.class)
    String egressDeviceString = null;

    @Argument(index = 2, name = "numberOfIntents",
            description = "Number of intents to install/withdraw",
            required = true, multiValued = false)
    @Completion(NullCompleter.class)
    String numberOfIntents = null;

    @Argument(index = 3, name = "keyOffset",
            description = "Starting point for first key (default: 1)",
            required = false, multiValued = false)
    String keyOffsetStr = null;

    @Option(name = "-i", aliases = "--install",
            description = "Install intents",
            required = false, multiValued = false)
    private boolean installOnly = false;

    @Option(name = "-w", aliases = "--withdraw",
            description = "Withdraw intents",
            required = false, multiValued = false)
    private boolean withdrawOnly = false;

    private IntentService service;
    private CountDownLatch latch;
    private volatile long start, end;
    private int count;
    private int keyOffset;
    private boolean add;
    List<Key> keysForInstall = new ArrayList<>();
    List<Key> keysForWithdraw = new ArrayList<>();

    @Override
    protected void doExecute() {
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

        boolean both = !(installOnly ^ withdrawOnly);

        if (installOnly || both) {
            add = true;
            submitIntents(operations);
        }

        if (withdrawOnly || both) {
            add = false;
            submitIntents(operations);
        }

        service.removeListener(this);
    }

    private List<Intent> generateIntents(ConnectPoint ingress, ConnectPoint egress) {
        TrafficSelector.Builder selectorBldr = DefaultTrafficSelector.builder()
                .matchEthType(Ethernet.TYPE_IPV4);
        TrafficTreatment treatment = DefaultTrafficTreatment.emptyTreatment();

        List<Intent> intents = Lists.newArrayList();
        for (long i = 0; i < count; i++) {
            TrafficSelector selector = selectorBldr
                    .matchEthSrc(MacAddress.valueOf(i + keyOffset))
                    .build();
            intents.add(PointToPointIntent.builder()
                    .appId(appId())
                    .key(Key.of(i + keyOffset, appId()))
                    .selector(selector)
                    .treatment(treatment)
                    .filteredIngressPoint(new FilteredConnectPoint(ingress))
                    .filteredEgressPoint(new FilteredConnectPoint(egress))
                    .build());
            keysForInstall.add(Key.of(i + keyOffset, appId()));
            keysForWithdraw.add(Key.of(i + keyOffset, appId()));
        }
        return intents;
    }

    private void submitIntents(List<Intent> intents) {
        latch = new CountDownLatch(count);
        log.info("CountDownLatch is set with count of {}", count);
        start = System.currentTimeMillis();
        for (Intent intent : intents) {
            if (add) {
                service.submit(intent);
            } else {
                service.withdraw(intent);
            }
        }

        try {
            // In this way with the tests in place the timeout will be
            // 61 seconds.
            if (latch.await(1000L + count * 60L, TimeUnit.MILLISECONDS)) {
                printResults(count);
            } else {
                print("Failure: %d intents not installed", latch.getCount());
            }
        } catch (InterruptedException e) {
            print(e.toString());
        }
    }

    private void printResults(int count) {
        long delta = end - start;
        String text = add ? "install" : "withdraw";
        print("Time to %s %d intents: %d ms", text, count, delta);
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

    private static final EnumSet<IntentEvent.Type> IGNORE_EVENT
            = EnumSet.of(Type.INSTALL_REQ, Type.WITHDRAW_REQ);
    @Override
    public synchronized void event(IntentEvent event) {
        if (!appId().equals(event.subject().appId())) {
            // not my event, ignore
            return;
        }
        Type expected = add ? Type.INSTALLED : Type.WITHDRAWN;
        List keylist = add ? keysForInstall : keysForWithdraw;
        log.debug("Event generated: {}", event);
        if (event.type() == expected && keylist.contains(event.subject().key())) {
            end = Math.max(end, event.time());
            keylist.remove(event.subject().key());
            if (latch != null) {
                if (latch.getCount() == 0) {
                    log.warn("Latch was already 0 before counting down?");
                }
                latch.countDown();
                log.debug("Latch count is {}", latch.getCount());
            } else {
                log.warn("install event latch is null");
            }
        } else if (IGNORE_EVENT.contains(event.type())) {
            log.debug("Unexpected intent event: {}", event);
        }
    }
}
