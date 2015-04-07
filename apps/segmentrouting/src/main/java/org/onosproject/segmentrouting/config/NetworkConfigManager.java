package org.onosproject.segmentrouting.config;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.onosproject.net.DeviceId;
import org.onosproject.net.Link;
import org.onosproject.segmentrouting.config.NetworkConfig.LinkConfig;
import org.onosproject.segmentrouting.config.NetworkConfig.SwitchConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * NetworkConfigManager manages all network configuration for switches, links
 * and any other state that needs to be configured for correct network
 * operation.
 *
 */
public class NetworkConfigManager implements NetworkConfigService {
    protected static final Logger log = LoggerFactory
            .getLogger(NetworkConfigManager.class);
    private static final String CONFIG_DIR = "../config";
    private static final String DEFAULT_CONFIG_FILE = "segmentrouting.conf";
    private final String configFileName = DEFAULT_CONFIG_FILE;
    /**
     * JSON Config file needs to use one of the following types for defining the
     * kind of switch or link it wishes to configure.
     */
    public static final String SEGMENT_ROUTER = "Router_SR";

    public static final String PKT_LINK = "pktLink";

    NetworkConfig networkConfig;
    private ConcurrentMap<DeviceId, SwitchConfig> configuredSwitches;
    private ConcurrentMap<Link, LinkConfig> configuredLinks;
    private Map<String, DeviceId> nameToDpid;

    @Override
    public SwitchConfigStatus checkSwitchConfig(DeviceId dpid) {
        SwitchConfig swc = configuredSwitches.get(dpid);
        if (networkConfig.getRestrictSwitches()) {
            // default deny behavior
            if (swc == null) {
                // switch is not configured - we deny this switch
                return new SwitchConfigStatus(NetworkConfigState.DENY, null,
                        "Switch not configured, in network denying switches by default.");
            }
            if (swc.isAllowed()) {
                // switch is allowed in config, return configured attributes
                return new SwitchConfigStatus(NetworkConfigState.ACCEPT_ADD, swc);
            } else {
                // switch has been configured off (administratively down)
                return new SwitchConfigStatus(NetworkConfigState.DENY, null,
                        "Switch configured down (allowed=false).");
            }
        } else {
            // default allow behavior
            if (swc == null) {
                // no config to add
                return new SwitchConfigStatus(NetworkConfigState.ACCEPT, null);
            }
            if (swc.isAllowed()) {
                // switch is allowed in config, return configured attributes
                return new SwitchConfigStatus(NetworkConfigState.ACCEPT_ADD, swc);
            } else {
                // switch has been configured off (administratively down)
                return new SwitchConfigStatus(NetworkConfigState.DENY, null,
                        "Switch configured down (allowed=false).");
            }
        }

    }

    @Override
    public LinkConfigStatus checkLinkConfig(Link linkTuple) {
        LinkConfig lkc = getConfiguredLink(linkTuple);
        // links are always disallowed if any one of the nodes that make up the
        // link are disallowed
        DeviceId linkNode1 = linkTuple.src().deviceId();
        SwitchConfigStatus scs1 = checkSwitchConfig(linkNode1);
        if (scs1.getConfigState() == NetworkConfigState.DENY) {
            return new LinkConfigStatus(NetworkConfigState.DENY, null,
                    "Link-node: " + linkNode1 + " denied by config: " + scs1.getMsg());
        }
        DeviceId linkNode2 = linkTuple.dst().deviceId();
        SwitchConfigStatus scs2 = checkSwitchConfig(linkNode2);
        if (scs2.getConfigState() == NetworkConfigState.DENY) {
            return new LinkConfigStatus(NetworkConfigState.DENY, null,
                    "Link-node: " + linkNode2 + " denied by config: " + scs2.getMsg());
        }
        if (networkConfig.getRestrictLinks()) {
            // default deny behavior
            if (lkc == null) {
                // link is not configured - we deny this link
                return new LinkConfigStatus(NetworkConfigState.DENY, null,
                        "Link not configured, in network denying links by default.");
            }
            if (lkc.isAllowed()) {
                // link is allowed in config, return configured attributes
                return new LinkConfigStatus(NetworkConfigState.ACCEPT_ADD, lkc);
            } else {
                // link has been configured off (administratively down)
                return new LinkConfigStatus(NetworkConfigState.DENY, null,
                        "Link configured down (allowed=false).");
            }
        } else {
            // default allow behavior
            if (lkc == null) {
                // no config to add
                return new LinkConfigStatus(NetworkConfigState.ACCEPT, null);
            }
            if (lkc.isAllowed()) {
                // link is allowed in config, return configured attributes
                return new LinkConfigStatus(NetworkConfigState.ACCEPT_ADD, lkc);
            } else {
                // link has been configured off (administratively down)
                return new LinkConfigStatus(NetworkConfigState.DENY, null,
                        "Link configured down (allowed=false).");
            }
        }

    }

