/*
 * Copyright 2016 Open Networking Laboratory
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
 *
 * Application developed by Elisa Rojas Sanchez
 */
package org.ctpd.closfwd;
import java.util.concurrent.TimeUnit;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.Dictionary;
import java.util.Optional;
import java.util.Properties;
import javax.swing.Timer;
import java.lang.Integer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.NumberDeserializers.IntegerDeserializer;
import com.fasterxml.jackson.databind.node.ObjectNode;
import javax.ws.rs.core.Response;
import org.onlab.packet.*;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
// import org.osgi.service.component.annotations.Property;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
// import org.osgi.service.component.annotations.Service;
//import org.apache.sshd.common.channel.AbstractChannel.GracefulChannelCloseable;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onlab.util.XmlString;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.cli.net.DeviceIdCompleter;
import org.onosproject.cli.net.PortNumberCompleter;

import java.lang.Math.*;
import org.onosproject.net.flowobjective.*;

// import java.security.acl.Group;
import java.util.*;


//import org.ctpd.closfwd..*;

//import org.ctpd.closfwd..UUIDPrefixList;
import org.onlab.packet.*;
import org.onlab.packet.ndp.NeighborSolicitation;
import org.onlab.packet.ndp.NeighborDiscoveryOptions;
import org.onlab.util.KryoNamespace;
import org.onlab.util.*;
import org.onosproject.cfg.ComponentConfigService;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.DefaultApplicationId;
import org.onosproject.core.CoreService;
/*API*/
import org.onosproject.event.AbstractListenerManager;
import org.onosproject.event.Event;
import org.onosproject.net.DeviceId;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.device.DeviceEvent.Type;
import org.onosproject.net.Device;
import org.onosproject.net.Link;
import org.onosproject.net.flow.FlowEntry;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.link.LinkService;
import org.onosproject.net.PortNumber;
import org.onosproject.net.behaviour.DeviceCpuStats;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flowobjective.FlowObjectiveService;
//import org.onosproject.net.flowobjective.ForwardingObjective.Builder;
//import org.onosproject.net.flowobjective.DefaultForwardingObjective.Builder;
//import org.onosproject.net.flowobjective.FilteringObjective.Builder;
//import org.onosproject.net.flowobjective.DefaultFilteringObjective.Builder;
import com.google.common.collect.ImmutableList;
import org.onosproject.net.flowobjective.ObjectiveError;
import org.onosproject.net.group.GroupService;
import org.onosproject.net.group.Group.GroupState;
import org.onosproject.core.GroupId;
import org.onosproject.net.packet.PacketProcessor;
import org.onosproject.net.packet.PacketService;
import org.onosproject.net.packet.PacketPriority;
import org.onosproject.net.link.LinkEvent;
import org.onosproject.net.device.DeviceEvent;
import org.onosproject.net.topology.TopologyEvent;
import org.onosproject.net.topology.TopologyListener;
import org.onosproject.net.topology.TopologyService;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.*;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.onosproject.net.Port;
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.IntentService;
import org.onosproject.net.intent.IntentState;
import org.onosproject.net.intent.Key;
import org.onosproject.net.group.*;
import org.onosproject.net.link.LinkEvent;
import org.onosproject.event.AbstractEvent;
import org.onosproject.net.ConnectPoint;
import org.onosproject.mastership.*;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.ControllerNode;
import org.onosproject.cluster.Node;
import org.onosproject.cluster.NodeId;
import org.onosproject.net.flow.instructions.Instruction;
import org.onosproject.net.flow.instructions.Instructions.GroupInstruction;
import org.onosproject.net.flow.instructions.Instructions.OutputInstruction;
import org.onosproject.net.flow.instructions.L2ModificationInstruction.ModEtherInstruction;
import org.onosproject.net.flow.instructions.L2ModificationInstruction;

import static com.google.common.base.Strings.isNullOrEmpty;
import static org.onlab.util.Tools.get;
import static org.onlab.util.Tools.retryable;
import org.ctpd.closfwd.EndpointManager;
import org.onosproject.net.device.DeviceAdminService;


import com.google.common.collect.Lists;


import org.onosproject.net.MastershipRole;
import org.onosproject.mastership.MastershipTerm;

import org.onosproject.net.device.DeviceListener;
import org.ctpd.closfwd.CustomiceException;
import org.ctpd.closfwd.ControllerEndpoint;
import java.lang.*;
import org.apache.commons.collections.IteratorUtils;

import static org.ctpd.closfwd.OsgiPropertyConstants.FLOW_TIMEOUT;
import static org.ctpd.closfwd.OsgiPropertyConstants.FLOW_PRIORITY;
import static org.ctpd.closfwd.OsgiPropertyConstants.BYPASS_FLOW_PRIORITY;
import static org.ctpd.closfwd.OsgiPropertyConstants.CTPD_FAKE_INTERNAL_MAC_ADDRESS;
import static org.ctpd.closfwd.OsgiPropertyConstants.CTPD_FAKE_EXTERNAL_MAC_ADDRESS;
import static org.ctpd.closfwd.OsgiPropertyConstants.VPDC_CLIENT_PREFIX_LENGTH;
import static org.ctpd.closfwd.OsgiPropertyConstants.L1_OPENFLOW_SWITCH_PREFIX;
import static org.ctpd.closfwd.OsgiPropertyConstants.L4_OPENFLOW_SWITCH_PREFIX;
import static org.ctpd.closfwd.OsgiPropertyConstants.SPINE_OPENFLOW_SWITCH_PREFIX;
import static org.ctpd.closfwd.OsgiPropertyConstants.NEIGHBOUR_SOLICITATION_INTERVAL;
import static org.ctpd.closfwd.OsgiPropertyConstants.FLOW_INSTALLATION_TIMEOUT;
import static org.ctpd.closfwd.OsgiPropertyConstants.OFDPA_ACTIVATED;
import static org.ctpd.closfwd.OsgiPropertyConstants.EMPTY_VLAN_ID;
import static org.ctpd.closfwd.OsgiPropertyConstants.SERVICE_VLAN_ID;
import static org.ctpd.closfwd.OsgiPropertyConstants.BYPASS_VLAN_ID;
import static org.ctpd.closfwd.OsgiPropertyConstants.EXTERNAL_SERVICE_VLAN_ID;
import static org.ctpd.closfwd.OsgiPropertyConstants.MONOETIQUETA;
import static org.ctpd.closfwd.OsgiPropertyConstants.DEFAULT_VLAN_ID;
import static org.ctpd.closfwd.OsgiPropertyConstants.INTERNET_SERVICES_FLOW_PRIOTITY;
import static org.ctpd.closfwd.OsgiPropertyConstants.RESPOND_NDP_LOCALLY;
import static org.ctpd.closfwd.OsgiPropertyConstants.PACKET_PRIORITY_PROCESSOR;
import static org.ctpd.closfwd.OsgiPropertyConstants.STREAM_ACTIVATED;
import static org.ctpd.closfwd.OsgiPropertyConstants.CTPD_IPV6;
import static org.ctpd.closfwd.OsgiPropertyConstants.CTPD_IPV4;
import static org.ctpd.closfwd.OsgiPropertyConstants.BGP_IPV4;
import static org.ctpd.closfwd.OsgiPropertyConstants.BGP_IPV6;
import static org.ctpd.closfwd.OsgiPropertyConstants.CLIENT_SERVICE_IDLE_TIMEOUT;
import static org.ctpd.closfwd.OsgiPropertyConstants.KEEP_DATA;
import static org.ctpd.closfwd.OsgiPropertyConstants.CHECK_LOCAL_NDP;
import static org.ctpd.closfwd.OsgiPropertyConstants.VPDC_CLIENT_PREFIX;
import static org.ctpd.closfwd.OsgiPropertyConstants.DEFAULT_VPDC_INTERNAL_PREFIX;
import static org.ctpd.closfwd.OsgiPropertyConstants.CTPD_MAC_PREFIX;
import static org.ctpd.closfwd.OsgiPropertyConstants.SERVICE_IPV6_PREFIX;
import static org.ctpd.closfwd.OsgiPropertyConstants.SERVICE_IPV4_PREFIX;
import static org.ctpd.closfwd.OsgiPropertyConstants.FLOW_ID_START;
import static org.ctpd.closfwd.OsgiPropertyConstants.USE_ECMP;
import static org.ctpd.closfwd.OsgiPropertyConstants.PRODUCTION_ENVIRONMENT;
import static org.ctpd.closfwd.OsgiPropertyConstants.STORAGE_NETWORK_ENABLED;
import static org.ctpd.closfwd.OsgiPropertyConstants.OLT_OPENFLOW_SWITCH_PREFIX;
import static org.ctpd.closfwd.OsgiPropertyConstants.VPDC_CLIENT_PREFIX;
import static org.ctpd.closfwd.OsgiPropertyConstants.VPDC_INTERNAL_PREFIX;

import static org.ctpd.closfwd.OsgiPropertyConstants.DEFAULT_FLOW_TIMEOUT;
import static org.ctpd.closfwd.OsgiPropertyConstants.DEFAULT_FLOW_PRIORITY;
import static org.ctpd.closfwd.OsgiPropertyConstants.DEFAULT_BYPASS_FLOW_PRIORITY;
import static org.ctpd.closfwd.OsgiPropertyConstants.DEFAULT_CTPD_FAKE_INTERNAL_MAC_ADDRESS;
import static org.ctpd.closfwd.OsgiPropertyConstants.DEFAULT_CTPD_FAKE_EXTERNAL_MAC_ADDRESS;
import static org.ctpd.closfwd.OsgiPropertyConstants.DEFAULT_VPDC_CLIENT_PREFIX_LENGTH;
import static org.ctpd.closfwd.OsgiPropertyConstants.DEFAULT_L1_OPENFLOW_SWITCH_PREFIX;
import static org.ctpd.closfwd.OsgiPropertyConstants.DEFAULT_L4_OPENFLOW_SWITCH_PREFIX;
import static org.ctpd.closfwd.OsgiPropertyConstants.DEFAULT_SPINE_OPENFLOW_SWITCH_PREFIX;
import static org.ctpd.closfwd.OsgiPropertyConstants.DEFAULT_NEIGHBOUR_SOLICITATION_INTERVAL;
import static org.ctpd.closfwd.OsgiPropertyConstants.DEFAULT_FLOW_INSTALLATION_TIMEOUT;
import static org.ctpd.closfwd.OsgiPropertyConstants.DEFAULT_OFDPA_ACTIVATED;
import static org.ctpd.closfwd.OsgiPropertyConstants.DEFAULT_EMPTY_VLAN_ID;
import static org.ctpd.closfwd.OsgiPropertyConstants.DEFAULT_SERVICE_VLAN_ID;
import static org.ctpd.closfwd.OsgiPropertyConstants.DEFAULT_BYPASS_VLAN_ID;
import static org.ctpd.closfwd.OsgiPropertyConstants.DEFAULT_EXTERNAL_SERVICE_VLAN_ID;
import static org.ctpd.closfwd.OsgiPropertyConstants.DEFAULT_MONOETIQUETA;
import static org.ctpd.closfwd.OsgiPropertyConstants.DEFAULT_DEFAULT_VLAN_ID;
import static org.ctpd.closfwd.OsgiPropertyConstants.DEFAULT_INTERNET_SERVICES_FLOW_PRIOTITY;
import static org.ctpd.closfwd.OsgiPropertyConstants.DEFAULT_RESPOND_NDP_LOCALLY;
import static org.ctpd.closfwd.OsgiPropertyConstants.DEFAULT_PACKET_PRIORITY_PROCESSOR;
import static org.ctpd.closfwd.OsgiPropertyConstants.DEFAULT_STREAM_ACTIVATED;
import static org.ctpd.closfwd.OsgiPropertyConstants.DEFAULT_CTPD_IPV6;
import static org.ctpd.closfwd.OsgiPropertyConstants.DEFAULT_CTPD_IPV4;
import static org.ctpd.closfwd.OsgiPropertyConstants.DEFAULT_BGP_IPV4;
import static org.ctpd.closfwd.OsgiPropertyConstants.DEFAULT_BGP_IPV6;
import static org.ctpd.closfwd.OsgiPropertyConstants.DEFAULT_CLIENT_SERVICE_IDLE_TIMEOUT;
import static org.ctpd.closfwd.OsgiPropertyConstants.DEFAULT_KEEP_DATA;
import static org.ctpd.closfwd.OsgiPropertyConstants.DEFAULT_CHECK_LOCAL_NDP;
import static org.ctpd.closfwd.OsgiPropertyConstants.DEFAULT_VPDC_CLIENT_PREFIX;
import static org.ctpd.closfwd.OsgiPropertyConstants.DEFAULT_VPDC_INTERNAL_PREFIX;
import static org.ctpd.closfwd.OsgiPropertyConstants.DEFAULT_CTPD_MAC_PREFIX;
import static org.ctpd.closfwd.OsgiPropertyConstants.DEFAULT_SERVICE_IPV6_PREFIX;
import static org.ctpd.closfwd.OsgiPropertyConstants.DEFAULT_SERVICE_IPV4_PREFIX;
import static org.ctpd.closfwd.OsgiPropertyConstants.DEFAULT_FLOW_ID_START;
import static org.ctpd.closfwd.OsgiPropertyConstants.DEFAULT_USE_ECMP;
import static org.ctpd.closfwd.OsgiPropertyConstants.DEFAULT_PRODUCTION_ENVIRONMENT;
import static org.ctpd.closfwd.OsgiPropertyConstants.DEFAULT_STORAGE_NETWORK_ENABLED;
import static org.ctpd.closfwd.OsgiPropertyConstants.DEFAULT_OLT_OPENFLOW_SWITCH_PREFIX;
import static org.ctpd.closfwd.OsgiPropertyConstants.DEFAULT_VPDC_CLIENT_PREFIX;
import static org.ctpd.closfwd.OsgiPropertyConstants.DEFAULT_VPDC_INTERNAL_PREFIX;
import org.ctpd.closfwd.ClosDeviceService;



