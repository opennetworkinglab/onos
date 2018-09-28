/*
 * Copyright 2018-present Open Networking Foundation
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
package org.onosproject.proxytest;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.cluster.NodeId;
import org.onosproject.net.DeviceId;

/**
 * Proxy test command.
 */
@Service
@Command(scope = "onos", name = "proxy-test", description = "Manipulate a distributed proxy")
public class ProxyTestCommand extends AbstractShellCommand {

    @Argument(
        index = 0,
        name = "operation",
        description = "Operation name",
        required = true,
        multiValued = false)
    String operation;

    @Argument(
        index = 1,
        name = "type",
        description = "Operation type, either \"node\" or \"master\"",
        required = true,
        multiValued = false)
    String type;

    @Argument(
        index = 2,
        name = "arg1",
        description = "Operation argument, either a device or node identifier",
        required = true,
        multiValued = false)
    String arg1;

    @Argument(
        index = 3,
        name = "arg2",
        description = "Operation argument",
        required = true,
        multiValued = false)
    String arg2;

    @Override
    protected void doExecute() {
        ProxyTest proxyTest = get(ProxyTest.class);
        TestProxy proxy;
        if ("node".equals(type)) {
            NodeId nodeId = NodeId.nodeId(arg1);
            proxy = proxyTest.getProxyFor(nodeId);
        } else if ("master".equals(type)) {
            DeviceId deviceId = DeviceId.deviceId(arg1);
            proxy = proxyTest.getProxyFor(deviceId);
        } else {
            throw new IllegalArgumentException("Unknown operation type " + type);
        }

        if ("sync".equals(operation)) {
            print("%s", proxy.testSync(arg2));
        } else if ("async".equals(operation)) {
            try {
                print("%s", proxy.testAsync(arg2).get(10, TimeUnit.SECONDS));
            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                throw new IllegalStateException(e);
            }
        } else {
            throw new IllegalArgumentException("Unknown operation " + operation);
        }
    }
}
