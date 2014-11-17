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

import java.util.LinkedList;
import java.util.List;

import org.apache.karaf.shell.commands.Option;
import org.onlab.onos.cli.AbstractShellCommand;
import org.onlab.onos.net.Link;
import org.onlab.onos.net.flow.DefaultTrafficSelector;
import org.onlab.onos.net.flow.TrafficSelector;
import org.onlab.onos.net.intent.Constraint;
import org.onlab.onos.net.intent.constraint.BandwidthConstraint;
import org.onlab.onos.net.intent.constraint.LambdaConstraint;
import org.onlab.onos.net.intent.constraint.LinkTypeConstraint;
import org.onlab.onos.net.resource.Bandwidth;
import org.onlab.packet.Ethernet;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.MacAddress;

import static com.google.common.base.Strings.isNullOrEmpty;

/**
 * Base class for command line operations for connectivity based intents.
 */
public abstract class ConnectivityIntentCommand extends AbstractShellCommand {

    @Option(name = "-s", aliases = "--ethSrc", description = "Source MAC Address",
            required = false, multiValued = false)
    private String srcMacString = null;

    @Option(name = "-d", aliases = "--ethDst", description = "Destination MAC Address",
            required = false, multiValued = false)
    private String dstMacString = null;

    @Option(name = "-t", aliases = "--ethType", description = "Ethernet Type",
            required = false, multiValued = false)
    private String ethTypeString = "";

    @Option(name = "--ipProto", description = "IP Protocol",
            required = false, multiValued = false)
    private String ipProtoString = null;

    @Option(name = "--ipSrc", description = "Source IP Address",
            required = false, multiValued = false)
    private String srcIpString = null;

    @Option(name = "--ipDst", description = "Destination IP Address",
            required = false, multiValued = false)
    private String dstIpString = null;

    @Option(name = "--tcpSrc", description = "Source TCP Port",
            required = false, multiValued = false)
    private String srcTcpString = null;

    @Option(name = "--tcpDst", description = "Destination TCP Port",
            required = false, multiValued = false)
    private String dstTcpString = null;

    @Option(name = "-b", aliases = "--bandwidth", description = "Bandwidth",
            required = false, multiValued = false)
    private String bandwidthString = "";

    @Option(name = "-l", aliases = "--lambda", description = "Lambda",
            required = false, multiValued = false)
    private boolean lambda = false;

    /**
     * Constructs a traffic selector based on the command line arguments
     * presented to the command.
     * @return traffic selector
     */
    protected TrafficSelector buildTrafficSelector() {
        TrafficSelector.Builder selectorBuilder = DefaultTrafficSelector.builder();
        Short ethType = Ethernet.TYPE_IPV4;

        if (!isNullOrEmpty(ethTypeString)) {
            EthType ethTypeParameter = EthType.valueOf(ethTypeString);
            ethType = ethTypeParameter.value();
        }
        selectorBuilder.matchEthType(ethType);

        if (!isNullOrEmpty(srcMacString)) {
            selectorBuilder.matchEthSrc(MacAddress.valueOf(srcMacString));
        }

        if (!isNullOrEmpty(dstMacString)) {
            selectorBuilder.matchEthDst(MacAddress.valueOf(dstMacString));
        }

        if (!isNullOrEmpty(ipProtoString)) {
            selectorBuilder.matchIPProtocol((byte) Short.parseShort(ipProtoString));
        }

        if (!isNullOrEmpty(srcIpString)) {
            selectorBuilder.matchIPSrc(IpPrefix.valueOf(srcIpString));
        }

        if (!isNullOrEmpty(dstIpString)) {
            selectorBuilder.matchIPDst(IpPrefix.valueOf(dstIpString));
        }

        if (!isNullOrEmpty(srcTcpString)) {
            selectorBuilder.matchTcpSrc((short) Integer.parseInt(srcTcpString));
        }

        if (!isNullOrEmpty(dstTcpString)) {
            selectorBuilder.matchTcpDst((short) Integer.parseInt(dstTcpString));
        }

        return selectorBuilder.build();
    }

    /**
     * Builds the constraint list for this command based on the command line
     * parameters.
     *
     * @return List of constraint objects describing the constraints requested
     */
    protected List<Constraint> buildConstraints() {
        final List<Constraint> constraints = new LinkedList<>();

        // Check for a bandwidth specification
        if (!isNullOrEmpty(bandwidthString)) {
            final double bandwidthValue = Double.parseDouble(bandwidthString);
            constraints.add(new BandwidthConstraint(Bandwidth.valueOf(bandwidthValue)));
        }

        // Check for a lambda specification
        if (lambda) {
            constraints.add(new LambdaConstraint(null));
        }
        constraints.add(new LinkTypeConstraint(lambda, Link.Type.OPTICAL));

        return constraints;
    }
}