@Service
@Component(
	immediate = true,
	service = ClosDeviceService.class,
	property = {
		FLOW_TIMEOUT + ":Integer=" + DEFAULT_FLOW_TIMEOUT,
		FLOW_PRIORITY + ":Integer=" + DEFAULT_FLOW_PRIORITY,
		BYPASS_FLOW_PRIORITY + ":Integer=" + DEFAULT_BYPASS_FLOW_PRIORITY,
		CTPD_FAKE_INTERNAL_MAC_ADDRESS + ":String=" + DEFAULT_CTPD_FAKE_INTERNAL_MAC_ADDRESS,
		CTPD_FAKE_EXTERNAL_MAC_ADDRESS + ":String=" + DEFAULT_CTPD_FAKE_EXTERNAL_MAC_ADDRESS,
		VPDC_CLIENT_PREFIX_LENGTH + ":Integer=" + DEFAULT_VPDC_CLIENT_PREFIX_LENGTH,
		L1_OPENFLOW_SWITCH_PREFIX + ":String=" + DEFAULT_L1_OPENFLOW_SWITCH_PREFIX,
		L4_OPENFLOW_SWITCH_PREFIX + ":String=" + DEFAULT_L4_OPENFLOW_SWITCH_PREFIX,
		SPINE_OPENFLOW_SWITCH_PREFIX + ":String=" + DEFAULT_SPINE_OPENFLOW_SWITCH_PREFIX,
		OLT_OPENFLOW_SWITCH_PREFIX + ":String=" + DEFAULT_OLT_OPENFLOW_SWITCH_PREFIX,
		NEIGHBOUR_SOLICITATION_INTERVAL + ":Integer=" + DEFAULT_NEIGHBOUR_SOLICITATION_INTERVAL,
		FLOW_INSTALLATION_TIMEOUT + ":Integer=" + DEFAULT_FLOW_INSTALLATION_TIMEOUT,
		OFDPA_ACTIVATED + ":Boolean=" + DEFAULT_OFDPA_ACTIVATED,
		EMPTY_VLAN_ID + ":Integer=" + DEFAULT_EMPTY_VLAN_ID,
		SERVICE_VLAN_ID + ":Integer=" + DEFAULT_SERVICE_VLAN_ID,
		BYPASS_VLAN_ID + ":Integer=" + DEFAULT_BYPASS_VLAN_ID,
		EXTERNAL_SERVICE_VLAN_ID + ":Integer=" + DEFAULT_EXTERNAL_SERVICE_VLAN_ID,
		MONOETIQUETA + ":Boolean=" + DEFAULT_MONOETIQUETA,
		DEFAULT_VLAN_ID + ":Integer=" + DEFAULT_DEFAULT_VLAN_ID,
		INTERNET_SERVICES_FLOW_PRIOTITY + ":Integer=" + DEFAULT_INTERNET_SERVICES_FLOW_PRIOTITY,
		RESPOND_NDP_LOCALLY + ":Boolean=" + DEFAULT_RESPOND_NDP_LOCALLY,
		PACKET_PRIORITY_PROCESSOR + ":Integer=" + DEFAULT_PACKET_PRIORITY_PROCESSOR,
		STREAM_ACTIVATED + ":Boolean=" + DEFAULT_STREAM_ACTIVATED,
		CTPD_IPV6 + ":String=" + DEFAULT_CTPD_IPV6,
		CTPD_IPV4 + ":String=" + DEFAULT_CTPD_IPV4,
		BGP_IPV4 + ":String=" + DEFAULT_BGP_IPV4,
		BGP_IPV6 + ":String=" + DEFAULT_BGP_IPV6,
		CLIENT_SERVICE_IDLE_TIMEOUT + ":Integer=" + DEFAULT_CLIENT_SERVICE_IDLE_TIMEOUT,
		KEEP_DATA + ":Boolean=" + DEFAULT_KEEP_DATA,
		CHECK_LOCAL_NDP + ":Boolean=" + DEFAULT_CHECK_LOCAL_NDP,
		VPDC_CLIENT_PREFIX + ":String=" + DEFAULT_VPDC_CLIENT_PREFIX,
		VPDC_INTERNAL_PREFIX + ":String=" + DEFAULT_VPDC_INTERNAL_PREFIX,
		CTPD_MAC_PREFIX + ":String=" + DEFAULT_CTPD_MAC_PREFIX,
		SERVICE_IPV6_PREFIX + ":String=" + DEFAULT_SERVICE_IPV6_PREFIX,
		SERVICE_IPV4_PREFIX + ":String=" + DEFAULT_SERVICE_IPV4_PREFIX,
		FLOW_ID_START + ":Integer=" + DEFAULT_FLOW_ID_START,
		USE_ECMP + ":Boolean=" + DEFAULT_USE_ECMP,
		PRODUCTION_ENVIRONMENT + ":Boolean=" + DEFAULT_PRODUCTION_ENVIRONMENT,
		STORAGE_NETWORK_ENABLED + ":Boolean=" + DEFAULT_STORAGE_NETWORK_ENABLED
	}
)

public class ClosFwd extends AbstractListenerManager<ClosDeviceEvent, ClosDeviceListener> implements ClosDeviceService {

	// // // Constants
	// private static final int DEFAULT_TIMEOUT = 0;
	// private static final int DEFAULT_PRIORITY = 10;
	// private static final int BYPASS_DEFAULT_PRIORITY = 40001;
  	// private static final int DEFAULT_INT_SERV_PRIORITY = 15;
	// private static final String CTPD_DEFAULT_INT_FAKEMAC = "22:22:22:00:00:00";
	// private static final String CTPD_DEFAULT_EXT_FAKEMAC = "22:22:22:00:00:01";
	// private static final int DEFAULT_CLI_PREF_LEN = 64;
	// // private static final String L4_DEVICE_ID = "of:0000000000000004";
	// // private static final String L3_DEVICE_ID = "of:0000000000000003";
	// // private static final String L2_DEVICE_ID = "of:0000000000000002";
	// private static final String L1_DEVICE_ID = "of:0000000000000001";
	// private static final String L4_DEVICE_ID = "of:0000000000000004";
	// private static final String OLT_DEVICE_ID = "of:0001";
	// private static final String SPINE_DEVICE_ID = "of:0000000000000f";
	// private static final int DEFAULT_NEIG_SOLIC_INTVAL = 30000;	//30 seconds
	// private static final int DEFAULT_INSTALLATION_TIMEOUT = 10;
	// private static final int DEFAULT_EMPTY_VLANID = 3333;
	// private static final int DEFAULT_BYPASS_VLANID = 2223;
	// private static final int DEFAULT_SERVICE_VLANID = 2222;
	// private static final int DEFAULT_EXT_SERVICE_VLANID = 2224;
	// private static final int DEFAULT_INTERNET_VLANID = 23;
	// private static final boolean DEFAULT_RESPOND_ALL_L2 = false;
  	// // private static final boolean DEFAULT_PATH_RECOVERY = false;
  	// private static final boolean DEFAULT_STREAM_ACTIVATED = false;
	// private static final int DEFAULT_PACKET_PROCESSOR_PRIORITY = 0;
	// private static final String DEFAULT_CTPD_IP = "2001:1498:14::1";
	// private static final String DEFAULT_CTPD_IPV4 = "81.47.232.49";
	// private static final String DEFAULT_CTPD_BGP_IPV4 = "172.30.127.66";
	// private static final String DEFAULT_CTPD_BGP_IPV6 = "2a02:9009:7:ffff::2";
	// private static final int DEFAULT_CLI_SERV_IDLE_TIMEOUT = 60;
	// private static final boolean DEFAULT_KEEP_DATA = true;
	// private static final boolean DEFAULT_LOCAL_CHECK = true;
	// private static final String PROCESSING = "PROCESSING";
	// private static final String PENDING = "PENDING";
	// private static final String ERROR = "ERROR";
	// private static final String DEFAULT_VPDC_CLIENT_PREFIX = "2a02:9009:4::/48";
	// private static final String DEFAULT_VPDC_INTERNAL_PREFIX = "2a02:9009:6::/48";
	// private static final String DEFAULT_SERVICE_IP6_PREFIX = "2a02::/16";
	// private static final String DEFAULT_SERVICE_IP4_PREFIX = "10.95.227.0/24";
	// private static final String DEFAULT_CTPD_MAC_PREFIX = "02:";
	// private static final String FLOW_ID_START = "500";
	// private static final boolean DEFAULT_USE_ECMP = false;
	// private static final boolean DEFAULT_PRO_ENV = false;
	// private static final boolean DEFAULT_STORAGE_NETWORK = false;

	private EndpointManager endpointManager;
	private DeviceListener deviceListener = new InternalDeviceListener();
	private final TopologyListener topologyListener = new InternalTopologyListener();
	private ClosfwdPacketProcessor processor = new ClosfwdPacketProcessor();
	private MastershipInfo mastershipinfo = new MastershipInfo();




	private int flowTimeout;

	private int flowPriority;

	private int bypassFlowPriority;

	private String ctpdFakeInternalMacAddress;

	private String ctpdFakeExternalMacAddress;

	private int vpdcClientPrefixlength;

	private String l1OpenflowSwitchPrefix;

	private String l4OpenflowSwitchPrefix;

	private String spineOpenflowSwitchPrefix ;

  	private String oltOpenflowSwitchPrefix;

	private int neighbourSolicitationInterval;

	private long flowInstallationTimeout;

	private boolean ofdpaActivated;

	private int emptyVlanIdP;

	private int serviceVlanId;

	private int bypassVlanIdP;

	private int extServiceVlanId;

	private boolean monoetiqueta;

	private int defaultVlan;

    private int internetServicesFlowPriority;

    private boolean respondNDPLocally;

	private int closPacketProcessorPriority;

	private boolean streamActivated;

	private String ctpdIp;

	private String ctpdIpv4;

	private String bgpCtpdIpv4;

	private String bgpCtpdIpv6;

	private int client2ServiceIdleTimeout;

	private boolean keepData;

	private boolean checkLocalNDP;

	private String vpdcClientPrefix;

	private String vpdcInternalPrefix;

	private String ctpdMacPrefix;

	private String serviceIp6Prefix;

	private String serviceIp4Prefix;

	private int flowIdStart;

	private boolean useEcmp;

	private boolean productionEnviorement;

	private boolean storageNetworkEnabled;

	private ApplicationId appId;

	@Reference(cardinality = ReferenceCardinality.MANDATORY)
	protected ComponentConfigService cfgService;

	@Reference(cardinality = ReferenceCardinality.MANDATORY)
	protected CoreService coreService;

	// To install flows in the network
	@Reference(cardinality = ReferenceCardinality.MANDATORY)
	public FlowObjectiveService flowObjectiveService;

	// To obtain the topology
	@Reference(cardinality = ReferenceCardinality.MANDATORY)
	public TopologyService topologyService;

	// To control the flows installed by the app
	@Reference(cardinality = ReferenceCardinality.MANDATORY)
	protected FlowRuleService flowRuleService;

	@Reference(cardinality = ReferenceCardinality.MANDATORY)
	protected StorageService storageService;

	@Reference(cardinality = ReferenceCardinality.MANDATORY)
	public PacketService packetService;

	@Reference(cardinality = ReferenceCardinality.MANDATORY)
	protected GroupService groupService;

	@Reference(cardinality = ReferenceCardinality.MANDATORY)
	protected GroupStore groupStore;

	@Reference(cardinality = ReferenceCardinality.MANDATORY)
	protected LinkService linkService;

	@Reference(cardinality = ReferenceCardinality.MANDATORY)
	protected DeviceService deviceService;

	@Reference(cardinality = ReferenceCardinality.MANDATORY)
	protected IntentService intentService;

	@Reference(cardinality = ReferenceCardinality.MANDATORY)
	protected MastershipStore mastershipStore;

	@Reference(cardinality = ReferenceCardinality.MANDATORY)
	protected MastershipService mastershipService;

	@Reference(cardinality = ReferenceCardinality.MANDATORY)
	protected MastershipTermService mastershipTermService;

	@Reference(cardinality = ReferenceCardinality.MANDATORY)
	protected DeviceAdminService deviceAdminService;

	@Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected ClusterService clusterService;

	private final Logger log = LoggerFactory.getLogger("log4j.logger.org.ctpdLogger");
	//private final Logger log = LoggerFactory.getLogger(getClass());

	// /*Pedro*/
	private Driver flowDriver;
	private Boolean flagDeviceEvent = false;


	/*  Los leaf del Vpdc */
	private final boolean leafActivated = true;

	// private LinkService linkServiceBeforeRemoveDevice;

	/* Dos modos de etiquetado */
	//private final boolean monoetiqueta = true;

	/*	Neigbour Solicitation Control Timer */
	private Timer neighSolictControlTimer = null;

	//public static final byte LINK_LOCAL_0 = (byte) 0xfe;
	//public static final byte LINK_LOCAL_1 = (byte) 0x80;

	/* Stores the devices with their UUID as the key */
	ConsistentMap<UUID, Endpoint> registry;
	ConsistentMap<UUID, ApplicationId> appIdRegistry;
	ConsistentMap<UUID, ApplicationId> appIdRegistryPermanent;
	ConsistentMap<UUID, Endpoint> vpdcRegistry;
	ConsistentMap<UUID, Endpoint> storageRegistry;
	ConsistentMap<String, Key> intentsRegistry;
	ConsistentMap<IpPrefix, UUID> ipToUUIDRegistry;
	ConsistentMap<String, Integer> counters;

	ConsistentMap<String, Device> l1Devices;
	ConsistentMap<String, Device> l2l3Devices;
	ConsistentMap<String, Device> l4Devices;
	ConsistentMap<String, Device> spineDevices;
	ConsistentMap<String, Object> pendingTopologyEvents;
	ConsistentMap<String, Object> inprogressTopologyEvents;
	ConsistentMap<String, Integer> srcLeafDstLeafEcmp;
	ConsistentMap<String, Integer> countEcmpsByPath;

