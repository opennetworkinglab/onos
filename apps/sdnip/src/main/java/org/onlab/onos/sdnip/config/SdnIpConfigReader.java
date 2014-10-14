package org.onlab.onos.sdnip.config;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.codehaus.jackson.map.ObjectMapper;
import org.onlab.packet.IpAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SDN-IP Config Reader provides IConfigInfoService
 * by reading from an SDN-IP configuration file.
 * It must be enabled on the nodes within the cluster
 * not running SDN-IP.
 * <p/>
 * TODO: As a long term solution, a module providing
 * general network configuration to ONOS nodes should be used.
 */
public class SdnIpConfigReader implements SdnIpConfigService {

    private static final Logger log = LoggerFactory.getLogger(SdnIpConfigReader.class);

    private static final String DEFAULT_CONFIG_FILE = "config/sdnip.json";
    private String configFileName = DEFAULT_CONFIG_FILE;
    //private Map<String, Interface> interfaces;
    // We call the BGP routers in our SDN network the BGP speakers, and call
    // the BGP routers outside our SDN network the BGP peers.
    private Map<String, BgpSpeaker> bgpSpeakers;
    private Map<IpAddress, BgpPeer> bgpPeers;
    //private InvertedRadixTree<Interface> interfaceRoutes;

    /**
     * Reads the info contained in the configuration file.
     *
     * @param configFilename The name of configuration file for SDN-IP application.
     */
    private void readConfiguration(String configFilename) {
        File gatewaysFile = new File(configFilename);
        ObjectMapper mapper = new ObjectMapper();

        try {
            Configuration config = mapper.readValue(gatewaysFile, Configuration.class);
            /*interfaces = new ConcurrentHashMap<>();
            for (Interface intf : config.getInterfaces()) {
                interfaces.put(intf.getName(), intf);
            }*/
            bgpSpeakers = new ConcurrentHashMap<>();
            for (BgpSpeaker speaker : config.getBgpSpeakers()) {
                bgpSpeakers.put(speaker.getSpeakerName(), speaker);
            }
            bgpPeers = new ConcurrentHashMap<>();
            for (BgpPeer peer : config.getPeers()) {
                bgpPeers.put(peer.getIpAddress(), peer);
            }
        } catch (IOException e) {
            log.error("Error reading JSON file", e);
            //throw new ConfigurationRuntimeException("Error in JSON file", e);
        }

        // Populate the interface InvertedRadixTree
        /*for (Interface intf : interfaces.values()) {
            Ip4Prefix prefix = intf.getIp4Prefix();
            String binaryString = RouteEntry.createBinaryString(prefix);
            interfaceRoutes.put(binaryString, intf);
        }*/
    }

    /**
     * To find the Interface which has longest matchable IP prefix (sub-network
     *  prefix) to next hop IP address.
     *
     * @param address the IP address of next hop router
     * @return the Interface which has longest matchable IP prefix
     */
    /*private Interface longestInterfacePrefixMatch(IpAddress address) {
        Ip4Prefix prefixToSearchFor =
            new Ip4Prefix(address, (short) Ip4Address.BIT_LENGTH);
        String binaryString = RouteEntry.createBinaryString(prefixToSearchFor);

        Iterator<Interface> it =
            interfaceRoutes.getValuesForKeysPrefixing(binaryString).iterator();
        Interface intf = null;
        // Find the last prefix, which will be the longest prefix
        while (it.hasNext()) {
            intf = it.next();
        }

        return intf;
    }*/

    /*@Override
    public Interface getOutgoingInterface(IpAddress dstIpAddress) {
        return longestInterfacePrefixMatch(dstIpAddress);
    }*/

    public void init() {
        //interfaceRoutes = new ConcurrentInvertedRadixTree<>(
                //new DefaultByteArrayNodeFactory());

        // Reading config values
        /*String configFilenameParameter = context.getConfigParams(this).get("configfile");
        if (configFilenameParameter != null) {
            currentConfigFilename = configFilenameParameter;
        }*/
        log.debug("Config file set to {}", configFileName);

        readConfiguration(configFileName);
    }

    /*@Override
    public Map<String, Interface> getInterfaces() {
        return Collections.unmodifiableMap(interfaces);
    }*/

    @Override
    public Map<String, BgpSpeaker> getBgpSpeakers() {
        return Collections.unmodifiableMap(bgpSpeakers);
    }

    @Override
    public Map<IpAddress, BgpPeer> getBgpPeers() {
        return Collections.unmodifiableMap(bgpPeers);
    }

    static String dpidToUri(String dpid) {
        return "of:" + dpid.replace(":", "");
    }
}
