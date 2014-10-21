package org.onlab.onos.cli.net;

import org.apache.karaf.shell.commands.Option;
import org.onlab.onos.cli.AbstractShellCommand;
import org.onlab.onos.net.flow.DefaultTrafficSelector;
import org.onlab.onos.net.flow.TrafficSelector;
import org.onlab.packet.Ethernet;
import org.onlab.packet.MacAddress;

import com.google.common.base.Strings;

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

    /**
     * Constructs a traffic selector based on the command line arguments
     * presented to the command.
     */
    protected TrafficSelector buildTrafficSelector() {
        TrafficSelector.Builder selectorBuilder = DefaultTrafficSelector.builder();

        Short ethType = Ethernet.TYPE_IPV4;
        if (!Strings.isNullOrEmpty(ethTypeString)) {
            EthType ethTypeParameter = EthType.valueOf(ethTypeString);
            ethType = ethTypeParameter.value();
        }
        selectorBuilder.matchEthType(ethType);

        if (!Strings.isNullOrEmpty(srcMacString)) {
            selectorBuilder.matchEthSrc(MacAddress.valueOf(srcMacString));
        }

        if (!Strings.isNullOrEmpty(dstMacString)) {
            selectorBuilder.matchEthDst(MacAddress.valueOf(dstMacString));
        }

        return selectorBuilder.build();
    }

}