	/*	Stores all next Ids for each type of treatment, metadata and device */
	private ConsistentMap<UUID, List<Integer>> nextIdStorage;

	private ConsistentMap<DeviceId, NodeId> lastKnownMaster;

	private ConsistentMap<Boolean, Link> lastLinkRemoved;

	private ConsistentMap<DeviceId, Set<PortNumber>> hostPorts;



	// public DistributedSet<MacAddress> externalServicesMacAddresses;

	public ConsistentMap<IpPrefix, MacAddress> hostMacMap;

	@Activate
	public void activate(ComponentContext context) {
		log.info("[Closfwd] Activating Closfwd App");
		appId = coreService.registerApplication("org.ctpd.closfwd");

		cfgService.registerProperties(getClass());
		modified(context);

		KryoNamespace.Builder serializer = KryoNamespace.newBuilder().register(KryoNamespaces.API)
				.register(java.util.UUID.class).register(org.ctpd.closfwd.Endpoint.class).register(DeviceId.class)
				.register(org.ctpd.closfwd.VpdcEndpoint.class).register(org.ctpd.closfwd.ClientServiceBypassEndpoint.class)
				.register(org.ctpd.closfwd.ServiceEndpoint.class).register(org.ctpd.closfwd.VpdcHostEndpoint.class)
				.register(org.ctpd.closfwd.OltEndpoint.class).register(org.ctpd.closfwd.VoltEndpoint.class)
				.register(org.ctpd.closfwd.OltControlEndpoint.class).register(org.ctpd.closfwd.ExternalServiceEndpoint.class)
				.register(java.lang.String.class).register(org.ctpd.closfwd.StorageEndpoint.class)
				.register(DeviceEvent.Type.class)
				.register(DeviceEvent.class)
				.register(LinkEvent.Type.class)
				.register(LinkEvent.class)
				.register(java.lang.Integer.class);

		registry = storageService.<UUID, Endpoint>consistentMapBuilder()
			.withSerializer(Serializer.using(serializer.build())).withName("registry").withApplicationId(appId)
			// To avoid register deletion
			// .withPurgeOnUninstall()
			.build();

		appIdRegistry = storageService.<UUID, ApplicationId>consistentMapBuilder()
		.withSerializer(Serializer.using(serializer.build())).withName("appIdRegistry").withApplicationId(appId)
		// To avoid register deletion
		// .withPurgeOnUninstall()
		.build();

		appIdRegistryPermanent = storageService.<UUID, ApplicationId>consistentMapBuilder()
		.withSerializer(Serializer.using(serializer.build())).withName("appIdRegistryPermanent").withApplicationId(appId)
		// To avoid register deletion
		// .withPurgeOnUninstall()
		.build();

		intentsRegistry = storageService.<String, Key>consistentMapBuilder()
			.withSerializer(Serializer.using(serializer.build())).withName("intents-registry").withApplicationId(appId)
			// To avoid register deletion
			// .withPurgeOnUninstall()
			.build();

		vpdcRegistry = storageService.<UUID, Endpoint>consistentMapBuilder()
			.withSerializer(Serializer.using(serializer.build())).withName("vpdc-registry").withApplicationId(appId)
			// To avoid register deletion
			// .withPurgeOnUninstall()
			.build();

		storageRegistry = storageService.<UUID, Endpoint>consistentMapBuilder()
			.withSerializer(Serializer.using(serializer.build())).withName("storage-registry").withApplicationId(appId)
			// To avoid register deletion
			// .withPurgeOnUninstall()
			.build();

		ipToUUIDRegistry = storageService.<IpPrefix, UUID>consistentMapBuilder()
			.withSerializer(Serializer.using(serializer.build())).withName("up-to-uuid").withApplicationId(appId)
			// To avoid register deletion
			// .withPurgeOnUninstall()
			.build();

		l1Devices = storageService.<String, Device>consistentMapBuilder()
			.withSerializer(Serializer.using(serializer.build())).withName("l1-devices").withApplicationId(appId)
			// To avoid register deletion
			// .withPurgeOnUninstall()
			.build();

		l4Devices = storageService.<String, Device>consistentMapBuilder()
			.withSerializer(Serializer.using(serializer.build())).withName("l4-devices").withApplicationId(appId)
			// To avoid register deletion
			// .withPurgeOnUninstall()
			.build();

		spineDevices = storageService.<String, Device>consistentMapBuilder()
			.withSerializer(Serializer.using(serializer.build())).withName("spine-devices").withApplicationId(appId)
			// To avoid register deletion
			// .withPurgeOnUninstall()
			.build();

		l2l3Devices = storageService.<String, Device>consistentMapBuilder()
			.withSerializer(Serializer.using(serializer.build())).withName("l2-l3-devices").withApplicationId(appId)
			// To avoid register deletion
			// .withPurgeOnUninstall()
			.build();

		// // jmcp storage mas adelante
		nextIdStorage = storageService.<UUID, List<Integer>>consistentMapBuilder()
				.withSerializer(Serializer.using(serializer.build())).withName("nextIdStorage").withApplicationId(appId)
				// To avoid register deletion
				// .withPurgeOnUninstall()
				.build();

		lastKnownMaster = storageService.<DeviceId, NodeId>consistentMapBuilder()
				.withSerializer(Serializer.using(serializer.build())).withName("lastKnownMaster").withApplicationId(appId)
				// To avoid register deletion
				// .withPurgeOnUninstall()
				.build();

		hostMacMap = storageService.<IpPrefix, MacAddress>consistentMapBuilder()
				.withSerializer(Serializer.using(serializer.build())).withName("host-mac-map").withApplicationId(appId)
				// To avoid register deletion
				// .withPurgeOnUninstall()
				.build();

		installControllerFlowsL2L3L4Devices();

		pendingTopologyEvents = storageService.<String, Object>consistentMapBuilder()
			.withSerializer(Serializer.using(serializer.build())).withName("pending-topology-events").withApplicationId(appId)
			// To avoid register deletion
			// .withPurgeOnUninstall()
			.build();

		inprogressTopologyEvents = storageService.<String, Object>consistentMapBuilder()
			.withSerializer(Serializer.using(serializer.build())).withName("inprogress-topology-events").withApplicationId(appId)
			// To avoid register deletion
			// .withPurgeOnUninstall()
			.build();

		counters = storageService.<String, Integer>consistentMapBuilder()
			.withSerializer(Serializer.using(serializer.build())).withName("counters").withApplicationId(appId)
			// To avoid register deletion
			// .withPurgeOnUninstall()
			.build();

		srcLeafDstLeafEcmp = storageService.<String, Integer>consistentMapBuilder()
			.withSerializer(Serializer.using(serializer.build())).withName("srcLeafDstLeafEcmp").withApplicationId(appId)
			// To avoid register deletion
			// .withPurgeOnUninstall()
			.build();

		lastLinkRemoved = storageService.<Boolean, Link>consistentMapBuilder()
			.withSerializer(Serializer.using(serializer.build())).withName("lastLinkRemoved").withApplicationId(appId)
			// To avoid register deletion
			// .withPurgeOnUninstall()
			.build();

		countEcmpsByPath = storageService.<String, Integer>consistentMapBuilder()
			.withSerializer(Serializer.using(serializer.build())).withName("countEcmpsByPath").withApplicationId(appId)
			// To avoid register deletion
			// .withPurgeOnUninstall()
			.build();

		hostPorts = storageService.<DeviceId, Set<PortNumber>>consistentMapBuilder()
			.withSerializer(Serializer.using(serializer.build())).withName("hostPorts").withApplicationId(appId)
			// To avoid register deletion
			// .withPurgeOnUninstall()
			.build();

		readComponentConfiguration(context);

		updateSwitches();

		endpointManager = new EndpointManager();

		if (ofdpaActivated)
			flowDriver = new FlowDriverOfdpa();
		else
			flowDriver = new FlowDriverOvs();

		topologyService.addListener(topologyListener);
		deviceService.addListener(deviceListener);

		packetService.addProcessor(processor, PacketProcessor.director(closPacketProcessorPriority));

		// log.debug("[activate] OnLife: CTpd ClosFwd started", appId.id());
		log.info("[Closfwd]  Closfwd App Activated");
		log.info("bypassFlowPriority "+bypassFlowPriority);

	}

	@Deactivate
	public void deactivate() {
		cfgService.unregisterProperties(getClass(), false);

		topologyService.removeListener(topologyListener);
		deviceService.removeListener(deviceListener);

		withdrawPackets();
		packetService.removeProcessor(processor);
		processor = null;

		if(!keepData)
		{
			purgeClos();
			registry.destroy();
			l1Devices.destroy();
			l4Devices.destroy();
			spineDevices.destroy();
			l2l3Devices.destroy();
			appIdRegistry.destroy();
			appIdRegistryPermanent.destroy();
			intentsRegistry.destroy();
			vpdcRegistry.destroy();
			storageRegistry.destroy();
			nextIdStorage.destroy();
			lastKnownMaster.destroy();
			ipToUUIDRegistry.destroy();
			inprogressTopologyEvents.destroy();
			pendingTopologyEvents.destroy();
			counters.destroy();
			srcLeafDstLeafEcmp.destroy();
			countEcmpsByPath.destroy();

		}


		// log.debug("[deactivate] OnLife: CTpd ClosFwd stopped");
	}

	@Modified
	public void modified(ComponentContext context) {
		Dictionary<?, ?> properties = context != null ? context.getProperties() : new Properties();
		readComponentConfiguration(context);
	}

	private void updateSwitches() {
		Iterator<Device> devices = deviceService.getDevices().iterator();
		try{
			while(devices.hasNext()) {
				Device device =  devices.next();
				if (!device.id().toString().startsWith(l1OpenflowSwitchPrefix) &&
					!device.id().toString().startsWith(spineOpenflowSwitchPrefix) &&
					!device.id().toString().startsWith(oltOpenflowSwitchPrefix)){
					if(device.id().toString().startsWith(l4OpenflowSwitchPrefix)){
						l4Devices.put(device.id().toString(), device);
						log.debug("update-switches-L4");
					}else{
						l2l3Devices.put(device.id().toString(), device);
						log.debug("update-switches-L2L3");
					}
				}
				else if(device.id().toString().startsWith(spineOpenflowSwitchPrefix)){
					spineDevices.put(device.id().toString(), device);
					log.debug("update-switches-Spines");
				}
				else if(device.id().toString().startsWith(l1OpenflowSwitchPrefix))
					l1Devices.put(device.id().toString(), device);
					log.debug("update-switches-L1");

			}
		} catch (Exception e) {
			log.error("update-switches-exception", e);
		}
	}

	private MacAddress getHostMac(Endpoint endpoint) {
		return getHostMac(endpoint.getNode(), endpoint.getPort());
	}

	@Override
	public MacAddress getHostMac(DeviceId deviceId, PortNumber portNumber) {

		String strId1 = deviceId.toString().substring(17);
		String strId2 = deviceId.toString().substring(15, 17);
		String strId3 = deviceId.toString().substring(13, 15);

		if(strId1 == null || strId2 == null || strId3 == null || portNumber == null) return null;

		byte[] hostMac = new byte[]{
				(byte) 0x44,
				(byte) 0x44,
				(byte) Short.parseShort(strId3),
				(byte) Short.parseShort(strId2),
				(byte) Short.parseShort(strId1),
				(byte) portNumber.toLong()
		};

		return MacAddress.valueOf(hostMac);
	}

	@Override
	public MacAddress getLeafDstMac(DeviceId deviceId) {

		String strId1 = deviceId.toString().substring(17);
		String strId2 = deviceId.toString().substring(15, 17);
		String strId3 = deviceId.toString().substring(13, 15);

		if(strId1 == null || strId2 == null || strId3 == null) return null;

		byte[] hostMac = new byte[]{
				(byte) 0x44,
				(byte) 0x44,
				(byte) 0x44,
				(byte) Short.parseShort(strId3),
				(byte) Short.parseShort(strId2),
				(byte) Short.parseShort(strId1)
		};

		return MacAddress.valueOf(hostMac);
	}

	@Override
	public boolean checkIfEcmpExists(DeviceId srcLeaf, DeviceId dstLeaf){

		String srcLeafString = srcLeaf.toString();
		String dstLeafString = dstLeaf.toString();
		String keySrcDstLeaf = srcLeafString+"/" + dstLeafString;
		return srcLeafDstLeafEcmp.containsKey(keySrcDstLeaf);
	}

	@Override
	public int getNextIdForEcmp(DeviceId srcLeaf, DeviceId dstLeaf, Boolean create){

		String srcLeafString = srcLeaf.toString();
		String dstLeafString = dstLeaf.toString();
		String keySrcDstLeaf = srcLeafString+"/" + dstLeafString;
		UUID srcDstLeafUuid = UUID.nameUUIDFromBytes(keySrcDstLeaf.getBytes());

		if(srcLeafDstLeafEcmp.containsKey(keySrcDstLeaf) && create){
			int count = countEcmpsByPath.get(keySrcDstLeaf).value();
			countEcmpsByPath.put(keySrcDstLeaf, count+1);
			int nextIdEcmp = srcLeafDstLeafEcmp.get(keySrcDstLeaf).value();
			// log.debug("Ecmp exists with number: " + nextIdEcmp);
			return nextIdEcmp;
		}
		else if(!srcLeafDstLeafEcmp.containsKey(keySrcDstLeaf) && create){
			int nextIdEcmp = getNextId(srcDstLeafUuid, true);
			countEcmpsByPath.put(keySrcDstLeaf, 1);
			srcLeafDstLeafEcmp.put(keySrcDstLeaf, nextIdEcmp);
			// log.debug("Ecmp not exists with number: " + nextIdEcmp);
			return nextIdEcmp;
		}else{
			int count = countEcmpsByPath.get(keySrcDstLeaf).value();
			countEcmpsByPath.put(keySrcDstLeaf, count-1);
			int nextIdEcmp = srcLeafDstLeafEcmp.get(keySrcDstLeaf).value();
			if(count-1 == 0){
				srcLeafDstLeafEcmp.remove(keySrcDstLeaf);
			}
			return nextIdEcmp;
		}
	}

