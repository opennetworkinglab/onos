/*
 * Copyright 2015 Open Networking Laboratory
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
package org.onosproject.provider.netconf.flow.impl;

import static com.google.common.base.Strings.isNullOrEmpty;
import static org.onlab.util.Tools.get;
import static org.slf4j.LoggerFactory.getLogger;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.jboss.netty.util.HashedWheelTimer;
import org.jboss.netty.util.Timeout;
import org.jboss.netty.util.TimerTask;
import org.onlab.util.Timer;
import org.onosproject.core.ApplicationId;
import org.onosproject.net.DeviceId;
import org.onosproject.net.flow.FlowEntry;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.FlowRuleBatchOperation;
import org.onosproject.net.flow.FlowRuleProvider;
import org.onosproject.net.flow.FlowRuleProviderRegistry;
import org.onosproject.net.flow.FlowRuleProviderService;
import org.onosproject.net.provider.AbstractProvider;
import org.onosproject.net.provider.ProviderId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.acl.rev140520.AccessList;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.acl.rev140520.AccessListBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.acl.rev140520.access.list.AccessListEntries;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.acl.rev140520.access.list.AccessListEntriesBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.acl.rev140520.access.list.access.list.entries.ActionsBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.acl.rev140520.access.list.access.list.entries.Matches;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.acl.rev140520.access.list.access.list.entries.MatchesBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.acl.rev140520.access.list.access.list.entries.actions.packet.handling.DenyBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.acl.rev140520.access.list.access.list.entries.actions.packet.handling.PermitBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.acl.rev140520.access.list.access.list.entries.matches.ace.type.AceIp;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.acl.rev140520.access.list.access.list.entries.matches.ace.type.AceIpBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.acl.rev140520.access.list.access.list.entries.matches.ace.type.ace.ip.ace.ip.version.AceIpv4;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.acl.rev140520.access.list.access.list.entries.matches.ace.type.ace.ip.ace.ip.version.AceIpv4Builder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.PortNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.packet.fields.rev140625.acl.transport.header.fields.DestinationPortRange;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.packet.fields.rev140625.acl.transport.header.fields.DestinationPortRangeBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.packet.fields.rev140625.acl.transport.header.fields.SourcePortRange;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.packet.fields.rev140625.acl.transport.header.fields.SourcePortRangeBuilder;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;

/**
 * Netconf provider to accept any flow and report them.
 */