    @Override
    public List<SwitchConfig> getConfiguredAllowedSwitches() {
        List<SwitchConfig> allowed = new ArrayList<SwitchConfig>();
        for (SwitchConfig swc : configuredSwitches.values()) {
            if (swc.isAllowed()) {
                allowed.add(swc);
            }
        }
        return allowed;
    }

    @Override
    public List<LinkConfig> getConfiguredAllowedLinks() {
        List<LinkConfig> allowed = new ArrayList<LinkConfig>();
        for (LinkConfig lkc : configuredLinks.values()) {
            if (lkc.isAllowed()) {
                allowed.add(lkc);
            }
        }
        return allowed;
    }

    @Override
    public DeviceId getDpidForName(String name) {
        if (nameToDpid.get(name) != null) {
            return nameToDpid.get(name);
        }
        return null;
    }

    // **************
    // Private methods
    // **************

    private void loadNetworkConfig() {
        File configFile = new File(CONFIG_DIR, configFileName);
        ObjectMapper mapper = new ObjectMapper();
        networkConfig = new NetworkConfig();

        try {
            networkConfig = mapper.readValue(configFile,
                                             NetworkConfig.class);
        } catch (JsonParseException e) {
            String err = String.format("JsonParseException while loading network "
                    + "config from file: %s: %s", configFileName,
                    e.getMessage());
            throw new NetworkConfigException.ErrorConfig(err);
        } catch (JsonMappingException e) {
            String err = String.format(
                    "JsonMappingException while loading network config "
                            + "from file: %s: %s",
                            configFileName,
                            e.getMessage());
            throw new NetworkConfigException.ErrorConfig(err);
        } catch (IOException e) {
            String err = String.format("IOException while loading network config "
                    + "from file: %s %s", configFileName, e.getMessage());
            throw new NetworkConfigException.ErrorConfig(err);
        }

        log.info("Network config specifies: {} switches and {} links",
                (networkConfig.getRestrictSwitches())
                        ? networkConfig.getSwitchConfig().size() : "default allow",
                        (networkConfig.getRestrictLinks())
                        ? networkConfig.getLinkConfig().size() : "default allow");
    }

    private void parseNetworkConfig() {
        List<SwitchConfig> swConfList = networkConfig.getSwitchConfig();
        List<LinkConfig> lkConfList = networkConfig.getLinkConfig();
        validateSwitchConfig(swConfList);
        createTypeSpecificSwitchConfig(swConfList);
        validateLinkConfig(lkConfList);
        createTypeSpecificLinkConfig(lkConfList);
        // TODO validate reachability matrix 'names' for configured dpids
    }

    private void createTypeSpecificSwitchConfig(List<SwitchConfig> swConfList) {
        for (SwitchConfig swc : swConfList) {
            nameToDpid.put(swc.getName(), swc.getDpid());
            String swtype = swc.getType();
            switch (swtype) {
            case SEGMENT_ROUTER:
                SwitchConfig sr = new SegmentRouterConfig(swc);
                configuredSwitches.put(sr.getDpid(), sr);
                break;
            default:
                throw new NetworkConfigException.UnknownSwitchType(swtype,
                        swc.getName());
            }
        }
    }