	@Override
	public ApplicationId getEcmpApplicationId(){

		String ecmpApPlicationIdString = "EcmpApplicationId";
		int idEcmpGroups = flowIdStart;
		UUID ecmpApplicationIdUuid = UUID.nameUUIDFromBytes(ecmpApPlicationIdString.getBytes());

		if (!appIdRegistry.containsKey(ecmpApplicationIdUuid)){
			ApplicationId ecmpApplicationId = new DefaultApplicationId(idEcmpGroups, ecmpApPlicationIdString);
			appIdRegistry.put(ecmpApplicationIdUuid, ecmpApplicationId);
			return ecmpApplicationId;

		}else{
			ApplicationId ecmpApplicationId = appIdRegistry.get(ecmpApplicationIdUuid).value();
			return ecmpApplicationId;
		}
	}

	@Override
	public ApplicationId getBypassApplicationId(){

		String bypassApPlicationIdString = "BypassApplicationId";
		int idBypassGroups = flowIdStart+1;
		UUID bypassApplicationIdUuid = UUID.nameUUIDFromBytes(bypassApPlicationIdString.getBytes());

		if (!appIdRegistry.containsKey(bypassApplicationIdUuid)){
			ApplicationId bypassApplicationId = new DefaultApplicationId(idBypassGroups, bypassApPlicationIdString);
			appIdRegistry.put(bypassApplicationIdUuid, bypassApplicationId);
			return bypassApplicationId;

		}else{
			ApplicationId bypassApplicationId = appIdRegistry.get(bypassApplicationIdUuid).value();
			return bypassApplicationId;
		}
	}

	private void installControllerFlowsL2L3L4Device(DeviceId deviceId) {

		TrafficSelector.Builder selector = DefaultTrafficSelector.builder()
													.matchEthType(Ethernet.TYPE_ARP)
													.matchArpOp(ARP.OP_REQUEST);

		packetService.requestPackets(selector.build(), PacketPriority.REACTIVE, appId, Optional.of(deviceId));


        selector = DefaultTrafficSelector.builder()
                .matchEthType(Ethernet.TYPE_IPV6)
				.matchIPProtocol(IPv6.PROTOCOL_ICMP6)
				.matchEthDstMasked(MacAddress.valueOf("33:33:00:00:00:00"),MacAddress.valueOf("ff:ff:00:00:00:00"));
        	//.matchIcmpv6Type(ICMP6.NEIGHBOR_SOLICITATION);// Not working

		packetService.requestPackets(selector.build(), PacketPriority.REACTIVE, appId, Optional.of(deviceId));

		selector = DefaultTrafficSelector.builder()
                .matchEthType(Ethernet.TYPE_IPV6)
                .matchIPProtocol(IPv6.PROTOCOL_ICMP6)
                .matchIPv6Dst(IpPrefix.valueOf(IpAddress.valueOf(ctpdIp),128));
				//.matchIcmpv6Type(ICMP6.NEIGHBOR_SOLICITATION);// Not working

		packetService.requestPackets(selector.build(), PacketPriority.REACTIVE, appId, Optional.of(deviceId));

		selector = DefaultTrafficSelector.builder()
                .matchEthType(Ethernet.TYPE_IPV4)
                .matchIPProtocol(IPv4.PROTOCOL_ICMP)
                .matchIPDst(IpPrefix.valueOf(IpAddress.valueOf(ctpdIpv4),32));
				//.matchIcmpv6Type(ICMP6.NEIGHBOR_SOLICITATION);// Not working

		packetService.requestPackets(selector.build(), PacketPriority.REACTIVE, appId, Optional.of(deviceId));

	}

	private void withdrawPacketsFromDevice(DeviceId deviceId) {
		TrafficSelector.Builder selector = DefaultTrafficSelector.builder()
													.matchEthType(Ethernet.TYPE_ARP)
													.matchArpOp(ARP.OP_REQUEST);

		packetService.cancelPackets(selector.build(), PacketPriority.REACTIVE, appId, Optional.of(deviceId));



        selector = DefaultTrafficSelector.builder()
                .matchEthType(Ethernet.TYPE_IPV6)
				.matchIPProtocol(IPv6.PROTOCOL_ICMP6)
				.matchEthDstMasked(MacAddress.valueOf("33:33:00:00:00:00"),MacAddress.valueOf("ff:ff:00:00:00:00"));

        //.matchIcmpv6Type(ICMP6.NEIGHBOR_SOLICITATION); // Not working


		// packetService.cancelPackets(selector.build(), PacketPriority.REACTIVE, appId, Optional.of(deviceId));

        // selector = DefaultTrafficSelector.builder()
        //         .matchEthType(Ethernet.TYPE_IPV6)
        //         .matchIPProtocol(IPv6.PROTOCOL_ICMP6)
        //         .matchIPv6Dst(IpPrefix.valueOf(IpAddress.valueOf(ctpdIpInt),128));

        // packetService.cancelPackets(selector.build(), PacketPriority.REACTIVE, appId, Optional.of(deviceId));

	}

	private void withdrawPackets() {

		// Remove ARP/NDP from all switches, not only active
		Iterator<Device> iter = deviceService.getDevices().iterator();
		if (!iter.hasNext())
			log.debug("[event] No devices provided by Device Service");
		else
			log.debug("[event] At least one device provided by Device Service");

		while (iter.hasNext()) {
			Device d = iter.next();
			if (isL2orL3orL4Device(d.id()))
			{
				withdrawPacketsFromDevice(d.id());
				log.debug("[event] cotroller flows uninstalled in {} ", d.id().toString());
			}
		}
	}

	@Override
	public ApplicationId setApplicationFlowId(UUID uuidEndpoint){

		if (appIdRegistry.isEmpty()){
			int flowIdStartInt = flowIdStart+2;
			log.debug("FlowsId " + flowIdStart);
			ApplicationId applicationId = new DefaultApplicationId(flowIdStartInt, uuidEndpoint.toString());
			counters.put("flowsIds", flowIdStartInt);
			return applicationId;
		}else{
			int flowIdCounter = counters.get("flowsIds").value();
			flowIdCounter++;
			log.debug("FlowsId not first " + flowIdCounter);
			ApplicationId applicationId = new DefaultApplicationId(flowIdCounter, uuidEndpoint.toString());
			counters.put("flowsIds", flowIdCounter);
			return applicationId;
		}
	}

	@Override
	public ApplicationId getApplicationFlowId(Endpoint endpoint){
		log.debug("appIdRegistry "+appIdRegistry);
		log.debug("endpoint.id "+endpoint.id);
		return appIdRegistry.get(endpoint.id).value();
	}


	@Override
	public UUID addEndpoint(Endpoint endpoint) {

		Endpoint endp;
		log.debug("add-endpoint-start");

		log.debug(endpoint.toString());

		// Check if it is an existing endpoint...
		if(checkExistingEndpoint(endpoint))
			return endpoint.getUUID();

		if(endpoint instanceof ClientServiceBypassEndpoint){
			ClientServiceBypassEndpoint bypassServiceEnd=(ClientServiceBypassEndpoint) endpoint;
			ArrayList<UUID> myList=bypassServiceEnd.getServiceUUIDs();

			for ( UUID uid : myList){
				try{
					endp=getEndpoint(uid);
					if((endp==null) || (endp instanceof ServiceEndpoint==false && endp instanceof ExternalServiceEndpoint==false && endp instanceof StorageEndpoint==false)){
						throw new CustomiceException("Excepcion");
					}
				}
				catch(CustomiceException e){
					throw new CustomiceException("There is no ServiceEndpoit or ExternalServiceEndpoint with this id");
				}
			}
		}

		if(endpoint instanceof OltControlEndpoint){

			OltControlEndpoint olt=(OltControlEndpoint) endpoint;
			try{
				endp=getEndpoint(olt.getVoltUUID());
				//VlanId endpVlan=endp.getVlan();
				//VlanId oltVlan=olt.getVlan();
				if(endp==null || (olt.getVlan().toString().equals(endp.getVlan().toString())==false)){
					throw new CustomiceException("No se puede crear");
				}
			}
			catch(CustomiceException e){
				throw new CustomiceException("There is no Voltendpoint with this id or it is not the same vlan ");
			}
			VoltEndpoint volt=(VoltEndpoint) endp;
			volt.addReference();
			registry.put(endpoint.getUUID(),endpoint);
			appIdRegistry.put(endpoint.getUUID(), setApplicationFlowId(endpoint.getUUID()));
			appIdRegistryPermanent.put(endpoint.getUUID(), getApplicationFlowId(endpoint));

		}
		else if(endpoint instanceof VpdcHostEndpoint){
			VpdcHostEndpoint vpdcHost = (VpdcHostEndpoint) endpoint;
			try{
				endp=getEndpoint(vpdcHost.getOltUUID());
				if(endp==null || (vpdcHost.getVlan().toString().equals(endp.getVlan().toString())==false)){
					throw new CustomiceException("No se puede crear");
				}
			}
			catch(Exception e){
				throw new CustomiceException("There is no Oltendpoint with this id or it is not the same vlan");
			}
			OltEndpoint oltEnd=(OltEndpoint) endp;
			oltEnd.addReference();
			registry.put(endpoint.getUUID(), endpoint);
			appIdRegistry.put(endpoint.getUUID(), setApplicationFlowId(endpoint.getUUID()));
			appIdRegistryPermanent.put(endpoint.getUUID(), getApplicationFlowId(endpoint));
		}
		else if(endpoint instanceof VpdcEndpoint ||
			endpoint instanceof ClientServiceBypassEndpoint)
		{
			vpdcRegistry.put(endpoint.getUUID(), endpoint);
			appIdRegistry.put(endpoint.getUUID(), setApplicationFlowId(endpoint.getUUID()));
			appIdRegistryPermanent.put(endpoint.getUUID(), getApplicationFlowId(endpoint));
		}
		else if(endpoint instanceof StorageEndpoint)
		{
			storageRegistry.put(endpoint.getUUID(), endpoint);
			appIdRegistry.put(endpoint.getUUID(), setApplicationFlowId(endpoint.getUUID()));
			appIdRegistryPermanent.put(endpoint.getUUID(), getApplicationFlowId(endpoint));
		}
		else{
			registry.put(endpoint.getUUID(), endpoint);
			appIdRegistry.put(endpoint.getUUID(), setApplicationFlowId(endpoint.getUUID()));
			appIdRegistryPermanent.put(endpoint.getUUID(), getApplicationFlowId(endpoint));
		}

		if (endpoint instanceof ServiceEndpoint || endpoint instanceof StorageEndpoint) {
			MacAddress hostMac = getHostMac(endpoint);
			hostMacMap.put(endpoint.getIpPrefix(), hostMac);
		}

		// For those services which port is not enabled on the device, we do not install flows...
		if(endpoint instanceof ServiceEndpoint ||
				endpoint instanceof ExternalServiceEndpoint
				|| endpoint instanceof StorageEndpoint) {
			if(checkEndpointPortStatus(endpoint.getNode(), endpoint.getPort()))
				log.debug("service-endpoint-port-enabled");
			else {
				log.debug("service-endpoint-port-disabled");
				return endpoint.getUUID();
			}
		}

		if(endpoint instanceof ClientServiceBypassEndpoint) {
			// Map IP to UUID for quick searching
			ipToUUIDRegistry.put(endpoint.getIpPrefix(), endpoint.getUUID());
		}

		endpointManager.installEndpointFlows(endpoint, true);
		log.debug("add-endpoint-end");
		return endpoint.getUUID();
	}

	private boolean checkEndpointPortStatus(DeviceId deviceId, PortNumber portNumber) {
		ConnectPoint cp = new ConnectPoint(deviceId, portNumber);
		Port port = deviceService.getPort(cp);
		if (port == null){
			return false;
		}else{
			return port.isEnabled();
		}
	}

	@Override
	public boolean checkExistingEndpoint(Endpoint endpoint) {
		if (getEndpoint(endpoint.getUUID())!=null)
			return true;
		else
			return false;
	}

	/* Get device from registry associated with given UUID */
	@Override
	public Endpoint getEndpoint(UUID id) {
		Versioned<Endpoint> endpoint = registry.get(id);
		if (endpoint != null)
			return endpoint.value();
		else
		{
			endpoint = vpdcRegistry.get(id);
			if (endpoint != null)
				return endpoint.value();
			else
			{
				endpoint = storageRegistry.get(id);
				if (endpoint != null)
					return endpoint.value();
			}
		}
			return null;
	}