@Component(immediate = true)
public class NetconfFlowRuleProvider extends AbstractProvider
        implements FlowRuleProvider {
    private final Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected FlowRuleProviderRegistry providerRegistry;

    private ConcurrentMap<DeviceId, Set<FlowEntry>> flowTable = new ConcurrentHashMap<>();

    private FlowRuleProviderService providerService;

    private XmlBuilder xmlBuilder;

    private AceIp aceIp;
    private SourcePortRange srcPortRange;
    private DestinationPortRange destPortRange;
    private Matches matches;
    private HashedWheelTimer timer = Timer.getTimer();
    private Timeout timeout;
    private static final String ACL_NAME_KEY = "acl-name";
    private static final String ACL_LIST_ENTRIES_RULE_NAME_KEY = "access-list-entries.rule-name";
    private static final String ACL_LIST_SP_LOWER_KEY = "source-port-range.lower-port";
    private static final String ACL_LIST_SP_UPPER_KEY = "source-port-range.upper-port";
    private static final String ACL_LIST_DP_LOWER_KEY = "destination-port-range.lower-port";
    private static final String ACL_LIST_DP_UPPER_KEY = "destination-port-range.upper-port";
    private static final String ACL_LIST_DEST_IPV4_KEY = "matches.destination-ipv4-address";
    private static final String ACL_LIST_SRC_IPV4_KEY = "matches.source-ipv4-address";
    private static final String ACL_LIST_ACTIONS_KEY = "actions";

    public NetconfFlowRuleProvider() {
        super(new ProviderId("netconf", "org.onosproject.provider.netconf"));
    }

    @Activate
    public void activate(ComponentContext context) {
        providerService = providerRegistry.register(this);
        timeout = timer.newTimeout(new StatisticTask(), 5, TimeUnit.SECONDS);
        applyRule();
        modified(context);
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        providerRegistry.unregister(this);
        providerService = null;
        timeout.cancel();
        log.info("Stopped");
    }

    @Modified
    public void modified(ComponentContext context) {
        if (xmlBuilder == null) {
            xmlBuilder = new XmlBuilder();
        }
        if (context == null) {
            log.info("No configuration file");
            return;
        }
        Dictionary<?, ?> properties = context.getProperties();
        String deviceEntry = get(properties, "devConfigs");
        log.info("Settings: devConfigs={}", deviceEntry);
        Enumeration<?> elements = properties.keys();
        Object nextElement = elements.nextElement();
        while (elements.hasMoreElements()) {
            if (nextElement instanceof String) {
                log.info("key::" + nextElement + ", value::"
                        + get(properties, (String) nextElement));
            }
            nextElement = elements.nextElement();
        }
        if (!isNullOrEmpty(deviceEntry)) {
            Map<String, String> deviceMap = processDeviceEntry(deviceEntry);
            AccessList accessList = buildAccessList(properties);
            String xmlMsg = xmlBuilder.buildAclRequestXml(accessList);
            log.info("The resultant xml from the builder\n" + xmlMsg);
            NetconfOperation netconfOperation = new NetconfOperation();
            netconfOperation.sendXmlMessage(xmlMsg, deviceMap.get("username"),
                                            deviceMap.get("password"),
                                            deviceMap.get("hostIp"), Integer
                                                    .parseInt(deviceMap
                                                            .get("hostPort")));
        }
    }

    /**
     * @param properties
     * @return accessList
     */
    private AccessList buildAccessList(Dictionary<?, ?> properties) {
        /**
         * Populating Access List.
         */
        AccessListBuilder abuilder = new AccessListBuilder();
        String aclName = get(properties, ACL_NAME_KEY);
        if (aclName != null) {
            abuilder.setAclName(aclName);
        }
        AccessList accessList = abuilder.build();
        abuilder.setAccessListEntries(getAccessListEntries(properties, matches));
        srcPortRange = getSourcePortRange(properties);
        destPortRange = getDestinationPortRange(properties);
        aceIp = getAceIp(properties, srcPortRange, destPortRange);
        matches = getMatches(properties);
        return accessList;
    }

    /**
     * @param properties
     * @return matches
     */
    private Matches getMatches(Dictionary<?, ?> properties) {
        /**
         * Building Matches for given ACL model.
         */
        MatchesBuilder matchesBuilder = new MatchesBuilder();
        if (aceIp != null) {
            matchesBuilder.setAceType(aceIp);
        }
        matches = matchesBuilder.build();
        return matches;
    }

    /**
     * @param properties
     * @return srcPortRange
     */
    private SourcePortRange getSourcePortRange(Dictionary<?, ?> properties) {
        /**
         * Building Source Port Range for given ACL model.
         */
        String spRangeLowerStr = get(properties, ACL_LIST_SP_LOWER_KEY);
        String spRangeUpperStr = get(properties, ACL_LIST_SP_UPPER_KEY);
        SourcePortRangeBuilder srcPortRangeBuilder = new SourcePortRangeBuilder();
        if (spRangeLowerStr != null) {
            int spRangeLower = Integer.parseInt(spRangeLowerStr);
            srcPortRangeBuilder.setLowerPort(new PortNumber(spRangeLower));
        }
        if (spRangeUpperStr != null) {
            int spRangeUpper = Integer.parseInt(spRangeUpperStr);
            srcPortRangeBuilder.setUpperPort(new PortNumber(spRangeUpper));
        }
        srcPortRange = srcPortRangeBuilder.build();
        return srcPortRange;
    }

    /**
     * @param properties
     * @return destPortRange
     */
    private DestinationPortRange getDestinationPortRange(Dictionary<?, ?> properties) {
        /**
         * Building Destination Port Range for given ACL model.
         */
        String dpRangeLowerStr = get(properties, ACL_LIST_DP_LOWER_KEY);
        String dpRangeUpperStr = get(properties, ACL_LIST_DP_UPPER_KEY);
        DestinationPortRangeBuilder destPortRangeBuilder = new DestinationPortRangeBuilder();
        if (dpRangeLowerStr != null) {
            int dpRangeLower = Integer.parseInt(dpRangeLowerStr);
            destPortRangeBuilder.setLowerPort(new PortNumber(dpRangeLower));
        }
        if (dpRangeUpperStr != null) {
            int dpRangeUpper = Integer.parseInt(dpRangeUpperStr);
            destPortRangeBuilder.setUpperPort(new PortNumber(dpRangeUpper));
        }
        destPortRange = destPortRangeBuilder.build();
        return destPortRange;
    }

    /**
     * @param properties
     * @return accessListEntries
     */
    private List<AccessListEntries> getAccessListEntries(Dictionary<?, ?> properties,
                                                         Matches matches) {
        /**
         * Build and Populate Access List Entries.
         */
        AccessListEntriesBuilder acLListEntriesBuilder = new AccessListEntriesBuilder();
        String aclListEntriesRuleName = get(properties,
                                            ACL_LIST_ENTRIES_RULE_NAME_KEY);
        if (aclListEntriesRuleName != null) {
            acLListEntriesBuilder.setRuleName(aclListEntriesRuleName);
        }
        acLListEntriesBuilder.setMatches(matches);
        String aclActions = get(properties, ACL_LIST_ACTIONS_KEY);
        if (aclActions != null) {
            ActionsBuilder actionBuilder = new ActionsBuilder();
            if (aclActions.equalsIgnoreCase("deny")) {
                DenyBuilder denyBuilder = new DenyBuilder();
                actionBuilder.setPacketHandling(denyBuilder.build());
            } else if (aclActions.equalsIgnoreCase("permit")) {
                PermitBuilder permitBuilder = new PermitBuilder();
                actionBuilder.setPacketHandling(permitBuilder.build());
            }
            acLListEntriesBuilder.setActions(actionBuilder.build());
        }
        AccessListEntries aclListEntries = acLListEntriesBuilder.build();
        List<AccessListEntries> accessListEntries = new ArrayList<AccessListEntries>();
        accessListEntries.add(aclListEntries);
        return accessListEntries;
    }

    /**
     * @param properties
     * @return aceIp
     */
    private AceIp getAceIp(Dictionary<?, ?> properties,
                           SourcePortRange srcPortRange,
                           DestinationPortRange destPortRange) {
        /**
         * Building Ace IPV4 Type
         */
        String destIpv4 = get(properties, ACL_LIST_DEST_IPV4_KEY);
        String srcIpv4 = get(properties, ACL_LIST_SRC_IPV4_KEY);
        AceIpv4Builder aceIpv4Builder = new AceIpv4Builder();
        aceIp = null;
        if (destIpv4 != null) {
            Ipv4Prefix destinationIp = new Ipv4Prefix(destIpv4);
            aceIpv4Builder.setDestinationIpv4Address(destinationIp);
        }
        if (srcIpv4 != null) {
            Ipv4Prefix sourceIp = new Ipv4Prefix(srcIpv4);
            aceIpv4Builder.setSourceIpv4Address(sourceIp);
        }
        if (destIpv4 != null || srcIpv4 != null) {
            AceIpv4 aceIpv4 = aceIpv4Builder.build();
            AceIpBuilder aceIpBuilder = new AceIpBuilder();
            aceIpBuilder.setAceIpVersion(aceIpv4);
            aceIpBuilder.setSourcePortRange(srcPortRange);
            aceIpBuilder.setDestinationPortRange(destPortRange);
            aceIp = aceIpBuilder.build();
        }
        return aceIp;
    }

    /**
     * @param deviceEntry
     * @return deviceMap
     */
    private Map<String, String> processDeviceEntry(String deviceEntry) {
        if (deviceEntry == null) {
            log.info("No content for Device Entry, so cannot proceed further.");
            return null;
        }

        Map<String, String> deviceMap = new HashMap<String, String>();
        log.info("Trying to convert Device Entry String: " + deviceEntry
                + " to a Netconf Device Object");
        try {
            URI uri = new URI(deviceEntry);
            String path = uri.getPath();
            String userInfo = path.substring(path.lastIndexOf('@'));
            String hostInfo = path.substring(path.lastIndexOf('@') + 1);
            String[] infoSplit = userInfo.split(":");
            String username = infoSplit[0];
            String password = infoSplit[1];
            infoSplit = hostInfo.split(":");
            String hostIp = infoSplit[0];
            String hostPort = infoSplit[1];
            if (isNullOrEmpty(username) || isNullOrEmpty(password)
                    || isNullOrEmpty(hostIp) || isNullOrEmpty(hostPort)) {
                log.warn("Bad Configuration Data: both user and device"
                        + " information parts of Configuration " + deviceEntry
                        + " should be non-nullable");
            } else {
                deviceMap.put("hostIp", hostIp);
                deviceMap.put("hostPort", hostPort);
                deviceMap.put("username", username);
                deviceMap.put("password", password);
            }
        } catch (ArrayIndexOutOfBoundsException aie) {
            log.error("Error while reading config infromation from the config file: "
                              + "The user, host and device state infomation should be "
                              + "in the order 'userInfo@hostInfo:deviceState'"
                              + deviceEntry, aie);
        } catch (URISyntaxException urie) {
            log.error("Error while parsing config information for the device entry: "
                              + "Illegal character in path " + deviceEntry,
                      urie);
        } catch (Exception e) {
            log.error("Error while parsing config information for the device entry: "
                              + deviceEntry, e);
        }
        return deviceMap;
    }

    @Override
    public void applyFlowRule(FlowRule... flowRules) {
    }

    @Override
    public void removeFlowRule(FlowRule... flowRules) {
    }

    private void applyRule() {
        // applyFlowRule(flowRules);//currentl
    }

    @Override
    public void removeRulesById(ApplicationId id, FlowRule... flowRules) {
        log.info("removal by app id not supported in null provider");
    }

    @Override
    public void executeBatch(FlowRuleBatchOperation batch) {

    }

    private class StatisticTask implements TimerTask {

        @Override
        public void run(Timeout to) throws Exception {
            for (DeviceId devId : flowTable.keySet()) {
                providerService.pushFlowMetrics(devId, flowTable
                        .getOrDefault(devId, Collections.emptySet()));
            }
            timeout = timer.newTimeout(to.getTask(), 5, TimeUnit.SECONDS);

        }
    }
}
