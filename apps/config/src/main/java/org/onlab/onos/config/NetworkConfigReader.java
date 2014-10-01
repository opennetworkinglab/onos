package org.onlab.onos.config;

import static org.slf4j.LoggerFactory.getLogger;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.codehaus.jackson.map.ObjectMapper;
import org.onlab.onos.net.ConnectPoint;
import org.onlab.onos.net.DeviceId;
import org.onlab.onos.net.PortNumber;
import org.onlab.onos.net.host.HostAdminService;
import org.onlab.onos.net.host.PortAddresses;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.MacAddress;
import org.slf4j.Logger;

/**
 * Simple configuration module to read in supplementary network configuration
 * from a file.
 */
@Component(immediate = true)
public class NetworkConfigReader {

    private final Logger log = getLogger(getClass());

    private static final String DEFAULT_CONFIG_FILE = "config/addresses.json";
    private String configFileName = DEFAULT_CONFIG_FILE;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected HostAdminService hostAdminService;

    @Activate
    protected void activate() {
        log.info("Started network config reader");

        log.info("Config file set to {}", configFileName);

        AddressConfiguration config = readNetworkConfig();

        if (config != null) {
            for (AddressEntry entry : config.getAddresses()) {

                ConnectPoint cp = new ConnectPoint(
                        DeviceId.deviceId(dpidToUri(entry.getDpid())),
                        PortNumber.portNumber(entry.getPortNumber()));

                Set<IpPrefix> ipAddresses = new HashSet<IpPrefix>();

                for (String strIp : entry.getIpAddresses()) {
                    try {
                        IpPrefix address = IpPrefix.valueOf(strIp);
                        ipAddresses.add(address);
                    } catch (IllegalArgumentException e) {
                        log.warn("Bad format for IP address in config: {}", strIp);
                    }
                }

                MacAddress macAddress = null;
                if (entry.getMacAddress() != null) {
                    try {
                        macAddress = MacAddress.valueOf(entry.getMacAddress());
                    } catch (IllegalArgumentException e) {
                        log.warn("Bad format for MAC address in config: {}",
                                entry.getMacAddress());
                    }
                }

                PortAddresses addresses = new PortAddresses(cp,
                        ipAddresses, macAddress);

                hostAdminService.bindAddressesToPort(addresses);
            }
        }
    }

    @Deactivate
    protected void deactivate() {
        log.info("Stopped");
    }

    private AddressConfiguration readNetworkConfig() {
        File configFile = new File(configFileName);

        ObjectMapper mapper = new ObjectMapper();

        try {
            AddressConfiguration config =
                    mapper.readValue(configFile, AddressConfiguration.class);

            return config;
        } catch (FileNotFoundException e) {
            log.warn("Configuration file not found: {}", configFileName);
        } catch (IOException e) {
            log.error("Unable to read config from file:", e);
        }

        return null;
    }

    private static String dpidToUri(String dpid) {
        return "of:" + dpid.replace(":", "");
    }
}