	/* Remove device from registry associated with given UUID */
	@Override
	public Endpoint removeEndpoint(UUID id) {

		Versioned<Endpoint> endpoint = registry.get(id);
		Versioned<Endpoint> endpointVpdc = vpdcRegistry.get(id);
		Endpoint endpp= getEndpoint(id);
		Endpoint end;

		if (endpoint != null) {
			if (endpp instanceof VpdcHostEndpoint){
				VpdcHostEndpoint vpdcHo=(VpdcHostEndpoint) endpp;
				end=getEndpoint(vpdcHo.getOltUUID());
				OltEndpoint oltEn=(OltEndpoint) end;
				oltEn.drcReference();
				registry.put(oltEn.getUUID(),oltEn);
			}

			if(endpp instanceof OltControlEndpoint){
				OltControlEndpoint oltEndpo= (OltControlEndpoint) endpp;
				end=getEndpoint(oltEndpo.getVoltUUID());
				VoltEndpoint voltEndp=(VoltEndpoint) end;
				voltEndp.drcReference();
				registry.put(voltEndp.getUUID(),voltEndp);
			}

			if(endpp instanceof VoltEndpoint){
				end=getEndpoint(id);
				VoltEndpoint voltEnd=(VoltEndpoint) end;
				try{
					if(voltEnd.getReference()>0){

						  throw new CustomiceException("Excepcion");
					}
				}
				catch(CustomiceException e ){
					throw new CustomiceException("Can not delete a referenced Endpoint");
				}



			}
			if(endpp instanceof OltEndpoint){
				Endpoint endp=getEndpoint(id);
				OltEndpoint oltEndp=(OltEndpoint) endp;
				try{
					if(oltEndp.getReference()>0){

						throw new CustomiceException("Excepcion");
					}
				}
				catch(CustomiceException e ){
					throw new CustomiceException("Can not delete a referenced Endpoint");

				}

			}

			if(endpoint.value() instanceof ServiceEndpoint ||
				endpoint.value() instanceof ExternalServiceEndpoint ||
				endpoint.value() instanceof StorageEndpoint) {

				if(checkEndpointPortStatus(endpoint.value().getNode(), endpoint.value().getPort()))
				{
					log.debug("service-endpoint-port-enabled");
					endpointManager.installEndpointFlows(endpoint.value(), false);
				}
				else {
					log.debug("service-endpoint-port-disabled");
				}
			}
			else {
				endpointManager.installEndpointFlows(endpoint.value(), false);
			}

			if(endpoint.value() instanceof ServiceEndpoint ||
				endpoint.value() instanceof StorageEndpoint)
				hostMacMap.remove(endpoint.value().getIpPrefix());

			if(endpoint.value() instanceof StorageEndpoint)
				storageRegistry.remove(id);

			if(endpoint.value() instanceof VpdcEndpoint ||
				endpoint.value() instanceof ClientServiceBypassEndpoint){
					vpdcRegistry.remove(id);
			} else {
				registry.remove(id);
			}

			appIdRegistry.remove(id);

			return endpoint.value();
		}
		if (endpointVpdc != null) {
			endpointManager.installEndpointFlows(endpointVpdc.value(), false);
			if(endpointVpdc.value() instanceof VpdcEndpoint){
				vpdcRegistry.remove(id);
			}
			if(endpointVpdc.value() instanceof ClientServiceBypassEndpoint){
				vpdcRegistry.remove(id);
				ipToUUIDRegistry.remove(endpointVpdc.value().getIpPrefix());
			}

			appIdRegistry.remove(id);

			return endpointVpdc.value();
		}
		return null;
	}

	public void installControllerFlowsL2L3L4Devices() {

		Iterable<Device> devices = deviceService.getAvailableDevices();

		for(Device d : devices) {
			if (isL2orL3orL4Device(d.id())) {
				installControllerFlowsL2L3L4Device(d.id());
				log.debug("[event] cotroller flows installed in {} ", d.id().toString());
			}
		}
	}

	@Override
	public String linkIdFromDevices(DeviceId src, DeviceId dst)
	{
		int srcId = src.hashCode();
		int dstId = dst.hashCode();

		if (srcId>dstId) {
			String r = String.valueOf(src.toString())+"-" + String.valueOf(dst.toString());
			log.debug("[linkIdFromDevices]: linkId returned:{}", r);
			return r;
		}
		else{
			String r = String.valueOf(dst.toString())+"-" + String.valueOf(src.toString());
			log.debug("[linkIdFromDevices]: linkId returned:{}", r);
			return r;
		}
	}

	@Override
	public Set<UUID> getRegisterUUIDs() {
		return registry.keySet();
	}

	@Override
	public Set<UUID> getVpdcRegisterUUIDs() {
		return vpdcRegistry.keySet();
	}

	@Override
	public ConsistentMap<UUID,Endpoint> getRegisterEndpoints() {
		return registry;
	}

	@Override
	public ConsistentMap<String,Key> getRegisterIntents() {
		return intentsRegistry;
	}

// 	// public Set<String> getRegisterPathIds(){
// 	// 	return pathData.keySet();
// 	// }
	@Override
	public int getSize() {
		return registry.size();
	}

	@Override
	public String purgeIntents() {
		int intentsInstalled = 0;
		int intentsWithdrawn = 0;
		int intentsWithdrawing = 0;
		for (Intent intent: intentService.getIntents()){
			intentService.purge(intent);
			if(intentService.getIntentState(intent.key()) == IntentState.INSTALLED){
				intentsInstalled++;
			}else if(intentService.getIntentState(intent.key()) == IntentState.WITHDRAWN){
				intentsWithdrawn++;
			}else if(intentService.getIntentState(intent.key()) == IntentState.WITHDRAWING){
				intentsWithdrawing++;
			}
		}
		intentsRegistry.clear();
		return "Total intents Installed: " + intentsInstalled+" , Withdrawn: " + intentsWithdrawn+" , Withdrawing :" + intentsWithdrawing;
	}

	@Override
	public String withdrawIntents() {
		int intentsInstalled = 0;
		int intentsWithdrawn = 0;
		int intentsWithdrawing = 0;
		for (Intent intent: intentService.getIntents()){
			intentService.withdraw(intent);
			if(intentService.getIntentState(intent.key()) == IntentState.INSTALLED){
				intentsInstalled++;
			}else if(intentService.getIntentState(intent.key()) == IntentState.WITHDRAWN){
				intentsWithdrawn++;
			}else if(intentService.getIntentState(intent.key()) == IntentState.WITHDRAWING){
				intentsWithdrawing++;
			}
		}
		intentsRegistry.clear();
		return "Total intents Installed: " + intentsInstalled+" , Withdrawn: " + intentsWithdrawn+" , Withdrawing :" + intentsWithdrawing;
	}

	public void removeIntents(Endpoint endpoint){

		Set<String> keySetIntents = getRegisterIntents().keySet();
		for(String idIter : keySetIntents){
			if((idIter.toString().split("/")[0]).equals(endpoint.getUUID().toString())){
				Key key = getRegisterIntents().get(idIter).value();
				getRegisterIntents().remove(idIter);
				Intent intentToDelete = getIntentService().getIntent(key);
				getIntentService().withdraw(intentToDelete);

			}
		}
	}


	@Override
	public int purgeFlowIdPermanentClos() {
		purgeClos();
		for(UUID uuidEndpoint: appIdRegistryPermanent.keySet()){
			log.debug("Force to delete endpoint with appId: " + appIdRegistryPermanent.get(uuidEndpoint).value());
			flowRuleService.removeFlowRulesById(appIdRegistryPermanent.get(uuidEndpoint).value());
		}
		return appIdRegistryPermanent.size();

	}

	@Override
	public int resetPermanentFlowIdRegister(){

		appIdRegistryPermanent.clear();
		appIdRegistryPermanent.destroy();

		return appIdRegistryPermanent.size();
	}

	// @Override
	// public void purgeClosByUuidEndpoint(Endpoint endpoint, Boolean force) {
	// 	if(!(endpoint instanceof OltEndpoint) ||
	// 		!(endpoint instanceof VoltEndpoint) ||
	// 		!(endpoint instanceof OltControlEndpoint)){
	// 		if(force){
	// 			flowRuleService.removeFlowRulesById(appIdRegistryPermanent.get(endpoint.getUUID()).value());
	// 			appIdRegistry.remove(endpoint.getUUID());
	// 		}else{
	// 			endpointManager.installEndpointFlows(endpoint, false);
	// 			// flowRuleService.removeFlowRulesById(appIdRegistry.get(endpoint.getUUID()).value());
	// 			// appIdRegistry.remove(endpoint.getUUID());
	// 		}
	// 	}
	// 	if(endpoint instanceof VpdcHostEndpoint ||
	// 		endpoint instanceof OltControlEndpoint){
	// 		removeIntents(endpoint);
	// 	}
	// 	if(endpoint instanceof VpdcEndpoint ||
	// 		endpoint instanceof ClientServiceBypassEndpoint){
	// 			vpdcRegistry.remove(endpoint.getUUID());
	// 	}else{
	// 		registry.remove(endpoint.getUUID());
	// 	}
	// }


	@Override
	public int purgeClos() {

		for(UUID uuidEndpoint: appIdRegistry.keySet()){
			log.debug("Deleting endpoint with appId: " + appIdRegistry.get(uuidEndpoint).value());
			flowRuleService.removeFlowRulesById(appIdRegistry.get(uuidEndpoint).value());
		}

		flowRuleService.removeFlowRulesById(appId);
		// groupService.purgeGroupEntries();

		registry.clear();
		vpdcRegistry.clear();
		appIdRegistry.clear();
		nextIdStorage.clear();
		lastKnownMaster.clear();
		hostMacMap.clear();
		inprogressTopologyEvents.clear();
		pendingTopologyEvents.clear();
		counters.clear();
		srcLeafDstLeafEcmp.clear();
		countEcmpsByPath.clear();
		lastLinkRemoved.clear();
		hostPorts.clear();

		int size = registry.size()+vpdcRegistry.size();

		return size;
	}

	public String recreateEndpoint(Endpoint endpoint){

		if((endpoint instanceof VoltEndpoint) ||
			(endpoint instanceof OltEndpoint)){

			return("Not recreating VoltEndpoint or OltEndpoint because has no flows");

		}else{

			// We cannot remove flows of a EP by its AppId because flows with ECMP groups have EcmpApplicationId
			// flowRuleService.removeFlowRulesById(appIdRegistry.get(endpoint.getUUID()).value());
			endpointManager.installEndpointFlows(endpoint, false);
			endpointManager.installEndpointFlows(endpoint, true);
			return("Recreated endpoint UUID " + endpoint.id);
		}
	}

	// @Override
	public VlanId getVlanExternal() {

		//There is a constructor in class VlanId with input param of type String
		VlanId vlan = VlanId.vlanId((short)extServiceVlanId);
		return vlan;
	}

	@Override
	public int getNextId(UUID endpointUUID, Boolean create) {
		if (create) {
			int nextId = flowObjectiveService.allocateNextId();
			Versioned<List<Integer>> nextIdListV = nextIdStorage.get(endpointUUID);
			if(nextIdListV == null){
				List<Integer> saveNextIdsList = new ArrayList<Integer>();
				saveNextIdsList.add(nextId);
				nextIdStorage.put(endpointUUID, saveNextIdsList);
				return nextId;
			}else{
				List<Integer> nextIdList = nextIdListV.value();
				List<Integer> saveNextIdsList = new ArrayList<Integer>(nextIdList);
				saveNextIdsList.add(nextId);
				nextIdStorage.put(endpointUUID, saveNextIdsList);
				return nextId;
			}

		} else {
			List<Integer> nextIdList = nextIdStorage.get(endpointUUID).value();
			if(nextIdList.size()==0)
				return -1;
			/*	Get first element from List */
			int nextId = nextIdList.get(0);

			/*	remove and save to NextId Storage */
			nextIdList.remove(0);
			nextIdStorage.put(endpointUUID, nextIdList);
			return nextId;

		}
	}

	private Boolean isL2orL3orL4Device(DeviceId deviceId) {
		String device = deviceId.toString();
		if (device.startsWith(l1OpenflowSwitchPrefix)) {
			return false;
		} else if (device.startsWith(spineOpenflowSwitchPrefix)) {
			return false;
		} else if (device.startsWith(oltOpenflowSwitchPrefix)) {
			return false;
		} else {
			return true;
		}
	}

	private Boolean isSpineDevice(DeviceId deviceId) {
		String device = deviceId.toString();
		if (device.startsWith(spineOpenflowSwitchPrefix)) {
			return true;
		}
		return false;
	}

	private Boolean isL2orL3Device(DeviceId deviceId) {
		String device = deviceId.toString();
		if (device.startsWith(l1OpenflowSwitchPrefix)) {
			return false;
		} else if (device.startsWith(l4OpenflowSwitchPrefix)) {
			return false;
		} else if (device.startsWith(spineOpenflowSwitchPrefix)) {
			return false;
		} else if (device.startsWith(oltOpenflowSwitchPrefix)) {
			return false;
		} else {
			return true;
		}
	}


// 	// Aux Method
	@Override
	public String getCtpdFakeInternalMacAddress(){
		return ctpdFakeInternalMacAddress;
	}

// 	// Aux Method
	@Override
	public String getCtpdFakeExternalMacAddress(){
		return ctpdFakeExternalMacAddress;
	}

// 	// Aux Method
// 	// @Override
	public int getEmptyVlanId(){
		return emptyVlanIdP;
	}

	@Override
	public int getServiceVlanId(){
		return serviceVlanId;
	}

	@Override
	public int getExtServiceVlanId(){
		return extServiceVlanId;
	}

	public int getBypassVlanId(){
		return bypassVlanIdP;
	}


// 	// Aux Method
	@Override
	public int getVpdcClientPrefixlength(){ return vpdcClientPrefixlength;}

	// Aux Method
	@Override
	public ConsistentMap<UUID, Endpoint> getRegistry(){ return registry; }

	// Aux Method
	@Override
	public boolean getrespondNDPLocally(){ return respondNDPLocally; }

	// Aux Method
	@Override
	public boolean getcheckLocalNDP(){ return checkLocalNDP; }

	@Override
	public boolean getUseEcmp(){ return useEcmp; }

	@Override
	public boolean getProductionEnviorement(){ return productionEnviorement; }

	// Aux Method
	@Override
	public String getCtpdIpv4(){ return ctpdIpv4; }

	@Override
	public String getBgpCtpdIpv4(){ return bgpCtpdIpv4; }

	@Override
	public String getBgpCtpdIpv6(){ return bgpCtpdIpv6; }

	@Override
	public String getCtpdIpv6(){ return ctpdIp; }

// 	// Aux Method
	@Override
	public ApplicationId getAppId(){ return appId; }

// 	// Aux Method
	@Override
	public int getFlowPriority(){ return flowPriority; }

	@Override
	public int getBypassFlowPriority(){ return bypassFlowPriority; }

	// Aux Method
	@Override
	public int getClient2ServiceIdleTimeout(){ return client2ServiceIdleTimeout; }

// 	// Aux Method
	@Override
	public long getFlowInstallationTimeout(){ return flowInstallationTimeout; }

