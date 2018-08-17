/*
 * Copyright 2015-present Open Networking Foundation
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

import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.math.RandomUtils;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onlab.packet.MacAddress;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.Device;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.flow.DefaultFlowRule;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.FlowEntry.FlowEntryState;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.FlowRuleOperations;
import org.onosproject.net.flow.FlowRuleOperationsContext;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Stopwatch;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Streams;

/**
 * Installs bulk flows.
 */
@Service
@Command(scope = "onos", name = "add-test-flows",
         description = "Installs a number of test flow rules - for testing only")
public class AddTestFlowsCommand extends AbstractShellCommand {

    private static final int MAX_OUT_PORT = 254;

    private CountDownLatch latch;

    @Argument(index = 0, name = "flowPerDevice", description = "Number of flows to add per device",
              required = true, multiValued = false)
    String flows = null;

    @Argument(index = 1, name = "numOfRuns", description = "Number of iterations",
              required = true, multiValued = false)
    String numOfRuns = null;

    @Override
    protected void doExecute() {
        FlowRuleService flowService = get(FlowRuleService.class);
        DeviceService deviceService = get(DeviceService.class);
        CoreService coreService = get(CoreService.class);

        ApplicationId appId = coreService.registerApplication("onos.test.flow.installer");

        int flowsPerDevice = Integer.parseInt(flows);
        int num = Integer.parseInt(numOfRuns);

        ArrayList<Long> results = Lists.newArrayList();
        Iterable<Device> devices = deviceService.getDevices();
        TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                .setOutput(PortNumber.portNumber(RandomUtils.nextInt(MAX_OUT_PORT))).build();
        TrafficSelector.Builder sbuilder;
        FlowRuleOperations.Builder rules = FlowRuleOperations.builder();
        FlowRuleOperations.Builder remove = FlowRuleOperations.builder();

        for (Device d : devices) {
            for (long i = 0; i < flowsPerDevice; i++) {
                sbuilder = DefaultTrafficSelector.builder();

                sbuilder.matchEthSrc(MacAddress.valueOf(RandomUtils.nextInt() * i))
                        .matchEthDst(MacAddress.valueOf((Integer.MAX_VALUE - i) * RandomUtils.nextInt()));


                int randomPriority = RandomUtils.nextInt(
                        FlowRule.MAX_PRIORITY - FlowRule.MIN_PRIORITY + 1) + FlowRule.MIN_PRIORITY;

                FlowRule addRule = DefaultFlowRule.builder()
                        .forDevice(d.id())
                        .withSelector(sbuilder.build())
                        .withTreatment(treatment)
                        .withPriority(randomPriority)
                        .fromApp(appId)
                        .makeTemporary(10)
                        .build();
                FlowRule removeRule = DefaultFlowRule.builder()
                        .forDevice(d.id())
                        .withSelector(sbuilder.build())
                        .withTreatment(treatment)
                        .withPriority(randomPriority)
                        .fromApp(appId)
                        .makeTemporary(10)
                        .build();

                rules.add(addRule);
                remove.remove(removeRule);

            }
        }
        // close stages
        rules.newStage();
        remove.newStage();

        for (int i = 0; i < num; i++) {
            printProgress("Run %d:", i);
            latch = new CountDownLatch(2);
            final CountDownLatch addSuccess = new CountDownLatch(1);
            printProgress("..batch add request");
            Stopwatch add = Stopwatch.createStarted();

            flowService.apply(rules.build(new FlowRuleOperationsContext() {

                private final Stopwatch timer = Stopwatch.createStarted();

                @Override
                public void onSuccess(FlowRuleOperations ops) {

                    timer.stop();
                    printProgress("..add success");
                    results.add(timer.elapsed(TimeUnit.MILLISECONDS));
                    if (results.size() == num) {
                        if (outputJson()) {
                            print("%s", json(new ObjectMapper(), true, results));
                        } else {
                            printTime(true, results);
                        }
                    }
                    latch.countDown();
                    addSuccess.countDown();
                }
            }));

            try {
                addSuccess.await();
                // wait until all flows reaches ADDED state
                while (!Streams.stream(flowService.getFlowEntriesById(appId))
                        .allMatch(fr -> fr.state() == FlowEntryState.ADDED)) {
                    Thread.sleep(100);
                }
                add.stop();
                printProgress("..completed %d ± 100 ms", add.elapsed(TimeUnit.MILLISECONDS));
            } catch (InterruptedException e1) {
                printProgress("Interrupted");
                Thread.currentThread().interrupt();
            }

            printProgress("..cleaning up");
            flowService.apply(remove.build(new FlowRuleOperationsContext() {
                @Override
                public void onSuccess(FlowRuleOperations ops) {
                    latch.countDown();
                }
            }));

            try {
                latch.await();
                while (!Iterables.isEmpty(flowService.getFlowEntriesById(appId))) {
                    Thread.sleep(500);
                }
            } catch (InterruptedException e) {
                printProgress("Interrupted.");
                Thread.currentThread().interrupt();
            }
        }
        if (outputJson()) {
            print("%s", json(new ObjectMapper(), true, results));
        } else {
            printTime(true, results);
        }
    }

    private Object json(ObjectMapper mapper, boolean isSuccess, ArrayList<Long> elapsed) {
        ObjectNode result = mapper.createObjectNode();
        result.put("Success", isSuccess);
        ArrayNode node = result.putArray("elapsed-time");
        for (Long v : elapsed) {
            node.add(v);
        }
        return result;
    }

    private void printProgress(String format, Object... args) {
        if (!outputJson()) {
            print(format, args);
        }
    }


    private void printTime(boolean isSuccess, ArrayList<Long> elapsed) {
        print("Run is %s.", isSuccess ? "success" : "failure");
        for (int i = 0; i < elapsed.size(); i++) {
            print("  Run %s : %s ms", i, elapsed.get(i));
        }
    }
}