    private void createTypeSpecificLinkConfig(List<LinkConfig> lkConfList) {
        for (LinkConfig lkc : lkConfList) {
            String lktype = lkc.getType();
            switch (lktype) {
            case PKT_LINK:
                PktLinkConfig pk = new PktLinkConfig(lkc);
                for (Link lt : pk.getLinkTupleList()) {
                    configuredLinks.put(lt, pk);
                }
                break;
            default:
                throw new NetworkConfigException.UnknownLinkType(lktype,
                        lkc.getNodeDpid1(), lkc.getNodeDpid2());
            }
        }
    }

    private void validateSwitchConfig(List<SwitchConfig> swConfList) {
        Set<DeviceId> swDpids = new HashSet<DeviceId>();
        Set<String> swNames = new HashSet<String>();
        for (SwitchConfig swc : swConfList) {
            if (swc.getNodeDpid() == null || swc.getDpid() == null) {
                throw new NetworkConfigException.DpidNotSpecified(swc.getName());
            }
            // ensure both String and DeviceId values of dpid are set
            if (!swc.getDpid().equals(DeviceId.deviceId(swc.getNodeDpid()))) {
                throw new NetworkConfigException.SwitchDpidNotConverted(
                        swc.getName());
            }
            if (swc.getName() == null) {
                throw new NetworkConfigException.NameNotSpecified(swc.getDpid());
            }
            if (swc.getType() == null) {
                throw new NetworkConfigException.SwitchTypeNotSpecified(
                        swc.getDpid());
            }
            if (!swDpids.add(swc.getDpid())) {
                throw new NetworkConfigException.DuplicateDpid(swc.getDpid());
            }
            if (!swNames.add(swc.getName())) {
                throw new NetworkConfigException.DuplicateName(swc.getName());
            }
            // TODO Add more validations
        }
    }

    private void validateLinkConfig(List<LinkConfig> lkConfList) {
        for (LinkConfig lkc : lkConfList) {
            if (lkc.getNodeDpid1() == null || lkc.getNodeDpid2() == null) {
                throw new NetworkConfigException.LinkDpidNotSpecified(
                        lkc.getNodeDpid1(), lkc.getNodeDpid2());
            }
            // ensure both String and Long values are set
            if (!lkc.getDpid1().equals(DeviceId.deviceId(lkc.getNodeDpid1())) ||
                    !lkc.getDpid2().equals(DeviceId.deviceId(lkc.getNodeDpid2()))) {
                throw new NetworkConfigException.LinkDpidNotConverted(
                        lkc.getNodeDpid1(), lkc.getNodeDpid2());
            }
            if (lkc.getType() == null) {
                throw new NetworkConfigException.LinkTypeNotSpecified(
                        lkc.getNodeDpid1(), lkc.getNodeDpid2());
            }
            if (configuredSwitches.get(lkc.getDpid1()) == null) {
                throw new NetworkConfigException.LinkForUnknownSwitchConfig(
                        lkc.getNodeDpid1());
            }
            if (configuredSwitches.get(lkc.getDpid2()) == null) {
                throw new NetworkConfigException.LinkForUnknownSwitchConfig(
                        lkc.getNodeDpid2());
            }
            // TODO add more validations
        }

    }

    private LinkConfig getConfiguredLink(Link linkTuple) {
        LinkConfig lkc = null;
        // first try the unidirectional link with the ports assigned
        lkc = configuredLinks.get(linkTuple);
        return lkc;
    }


    /**
     * Initializes the network configuration manager module by
     * loading and parsing the network configuration file.
     */
    public void init() {
        loadNetworkConfig();
        configuredSwitches = new ConcurrentHashMap<DeviceId, SwitchConfig>();
        configuredLinks = new ConcurrentHashMap<Link, LinkConfig>();
        nameToDpid = new HashMap<String, DeviceId>();
        parseNetworkConfig();
    }
}