	@Override
	public FlowObjectiveService getFlowObjectiveService(){ return flowObjectiveService; }

	@Override
	public IntentService getIntentService(){ return intentService; }

	@Override
	public PacketService getPacketService() {
		return packetService;
	}

	@Override
	public String getVpdcClientPrefix(){ return vpdcClientPrefix; }

	@Override
	public String getVpdcInternalPrefix(){ return vpdcInternalPrefix; }

	@Override
	public TopologyService getTopologyService() { return topologyService; }

	public LinkService getLinkService(){ return linkService; }

	@Override
	public ConsistentMap<IpPrefix, MacAddress> getHostMacMap(){
		return hostMacMap;
	}

	@Override
	public Driver getDriver(){
		return flowDriver;
	}
	@Override
	public ConsistentMap<String, Device> getL1Devices(){
		return l1Devices;
	}
	@Override
	public ConsistentMap<String, Device> getL4Devices(){
		return l4Devices;
	}
	@Override
	public ConsistentMap<String, Device> getSpineDevices(){
		return spineDevices;
	}
	@Override
	public ConsistentMap<String, Device> getL2L3Devices(){
		return l2l3Devices;
	}

	@Override
	public String getCtpdMacPrefix(){
		return ctpdMacPrefix;
	}

	@Override
	public String getIp6ServicePrefix(){
		return serviceIp6Prefix;
	}

	@Override
	public String getIp4ServicePrefix(){
		return serviceIp4Prefix;
	}

	@Override
	public ConsistentMap<DeviceId, Set<PortNumber>> getHostPorts(){
		return hostPorts;
	}

	@Override
	public Set<Device> getDevicesClosFwd(){

		Iterator<Device> devices = deviceService.getDevices().iterator();
		Set<Device> setDevices = new HashSet<Device>(IteratorUtils.toList(devices));
		return setDevices;
	}

	public String getOpenFlowL1Preffix() {
		return l1OpenflowSwitchPrefix;
	}

	public String getOpenFlowOLTPreffix() {
		return oltOpenflowSwitchPrefix;
	}

	public String getOpenFlowSpinePreffix() {
		return spineOpenflowSwitchPrefix;
	}

	private class InternalDeviceListener implements DeviceListener {

		@Override
		public void event(DeviceEvent deviceEvent) {
			try{
				DeviceId deviceId = deviceEvent.subject().id();
				MastershipRole role = deviceService.getRole(deviceId);

				if (role == MastershipRole.MASTER) {

					NodeId nodeId = mastershipStore.getMaster(deviceId);
					lastKnownMaster.put(deviceId, nodeId);
				}

				// We trigger event handling only if it is an event from a device we are master ...
				// We only catch events of L2, L3 AND L4. L1 events are managed by Intent Service

				DeviceEvent.Type eventType = deviceEvent.type();
				log.debug("DeviceEvent Type:{}", eventType);

				if (eventType == DeviceEvent.Type.PORT_ADDED || eventType == DeviceEvent.Type.PORT_UPDATED || eventType == DeviceEvent.Type.PORT_REMOVED) {
					if (storageNetworkEnabled && isL2orL3Device(deviceId)) {
						Port port = deviceEvent.port();
						if (!port.isEnabled())
							if (storagePortCheck(deviceId, port.number()))
								delayToDownPorts(deviceId, deviceEvent.port());
					}
				}
			}
			catch(Exception e){
				log.error("Exception in Topology Event:", e);
			}
		}
	}


	private boolean storagePortCheck (DeviceId deviceId, PortNumber portNumber) {

		for (UUID storageUUID: storageRegistry.keySet()) {
			Versioned<Endpoint> versionedEndpoint = storageRegistry.get(storageUUID);
			if(versionedEndpoint != null) {
				Endpoint endpoint = versionedEndpoint.value();
				if (endpoint instanceof StorageEndpoint)
					if (endpoint.getPort().equals(portNumber))
						return true;
			}
		}
		return false;
	}


	private void delayToDownPorts(DeviceId devicePort, Port portDown){

		Thread t1 = new Thread(new Runnable() {
			public void run() {
				try {
					log.debug("Waiting 5 seconds to check if PORT_UPDATED is PORT_DOWN in {} port {} with state is {}", devicePort, portDown.number(), portDown.isEnabled());
					// log.debug("Port status before 15s: " + portDown);
					Thread.sleep(5000);

					for(Port port : deviceService.getPorts(devicePort)){
						if(port.number().equals(portDown.number())){
							// log.debug("Port status after 15s: " + port);
							if (!port.isEnabled()){
								log.debug("portDownAllHostPorts because PORT_UPDATED is PORT_DOWN in {} port {}", devicePort, portDown.number());
								Set<PortNumber> portSet = new HashSet<PortNumber>();
								if(hostPorts.get(devicePort) != null){
									portSet = hostPorts.get(devicePort).value();
								}
								portSet.add(portDown.number());
								hostPorts.put(devicePort, portSet);

								portDownAllHostPorts();
							}else{
								log.debug("Nothing to do because PORT_UPDATED is PORT_UP in {} port {}", devicePort, portDown.number());
							}
						}
					}
					log.debug("Ports to host detected " + hostPorts);
				} catch (InterruptedException e) {
					log.error("Thread error waiting");
				}
			}
		});
		t1.start();
	}



	private void portDownAllHostPorts(){

		//Here we do port down to all hosts

		Iterator<Device> devices = deviceService.getDevices().iterator();
		while(devices.hasNext()) {
			Device device = devices.next();
			Set<PortNumber> setPorts = new HashSet<PortNumber>();

			if(hostPorts.get(device.id()) != null){
				setPorts = hostPorts.get(device.id()).value();
			}
			List <PortNumber> portLinksByDevice = new ArrayList<>();
			if (!(device.id().toString().startsWith(spineOpenflowSwitchPrefix))
				&& !(device.id().toString().startsWith(l1OpenflowSwitchPrefix))
				&& !(device.id().toString().startsWith(l4OpenflowSwitchPrefix))
				&& !(device.id().toString().startsWith(oltOpenflowSwitchPrefix))){

				for(Link link : getLinkService().getDeviceEgressLinks(device.id())){
					portLinksByDevice.add(link.src().port());
				}
				for(Port port : deviceService.getPorts(device.id())){
					if(!(portLinksByDevice.contains(port.number())) && (port.isEnabled())){
						log.debug("Administratively broughting DOWN port {} of {}", port.number(), device.id());
						deviceAdminService.changePortState(device.id(), port.number(), false);
						setPorts.add(port.number());
					}
				}

			hostPorts.put(device.id(),setPorts);
			}
		}
		log.debug("Ports Down host ports: " + hostPorts);
	}


	private class InternalTopologyListener implements TopologyListener {
		@Override
		public void event(TopologyEvent event) {
			try{
				Optional<NodeId> nodeid = mastershipinfo.master();
				// log.info("MastershipInfo  " + nodeid.toString());
				log.debug("InternalTopologyListener");
				log.debug("[event] TopologyEvent Detected");
				List<Event> reasons = event.reasons();
				// flagDeviceEvent = false;
				if (reasons != null) {
					reasons.forEach(re -> {
						log.debug("re-> " + re.toString());
						if (re instanceof LinkEvent) {
							LinkEvent le = (LinkEvent) re;
							// MastershipRole srcRole = deviceService.getRole(le.subject().src().deviceId());
							DeviceId deviceId = le.subject().src().deviceId();
							NodeId currentNode = clusterService.getLocalNode().id();
							NodeId lastMaster = lastKnownMaster.get(deviceId).value();
							// We trigger event handling only if it is an event from a device we are master ...
							// We only catch events of L2, L3 AND L4. L1 events are managed by Intent Service
							// if (currentNode.equals(lastMaster)
							if (currentNode.equals(lastMaster)
								&& isL2orL3orL4Device(deviceId)
								&& useEcmp) {
								if (re.type() == LinkEvent.Type.LINK_ADDED  ||
									re.type() == LinkEvent.Type.LINK_REMOVED ||
									re.type() == LinkEvent.Type.LINK_UPDATED) {
									log.debug("[LinkEvent] LinkEvent Catched and Managed by ClosFwd App");
									log.debug("[LinkEvent] Modify detected in link {} on deviceId {}",le.subject(),deviceId.toString());
									// log.debug("Role " + srcRole.toString());
									// Put event in queue ...

									if(isL2orL3Device(le.subject().src().deviceId())
										&& isSpineDevice(le.subject().dst().deviceId())
										&& storageNetworkEnabled){
										Iterator<Link> links = getLinkService().getDeviceEgressLinks(deviceId).iterator();
										boolean isThereBakupLink = false;
										while(links.hasNext()){
											Link link = links.next();
											if(isL2orL3orL4Device(link.src().deviceId())
												&& isSpineDevice(link.dst().deviceId())
												&& link.src().deviceId().equals(le.subject().src().deviceId())
												&& !link.dst().deviceId().equals(le.subject().dst().deviceId())){
													isThereBakupLink = true;
											}
										}
										if(!isThereBakupLink){
											log.debug("No backup link found to join leaf {} with Spine {} Necesary to disable storage network in clos", le.subject().src().deviceId(), le.subject().dst().deviceId());
											portDownAllHostPorts();
										}else{
											log.debug("Not necesary to disable storage network in clos");
										}
									}

									pendingTopologyEvents.put(le.toString(), le);
									handleTopologyEvents();
								}
								else{
									log.debug("[LinkEvent] Unhandled LinkEvent event {} from {}", le.type() ,deviceId);
								}

							}else if(!useEcmp){
								log.debug("[LinkEvent] Unhandled LinkEvent beacuse UseEcmp is False", le.type() ,deviceId);

							}else if(!currentNode.equals(lastMaster)){
								log.debug("[LinkEvent] Not master controller for link {}", linkIdFromDevices(le.subject().src().deviceId(), le.subject().dst().deviceId()));
							}else{
								log.debug("[LinkEvent] Unhandled LinkEvent event {} from {}", le.type() ,deviceId);
							}
						}
					});
				}
			}
			catch(Exception e){
				log.error("Exception in Topology Event:", e);
			}
		}
	}

	private void handleTopologyEvents() {

		// We do handle here events from all devices. We guaranteed previously and event is added just once.

		log.debug("inprogressTopologyEvents size: " +  inprogressTopologyEvents.size());
		log.debug("pendingTopologyEvents size: " +  pendingTopologyEvents.size());


		if(inprogressTopologyEvents.size()==0)
		{
			log.debug("handle-topology-loop-start");
			Thread t1 = new Thread(new Runnable() {
				public void run() {
					while(pendingTopologyEvents.size()>0) {
						log.debug("Thread started");
						for(String eventRead: pendingTopologyEvents.keySet()){
							Object eventReadVersioned = pendingTopologyEvents.get(eventRead).value();

							//Always link events storaged in pendingTopologyEvents
							LinkEvent le = (LinkEvent) eventReadVersioned;

							inprogressTopologyEvents.put(eventRead, eventReadVersioned);
							pendingTopologyEvents.remove(eventRead);

							if (isL2orL3orL4Device(le.subject().src().deviceId()) && isSpineDevice(le.subject().dst().deviceId())){ // TO DO Maybe not necesary because our topology

								log.debug("handle-topology-link-event");

								DeviceId leafDevice = le.subject().src().deviceId();
								DeviceId spineDevice = le.subject().dst().deviceId();

								// log.debug("Link Down or Up with src {} and dst {}", leafDevice, spineDevice);
								// First we have to check if there are more buckup links for the link down
								boolean backupFound = false;

								for (Link link: getLinkService().getDeviceEgressLinks(leafDevice)) {
									if (link.src().deviceId().equals(leafDevice) &&
										link.dst().deviceId().equals(spineDevice) &&
										!link.src().port().equals(le.subject().src().port()) &&
										!link.dst().port().equals(le.subject().dst().port()) &&
										link.state().equals(Link.State.ACTIVE)) {
										backupFound = true;
										log.debug("BackUp Link to link up/down found. Nothing to do");
										break;
									}
								}

								for (Device deviceOther : deviceService.getDevices()){
									if (!leafDevice.equals(deviceOther.id())
										&& isL2orL3orL4Device(deviceOther.id())){
										if (((le.type() == LinkEvent.Type.LINK_REMOVED) || le.subject().state().equals(Link.State.INACTIVE))
												&& !backupFound){
											Iterator<Link> links = getLinkService().getDeviceEgressLinks(deviceOther.id()).iterator();
											while(links.hasNext()){
												Link link = links.next();
												if (link.dst().deviceId().equals(spineDevice)){
													updateEcmpGroup(link.src().port(), deviceOther.id(), leafDevice, true);
												}
											}
											//Delete all bypass flows to avoid loss packets because the link event
											flowRuleService.removeFlowRulesById(getBypassApplicationId());

										} else if ((le.type() == LinkEvent.Type.LINK_ADDED) || le.subject().state().equals(Link.State.ACTIVE)) {
											log.debug("Link Added handled by ClosFwd App");

											// String ecmpApPlicationIdString = "EcmpApplicationId";
											// UUID ecmpApPlicationIdUuid = UUID.nameUUIDFromBytes(ecmpApPlicationIdString.getBytes());
											// ApplicationId  appIdLocal = appIdRegistry.get(ecmpApPlicationIdUuid).value();
											// String srcLeafString = deviceOther.id().toString();
											// String dstLeafString = leafDevice.toString();
											// String keySrcDstLeaf = srcLeafString+"/" + dstLeafString;

											// First we delete all flows affected

											for(Versioned<Endpoint> endpoint: registry.values()){
												if (leafDevice.equals(endpoint.value().getNode())){
													log.debug("Recreating endpoint {} in device {} ", endpoint.toString(), deviceOther.id());
													installFlows(deviceOther.id(), endpoint.value(), false);
												}
											}

											// We have to delete ECMP groups to avoid 16 ECMP group limit.
											// Onos group service remove is too slow, so we delete this groups manually to avoid errors when creating
											updateEcmpGroup(null, deviceOther.id(), leafDevice, false);

											// Now we can create again this flows deleted before, because we have
											// already deleted ECMP groups not used avoiding errors of 16 limit and chained groups
											for(Versioned<Endpoint> endpoint: registry.values()){
												if (leafDevice.equals(endpoint.value().getNode())){
													installFlows(deviceOther.id(), endpoint.value(), true);
												}
											}

											//Delete all bypass flows to avoid loss packets because the link event
											flowRuleService.removeFlowRulesById(getBypassApplicationId());

										}else{
											log.debug("Link event no handled by closFwdApp");
										}
									}
								inprogressTopologyEvents.remove(eventRead);
								}
							}else{
								log.debug("No need to modify any flow");
							}
							inprogressTopologyEvents.remove(eventRead);
						}
					}
				}
			});
			t1.start();
		}
		else {
			log.debug("handle-topology-already-in-progress");
		}
		log.debug("handle-topology-event-stop");
	}


