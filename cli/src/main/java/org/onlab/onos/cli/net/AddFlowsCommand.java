/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.onlab.onos.cli.net;

import com.google.common.collect.Sets;
import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.onlab.onos.cli.AbstractShellCommand;
import org.onlab.onos.net.Device;
import org.onlab.onos.net.PortNumber;
import org.onlab.onos.net.device.DeviceService;
import org.onlab.onos.net.flow.CompletedBatchOperation;
import org.onlab.onos.net.flow.DefaultFlowRule;
import org.onlab.onos.net.flow.DefaultTrafficSelector;
import org.onlab.onos.net.flow.DefaultTrafficTreatment;
import org.onlab.onos.net.flow.FlowRuleBatchEntry;
import org.onlab.onos.net.flow.FlowRuleBatchOperation;
import org.onlab.onos.net.flow.FlowRuleService;
import org.onlab.onos.net.flow.TrafficSelector;
import org.onlab.onos.net.flow.TrafficTreatment;
import org.onlab.packet.MacAddress;

import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * Installs many many flows.
 */
@Command(scope = "onos", name = "add-flows",
         description = "Installs a flow rules")
public class AddFlowsCommand extends AbstractShellCommand {

    @Argument(index = 0, name = "flowPerDevice", description = "Number of flows to add per device",
              required = true, multiValued = false)
    String flows = null;


    @Override
    protected void execute() {

        FlowRuleService flowService = get(FlowRuleService.class);
        DeviceService deviceService = get(DeviceService.class);

        int flowsPerDevice = Integer.parseInt(flows);

        Iterable<Device> devices = deviceService.getDevices();
        TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                .setOutput(PortNumber.portNumber(1)).build();
        TrafficSelector.Builder sbuilder;
        Set<FlowRuleBatchEntry> rules = Sets.newHashSet();
        Set<FlowRuleBatchEntry> remove = Sets.newHashSet();
        for (Device d : devices) {
            for (int i = 0; i < flowsPerDevice; i++) {
                sbuilder = DefaultTrafficSelector.builder();
                sbuilder.matchEthSrc(MacAddress.valueOf(i))
                        .matchEthDst(MacAddress.valueOf(Integer.MAX_VALUE - i));
                rules.add(new FlowRuleBatchEntry(FlowRuleBatchEntry.FlowRuleOperation.ADD,
                                                    new DefaultFlowRule(d.id(), sbuilder.build(), treatment,
                                                                        100, (long) 0, 10, false)));
                remove.add(new FlowRuleBatchEntry(FlowRuleBatchEntry.FlowRuleOperation.REMOVE,
                                                 new DefaultFlowRule(d.id(), sbuilder.build(), treatment,
                                                                     100, (long) 0, 10, false)));

            }
        }
        long startTime = System.currentTimeMillis();
        Future<CompletedBatchOperation> op =  flowService.applyBatch(
                new FlowRuleBatchOperation(rules));
        CompletedBatchOperation completed = null;
        try {
            completed = op.get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        long endTime = System.currentTimeMillis();
        print("%s AFTER ELAPSED TIME %s", completed.isSuccess() ? "SUCCESS" : "FAILURE",
              endTime - startTime);

        flowService.applyBatch(
                new FlowRuleBatchOperation(remove));

    }

}