	// Method to install existing endpoints in a new switch

	private void installFlows(DeviceId deviceId, Endpoint endpoint, boolean create) {

		// We discard OLT and L1 switches...
		if (deviceId.toString().startsWith(l1OpenflowSwitchPrefix) ||
			deviceId.toString().startsWith(oltOpenflowSwitchPrefix))
			return;

		if (deviceId.toString().startsWith(l4OpenflowSwitchPrefix))
			flowDriver.installL4Flows(endpoint, deviceId, create);
		else if (deviceId.toString().startsWith(spineOpenflowSwitchPrefix))
			flowDriver.installSpineFlows(endpoint, deviceId, create);
		else
			flowDriver.installL2L3Flows(endpoint, deviceId, create);
			log.debug("Installed flows on device {} for endpoint {}  ", deviceId.toString(), endpoint.toString());
		}

	// Method to delete ecmp groups that connects Devices with links Down

	private void updateEcmpGroup(PortNumber port, DeviceId deviceId, DeviceId triggeringDevice, boolean linkDisabled) {

		String ecmpApPlicationIdString = "EcmpApplicationId";
		UUID ecmpApPlicationIdUuid = UUID.nameUUIDFromBytes(ecmpApPlicationIdString.getBytes());
		ApplicationId  appIdLocal = appIdRegistry.get(ecmpApPlicationIdUuid).value();
		MacAddress leafDstMac = getLeafDstMac(triggeringDevice);

		// Get all ecmps for the affected device (max 16)
		Iterable <Group> groupsByAppAndDevice = groupService.getGroups(deviceId, appIdLocal);

		// Look for affected ecmp
		for (Group groupEcmp : groupsByAppAndDevice){
			if(groupEcmp.appId().id() == appIdLocal.id()){ // TO DO Maybe not necesary because previous AppId filter. Pending to test
				if (groupEcmp.type() == GroupDescription.Type.SELECT){
					GroupBuckets bucketsEcmp = groupEcmp.buckets();
					// log.debug("Buckets ECMP " + bucketsEcmp);
					// Then we obtain buckets of ECMP group that join L3 Unicast Groups
					for (GroupBucket bucketEcmp : bucketsEcmp.buckets()){
						if (bucketEcmp.type()== GroupDescription.Type.SELECT){
							// log.debug("Group Description Type " + bucketEcmp.type());
							for (Instruction instructionEcmp : bucketEcmp.treatment().allInstructions()){
								if (instructionEcmp.type() == Instruction.Type.GROUP){
									GroupInstruction groupInstructionEcmp = (GroupInstruction) instructionEcmp;
									// Then we obtain l3Unicast group of each ECMP bucket
									Group l3Unicast = groupStore.getGroup(deviceId, groupInstructionEcmp.groupId());
									// groupEcmpToDelete.add(groupEcmp);
									// log.debug("Group l3 Unicast " + l3Unicast);
									if (l3Unicast.type() == GroupDescription.Type.INDIRECT){
										GroupBuckets bucketsl3Unicast = l3Unicast.buckets();
										// log.debug("bucketsl3Unicast " + bucketsl3Unicast);
										// Then we obtain the bucket of L3 Unicast group that join L2 Interface group
										for (GroupBucket bucketl3Unicast : bucketsl3Unicast.buckets()){
											if (bucketl3Unicast.type()== GroupDescription.Type.INDIRECT){
												boolean checkLeafDst = false;
												for (Instruction instructionl3Unicast : bucketl3Unicast.treatment().allInstructions()){
													// Only l3 unicast with set mac dst to triggering device
													if (instructionl3Unicast instanceof L2ModificationInstruction.ModEtherInstruction){
														L2ModificationInstruction.ModEtherInstruction l2ModificationInstruction = (L2ModificationInstruction.ModEtherInstruction) instructionl3Unicast;
														if ((l2ModificationInstruction.subtype() == L2ModificationInstruction.L2SubType.ETH_DST)
															&& l2ModificationInstruction.mac().equals(leafDstMac)){
																checkLeafDst = true;
															}
													}
												}
												if (checkLeafDst) {
													for (Instruction instructionl3Unicast : bucketl3Unicast.treatment().allInstructions()){
														if ((instructionl3Unicast.type() == Instruction.Type.GROUP)){
															GroupInstruction groupInstructionl3Unicast = (GroupInstruction) instructionl3Unicast;
															// log.debug("Group Instruction l3 Unicast " + groupInstructionl3Unicast);
															// Then we obtain l2Interface group of the previous L3 Unicast bucket
															Group l2Interface = groupStore.getGroup(deviceId, groupInstructionl3Unicast.groupId());
															// log.debug("l2Interface " + l2Interface);
															// groupL3UnicasteToDelete.add(l3Unicast);
															if (l2Interface.type() == GroupDescription.Type.INDIRECT){
																GroupBuckets bucketsl2Interface = l2Interface.buckets();
																// log.debug("bucketsl2Interface " + bucketsl2Interface);
																// Then we obtain bucket of L2 Interface group that join output port
																for (GroupBucket bucketl2Interface : bucketsl2Interface.buckets()){
																	// log.debug("bucketl2Interface " + bucketl2Interface);
																	if (bucketl2Interface.type()== GroupDescription.Type.INDIRECT){
																		for (Instruction instructionl2Interface : bucketl2Interface.treatment().allInstructions()){
																			// log.debug("instructionl2Interface " + instructionl2Interface);
																			if (instructionl2Interface.type() == Instruction.Type.OUTPUT){
																				OutputInstruction outputInstructionL2Interface = (OutputInstruction) instructionl2Interface;
																				PortNumber portToCheck = outputInstructionL2Interface.port();
																				List<GroupBucket> groupBucketToDelete = Arrays.asList(bucketEcmp);
																				GroupBuckets groupBucketsToDelete = new GroupBuckets(groupBucketToDelete);
																				// log.debug("groupBucketsToDelete " + groupBucketsToDelete);
																				// log.debug("Port To Check " + portToCheck);
																				// log.debug("port " + port);
																				if (portToCheck.equals(port) && linkDisabled){
																					groupService.removeBucketsFromGroup(deviceId, groupEcmp.appCookie(), groupBucketsToDelete, null, appIdLocal);
																					log.debug("Delete correctly buckets from group {} from device {} in port {} with mac dst {} so leaf dst {}", groupEcmp.id(), deviceId, port, leafDstMac, triggeringDevice);
																				} else if (!linkDisabled) {
																					log.debug("Deleted ECMP group {} to avoid 16 limit that joined {} with {}", groupEcmp, deviceId, triggeringDevice);
																					groupService.removeGroup(deviceId, groupEcmp.appCookie(), appIdLocal);
																				}
																			}
																		}
																	}
																}
															}
														}
													}
												}
											}
										}
									}
								}
							}
						}
					}
				}
			}
		}
		// if (groupEcmpToDelete != null && port == null){
		// 	groupService.removeGroup(deviceId, groupEcmpToDelete.appCookie(), appIdLocal);
		// 	log.debug("Deleted group ECMP to avoid 16 limit before new ECMP create");
		// }
	}

 	// private void installServiceFlowsByPort(DeviceId deviceId, PortNumber port, boolean create) {
	// 	// Look for all service enpoints in that port
	// 	for(Versioned<Endpoint> endpoint: registry.values())
	// 	{
	// 		if(endpoint.value().getPort().equals(port))
	// 		{
	// 			endpointManager.installEndpointFlows(endpoint.value(), create);
	// 		}
	// 	}
	// }

	@Override
	public void requestPackets(Endpoint src, Endpoint dst) {

		log.debug("Controller flows requested for egress scenario");
		DeviceId deviceId = src.getNode();
		//DeviceId deviceId = DeviceId.deviceId(l2OpenflowSwitch);
		TrafficSelector.Builder selector;

		if(src.getIpPrefix().isIp6()) {
			selector = DefaultTrafficSelector.builder()
					.matchInPort(src.getPort())
					.matchEthType(Ethernet.TYPE_IPV6)
					.matchVlanId(src.getVlan())
					//.matchIPProtocol(IPv6.PROTOCOL_ICMP6)
					.matchIPv6Src(src.getIpPrefix())
					.matchIPv6Dst(dst.getIpPrefix());
		}
		else {
			selector = DefaultTrafficSelector.builder()
					.matchInPort(src.getPort())
					.matchEthType(Ethernet.TYPE_IPV4)
					.matchVlanId(src.getVlan())
					//.matchIPProtocol(IPv6.PROTOCOL_ICMP6)
					.matchIPSrc(src.getIpPrefix())
					.matchIPDst(dst.getIpPrefix());
		}

		packetService.requestPackets(selector.build(), PacketPriority.CONTROL, appId, Optional.of(deviceId));
	}

	@Override
    public void withdrawPackets(Endpoint src, Endpoint dst) {

		log.debug("Controller flows requested to be removed for egress scenario");
        DeviceId deviceId = src.getNode();
        // DeviceId deviceId = DeviceId.deviceId(l2OpenflowSwitch);

        TrafficSelector.Builder selector = DefaultTrafficSelector.builder()
                .matchInPort(src.getPort())
                .matchEthType(Ethernet.TYPE_IPV6)
                .matchVlanId(src.getVlan())
                //.matchIPProtocol(IPv6.PROTOCOL_ICMP6)
                .matchIPv6Src(src.getIpPrefix())
                .matchIPv6Dst(dst.getIpPrefix());

        packetService.cancelPackets(selector.build(), PacketPriority.CONTROL, appId, Optional.of(deviceId));
	}

	public ClientServiceBypassEndpoint getBypassEndpointFromIp(IpAddress ipAddress) {

		IpPrefix ipPrefix = IpPrefix.valueOf(ipAddress, vpdcClientPrefixlength);
		Versioned<UUID> versionedUUID = ipToUUIDRegistry.get(ipPrefix);
		UUID uuid;

		if(versionedUUID!=null)
		{
			log.debug("UUID found from IP!");
			uuid = versionedUUID.value();
			if(uuid!=null)
			{
				log.debug("UUID found is " + uuid.toString());
				Versioned<Endpoint> versionedEndpoint =  vpdcRegistry.get(uuid);
				if(versionedEndpoint!=null)
				{
					log.debug("Endpoint found from UUID!");
					Endpoint endpoint = versionedEndpoint.value();
					if (endpoint instanceof ClientServiceBypassEndpoint)
					{
						log.debug("Endpoint found!");
						return (ClientServiceBypassEndpoint) endpoint;
					}
				}
			}
		}

		log.debug("Bypass enpoint not found from IP!");

		return null;
	}

	public ServiceEndpoint ipToBypassServiceEndpoint(IpAddress ipAddress, ClientServiceBypassEndpoint bypass)
	{
		ArrayList<UUID> uuids;
		uuids = bypass.getServiceUUIDs();

		if (uuids!=null)
		{
			for(UUID uuid: bypass.getServiceUUIDs())
			{
				Endpoint endpoint = getEndpoint(uuid);
				if(endpoint instanceof ServiceEndpoint)
				{
					if (endpoint != null) {
						if (endpoint.getIpPrefix().contains(ipAddress))
							return (ServiceEndpoint) endpoint;
					}
				}
			}
		}
		return null;
	}

	private void readComponentConfiguration(ComponentContext context) {
		Dictionary<?, ?> properties = context.getProperties();

		try {
			String s = get(properties, "flowTimeout");
			flowTimeout = isNullOrEmpty(s) ? OsgiPropertyConstants.DEFAULT_FLOW_TIMEOUT : Integer.parseInt(s.trim());
		} catch (Exception e) {
			flowTimeout= OsgiPropertyConstants.DEFAULT_FLOW_TIMEOUT;
		}

		try {
			String s = get(properties, "flowPriority");
			flowPriority = isNullOrEmpty(s) ? OsgiPropertyConstants.DEFAULT_FLOW_PRIORITY : Integer.parseInt(s.trim());
		} catch (Exception e) {
			flowPriority = OsgiPropertyConstants.DEFAULT_FLOW_PRIORITY;
		}

		try {
			String s = get(properties, "bypassFlowPriority");
			bypassFlowPriority = isNullOrEmpty(s) ? OsgiPropertyConstants.DEFAULT_BYPASS_FLOW_PRIORITY : Integer.parseInt(s.trim());
		} catch (Exception e) {
			bypassFlowPriority = OsgiPropertyConstants.DEFAULT_BYPASS_FLOW_PRIORITY;
		}

		try {
			String s = get(properties, "ctpdFakeInternalMacAddress");
			ctpdFakeInternalMacAddress = isNullOrEmpty(s) ? OsgiPropertyConstants.CTPD_FAKE_INTERNAL_MAC_ADDRESS : s.trim();
		} catch (Exception e) {
			ctpdFakeInternalMacAddress = OsgiPropertyConstants.CTPD_FAKE_INTERNAL_MAC_ADDRESS;
		}

		try {
			String s = get(properties, "ctpdFakeExternalMacAddress");
			ctpdFakeExternalMacAddress = isNullOrEmpty(s) ? OsgiPropertyConstants.CTPD_FAKE_EXTERNAL_MAC_ADDRESS : s.trim();
		} catch (Exception e) {
			ctpdFakeExternalMacAddress = OsgiPropertyConstants.CTPD_FAKE_EXTERNAL_MAC_ADDRESS;
		}

		try {
			String s = get(properties, "vpdcClientPrefixlength");
			vpdcClientPrefixlength = isNullOrEmpty(s) ? OsgiPropertyConstants.DEFAULT_VPDC_CLIENT_PREFIX_LENGTH : Integer.parseInt(s.trim());
		} catch (Exception e) {
			vpdcClientPrefixlength = OsgiPropertyConstants.DEFAULT_VPDC_CLIENT_PREFIX_LENGTH;
		}

        try {
            String s = get(properties, "l1OpenflowSwitchPrefix");
            l1OpenflowSwitchPrefix = isNullOrEmpty(s) ? OsgiPropertyConstants.DEFAULT_L1_OPENFLOW_SWITCH_PREFIX : s.trim();
        } catch (Exception e) {
            l1OpenflowSwitchPrefix = OsgiPropertyConstants.DEFAULT_L1_OPENFLOW_SWITCH_PREFIX;
		}

		try {
            String s = get(properties, "l4OpenflowSwitchPrefix");
            l4OpenflowSwitchPrefix = isNullOrEmpty(s) ? OsgiPropertyConstants.DEFAULT_L4_OPENFLOW_SWITCH_PREFIX : s.trim();
        } catch (Exception e) {
            l4OpenflowSwitchPrefix = OsgiPropertyConstants.DEFAULT_L4_OPENFLOW_SWITCH_PREFIX;
		}

		try {
            String s = get(properties, "oltOpenflowSwitchPrefix");
            oltOpenflowSwitchPrefix = isNullOrEmpty(s) ? OsgiPropertyConstants.DEFAULT_L1_OPENFLOW_SWITCH_PREFIX : s.trim();
        } catch (Exception e) {
            oltOpenflowSwitchPrefix = OsgiPropertyConstants.DEFAULT_L1_OPENFLOW_SWITCH_PREFIX;
		}

		try {
            String s = get(properties, "spineOpenflowSwitchPrefix");
            spineOpenflowSwitchPrefix = isNullOrEmpty(s) ? OsgiPropertyConstants.DEFAULT_SPINE_OPENFLOW_SWITCH_PREFIX : s.trim();
        } catch (Exception e) {
            spineOpenflowSwitchPrefix = OsgiPropertyConstants.DEFAULT_SPINE_OPENFLOW_SWITCH_PREFIX;
        }

		try {
			String s = get(properties, "neighbourSolicitationInterval");
			neighbourSolicitationInterval = isNullOrEmpty(s) ? OsgiPropertyConstants.DEFAULT_NEIGHBOUR_SOLICITATION_INTERVAL : Integer.parseInt(s.trim());
		} catch (Exception e) {
			neighbourSolicitationInterval = OsgiPropertyConstants.DEFAULT_NEIGHBOUR_SOLICITATION_INTERVAL;
		}

		try {
			String s = get(properties, "flowInstallationTimeout");
			flowInstallationTimeout = isNullOrEmpty(s) ? OsgiPropertyConstants.DEFAULT_FLOW_INSTALLATION_TIMEOUT : Long.parseLong(s.trim());
		} catch (Exception e) {
			flowInstallationTimeout = OsgiPropertyConstants.DEFAULT_FLOW_INSTALLATION_TIMEOUT;
		}

		try {
			String s = get(properties, "ofdpaActivated");
			ofdpaActivated = isNullOrEmpty(s) ? true : Boolean.parseBoolean(s.trim());
			if(ofdpaActivated)
				flowDriver = new FlowDriverOfdpa();
			else
				flowDriver = new FlowDriverOvs();
		} catch (Exception e) {
			ofdpaActivated = OsgiPropertyConstants.DEFAULT_OFDPA_ACTIVATED;
		}

		try {
			String s = get(properties, "emptyVlanIdP");
			emptyVlanIdP = isNullOrEmpty(s) ? OsgiPropertyConstants.DEFAULT_EMPTY_VLAN_ID : Integer.parseInt(s.trim());
		} catch (Exception e) {
			emptyVlanIdP = OsgiPropertyConstants.DEFAULT_EMPTY_VLAN_ID;
		}

		try {
			String s = get(properties, "serviceVlanId");
			serviceVlanId = isNullOrEmpty(s) ? OsgiPropertyConstants.DEFAULT_SERVICE_VLAN_ID : Integer.parseInt(s.trim());
		} catch (Exception e) {
			serviceVlanId = OsgiPropertyConstants.DEFAULT_SERVICE_VLAN_ID;
		}

		try {
			String s = get(properties, "extServiceVlanId");
			extServiceVlanId = isNullOrEmpty(s) ? OsgiPropertyConstants.DEFAULT_EXTERNAL_SERVICE_VLAN_ID : Integer.parseInt(s.trim());
		} catch (Exception e) {
			extServiceVlanId = OsgiPropertyConstants.DEFAULT_EXTERNAL_SERVICE_VLAN_ID;
		}

		try {
			String s = get(properties, "bypassVlanIdP");
			bypassVlanIdP = isNullOrEmpty(s) ? OsgiPropertyConstants.DEFAULT_BYPASS_VLAN_ID : Integer.parseInt(s.trim());
		} catch (Exception e) {
			bypassVlanIdP = OsgiPropertyConstants.DEFAULT_BYPASS_VLAN_ID;
		}

		try {
			String s = get(properties, "monoetiqueta");
			monoetiqueta = isNullOrEmpty(s) ? false : Boolean.parseBoolean(s.trim());
		} catch (Exception e) {
			monoetiqueta = OsgiPropertyConstants.DEFAULT_MONOETIQUETA;
		}

        try {
            String s = get(properties, "defaultVlan");
            defaultVlan = isNullOrEmpty(s) ? OsgiPropertyConstants.DEFAULT_DEFAULT_VLAN_ID : Integer.parseInt(s.trim());
        } catch (Exception e) {
            defaultVlan = OsgiPropertyConstants.DEFAULT_DEFAULT_VLAN_ID;
        }

        try {
            String s = get(properties, "internetServicesFlowPriority");
            internetServicesFlowPriority = isNullOrEmpty(s) ? OsgiPropertyConstants.DEFAULT_INTERNET_SERVICES_FLOW_PRIOTITY : Integer.parseInt(s.trim());
        } catch (Exception e) {
            internetServicesFlowPriority = OsgiPropertyConstants.DEFAULT_INTERNET_SERVICES_FLOW_PRIOTITY;
        }

        try {
            String s = get(properties, "respondNDPLocally");
            respondNDPLocally = isNullOrEmpty(s) ? OsgiPropertyConstants.DEFAULT_RESPOND_NDP_LOCALLY : Boolean.parseBoolean(s.trim());
        } catch (Exception e) {
            respondNDPLocally = OsgiPropertyConstants.DEFAULT_RESPOND_NDP_LOCALLY;
        }

        try {
            String s = get(properties, "client2ServiceIdleTimeout");
            client2ServiceIdleTimeout = isNullOrEmpty(s) ? OsgiPropertyConstants.DEFAULT_CLIENT_SERVICE_IDLE_TIMEOUT : Integer.parseInt(s.trim());
        } catch (Exception e) {
            client2ServiceIdleTimeout =  OsgiPropertyConstants.DEFAULT_CLIENT_SERVICE_IDLE_TIMEOUT;
        }

        try {
            String s = get(properties, "closPacketProcessorPriority");
            closPacketProcessorPriority = isNullOrEmpty(s) ? OsgiPropertyConstants.DEFAULT_PACKET_PRIORITY_PROCESSOR : Integer.parseInt(s.trim());
        } catch (Exception e) {
            closPacketProcessorPriority = OsgiPropertyConstants.DEFAULT_PACKET_PRIORITY_PROCESSOR;
        }

        try {
            String s = get(properties, "streamActivated");
            streamActivated = isNullOrEmpty(s) ? OsgiPropertyConstants.DEFAULT_STREAM_ACTIVATED : Boolean.parseBoolean(s.trim());
        } catch (Exception e) {
            streamActivated = OsgiPropertyConstants.DEFAULT_STREAM_ACTIVATED;
        }

        try {
            String s = get(properties, "ctpdIp");
            ctpdIp = isNullOrEmpty(s) ? OsgiPropertyConstants.DEFAULT_CTPD_IPV6 : s.trim();
        } catch (Exception e) {
            ctpdIp = OsgiPropertyConstants.DEFAULT_CTPD_IPV6;
		}

		try {
            String s = get(properties, "ctpdIpv4");
            ctpdIpv4 = isNullOrEmpty(s) ? OsgiPropertyConstants.DEFAULT_CTPD_IPV4 : s.trim();
        } catch (Exception e) {
            ctpdIpv4 = OsgiPropertyConstants.DEFAULT_CTPD_IPV4;
		}

		try {
            String s = get(properties, "bgpCtpdIpv4");
            bgpCtpdIpv4 = isNullOrEmpty(s) ? OsgiPropertyConstants.DEFAULT_BGP_IPV4 : s.trim();
        } catch (Exception e) {
            bgpCtpdIpv4 = OsgiPropertyConstants.DEFAULT_BGP_IPV4;
		}

		try {
            String s = get(properties, "bgpCtpdIpv6");
            bgpCtpdIpv6 = isNullOrEmpty(s) ? OsgiPropertyConstants.DEFAULT_BGP_IPV6 : s.trim();
        } catch (Exception e) {
            bgpCtpdIpv6 =  OsgiPropertyConstants.DEFAULT_BGP_IPV6;
		}

		try {
			String s = get(properties, "keepData");
			keepData= isNullOrEmpty(s) ? OsgiPropertyConstants.DEFAULT_KEEP_DATA : Boolean.parseBoolean(s.trim());
		} catch (Exception e) {
			keepData =  OsgiPropertyConstants.DEFAULT_KEEP_DATA;
		}

		try {
			String s = get(properties, "checkLocalNDP");
			checkLocalNDP= isNullOrEmpty(s) ? OsgiPropertyConstants.DEFAULT_CHECK_LOCAL_NDP : Boolean.parseBoolean(s.trim());
		} catch (Exception e) {
			checkLocalNDP = OsgiPropertyConstants.DEFAULT_CHECK_LOCAL_NDP;
		}

		try {
            String s = get(properties, "vpdcClientPrefix");
            vpdcClientPrefix = isNullOrEmpty(s) ? OsgiPropertyConstants.DEFAULT_VPDC_CLIENT_PREFIX : s.trim();
        } catch (Exception e) {
            vpdcClientPrefix = OsgiPropertyConstants.DEFAULT_VPDC_CLIENT_PREFIX;
		}

		try {
            String s = get(properties, "vpdcInternalPrefix");
            vpdcInternalPrefix = isNullOrEmpty(s) ? OsgiPropertyConstants.DEFAULT_VPDC_INTERNAL_PREFIX : s.trim();
        } catch (Exception e) {
            vpdcInternalPrefix = OsgiPropertyConstants.DEFAULT_VPDC_INTERNAL_PREFIX;
		}

		try {
            String s = get(properties, "ctpdMacPrefix");
            ctpdMacPrefix = isNullOrEmpty(s) ? OsgiPropertyConstants.DEFAULT_CTPD_MAC_PREFIX : s.trim();
        } catch (Exception e) {
            ctpdMacPrefix = OsgiPropertyConstants.DEFAULT_CTPD_MAC_PREFIX;
		}

		try {
            String s = get(properties, "servicePrefix");
            serviceIp6Prefix = isNullOrEmpty(s) ? OsgiPropertyConstants.DEFAULT_SERVICE_IPV6_PREFIX : s.trim();
        } catch (Exception e) {
            serviceIp6Prefix = OsgiPropertyConstants.DEFAULT_SERVICE_IPV6_PREFIX;
		}

		try {
            String s = get(properties, "servicePrefix");
            serviceIp4Prefix = isNullOrEmpty(s) ? OsgiPropertyConstants.DEFAULT_SERVICE_IPV4_PREFIX : s.trim();
        } catch (Exception e) {
            serviceIp4Prefix = OsgiPropertyConstants.DEFAULT_SERVICE_IPV4_PREFIX;
		}

		try {
            String s = get(properties, "flowIdStart");
            flowIdStart = isNullOrEmpty(s) ? OsgiPropertyConstants.DEFAULT_FLOW_ID_START : Integer.parseInt(s.trim());
        } catch (Exception e) {
            flowIdStart = OsgiPropertyConstants.DEFAULT_FLOW_ID_START;
		}

		try {
			String s = get(properties, "useEcmp");
			useEcmp= isNullOrEmpty(s) ? OsgiPropertyConstants.DEFAULT_USE_ECMP : Boolean.parseBoolean(s.trim());
		} catch (Exception e) {
			useEcmp = OsgiPropertyConstants.DEFAULT_USE_ECMP;
		}

		try {
			String s = get(properties, "productionEnviorement");
			productionEnviorement= isNullOrEmpty(s) ? OsgiPropertyConstants.DEFAULT_PRODUCTION_ENVIRONMENT : Boolean.parseBoolean(s.trim());
		} catch (Exception e) {
			productionEnviorement = OsgiPropertyConstants.DEFAULT_PRODUCTION_ENVIRONMENT;
		}

		try {
			String s = get(properties, "storageNetworkEnabled");
			storageNetworkEnabled= isNullOrEmpty(s) ? OsgiPropertyConstants.DEFAULT_STORAGE_NETWORK_ENABLED : Boolean.parseBoolean(s.trim());
		} catch (Exception e) {
			storageNetworkEnabled = OsgiPropertyConstants.DEFAULT_STORAGE_NETWORK_ENABLED;
		}

	}

}





