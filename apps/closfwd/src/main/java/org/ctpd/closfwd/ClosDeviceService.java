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

package org.ctpd.closfwd;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.Set;

import org.onlab.packet.*;
import org.onlab.packet.ndp.NeighborAdvertisement;
import org.onlab.packet.ndp.NeighborSolicitation;
import org.onosproject.core.ApplicationId;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flowobjective.*;
import org.onosproject.net.link.LinkService;
import org.onosproject.net.packet.PacketContext;
import org.onosproject.net.packet.PacketService;
import org.onosproject.net.topology.TopologyService;
import org.onosproject.store.service.ConsistentMap;
import org.onosproject.store.service.DistributedSet;
import org.onosproject.net.intent.Key;
import org.onosproject.net.Port;



import org.onosproject.net.Device;
import org.ctpd.closfwd.*;
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.IntentService;

/**
 * Service for interacting with a clos device (ClosFwd).
 */

public interface ClosDeviceService {
// 	// extends ListenerService<ClosDeviceEvent, ClosDeviceListener> {

// 	/**
// 	 * Provisions connectivity for a couple of end points in the CLOS network
// 	 */
// 	String addPath(Endpoint srcHost, Endpoint dstHost,UUID srcUuid, UUID dstUuid, int typeOfPath);
// 	String addPath(Endpoint srcHost, Endpoint dstHost,UUID srcUuid, UUID dstUuid, int typeOfPath, MacAddress srcMacAddress, IpAddress srcIpAddress);

// //	/**	Use only with double tags!
// //	 *	Used by OLT via REST to install provisional path.
// //	 *  Provisions connectivity with Client and Portal
// //	 */
// //	String createTempPathClient2Portal(MacAddress macAddr, IpAddress ip, VlanId upperVlan);

// 	/**
// 	 * Removes connectivity for a couple of end points in the CLOS network
// 	 */


// 	String removePath(Endpoint srcHost, Endpoint dstHost,UUID srcUuid, UUID dstUuid, int typeOfPath);

	UUID addEndpoint(Endpoint device);

	Endpoint getEndpoint(UUID id);

	Endpoint removeEndpoint(UUID id);

// 	String updateVpdcHost(UUID vpdcHostUUID, ArrayList<IpPrefix> ipList);

	Set<UUID> getRegisterUUIDs();
	Set<UUID> getVpdcRegisterUUIDs();

// // 	Set<String> getRegisterPathIds();

	int getSize();

	ConsistentMap<String, Device> getL1Devices();
	ConsistentMap<String, Device> getL2L3Devices();
	ConsistentMap<String, Device> getL4Devices();
	ConsistentMap<String, Device> getSpineDevices();
	Driver getDriver();

	int purgeClos();
	int resetPermanentFlowIdRegister();
	int purgeFlowIdPermanentClos();
	String purgeIntents();
	String withdrawIntents();
	String recreateEndpoint(Endpoint endpoint);



	// boolean purgePath(String pathId);

// // 	String showClosSavedLinks();

// // 	String showClosSavedPaths();

	void requestPackets(Endpoint src, Endpoint dst);

	// void purgeClosByUuidEndpoint(Endpoint endpoint, Boolean force);

	void withdrawPackets(Endpoint src, Endpoint dst);

	void installControllerFlowsL2L3L4Devices();

	boolean checkExistingEndpoint(Endpoint endpoint);

	int getNextId(UUID endpointUUID, Boolean create);

// 	String processObjectiveError(ObjectiveError error);

	String linkIdFromDevices(DeviceId src, DeviceId dst);

	// ConsistentMap<String, List<Integer>> getNextIdStorage();


// 	MacAddress getNDMacAddressFromIpAddress(IpAddress ip);

// 	Endpoint getVpdcClientSideFromVPDCServiceSide(MacAddress vpdcMac);

// 	Endpoint getVpdcClientSideFromService(MacAddress serviceMac);

// // 	String removePathfromVlanIdMac(String pathId, boolean keep);

// // 	/*Get methods: get global variables from ClosFwd*/

// 	// String[] getOpenFlowSwitches();

	String getOpenFlowL1Preffix();

	String getOpenFlowOLTPreffix();

	String getOpenFlowSpinePreffix();

	// ConsistentMap<UUID, List<Integer>> getNextIdStorage();

	String getCtpdFakeInternalMacAddress();

	ApplicationId getEcmpApplicationId();

	ApplicationId getBypassApplicationId();

	boolean checkIfEcmpExists(DeviceId srcLeaf, DeviceId dstLeaf);

	int getNextIdForEcmp(DeviceId srcLeaf, DeviceId dstLeaf, Boolean create);

	String getCtpdFakeExternalMacAddress();

	boolean getProductionEnviorement();

	int getEmptyVlanId();

	int getServiceVlanId();

	int getExtServiceVlanId();

	int getBypassVlanId();

	VlanId getVlanExternal();

// 	int getDefaultVlan();

// 	int getInternetServicesFlowPriority();

// 	boolean getMonoetiqueta();

	int getVpdcClientPrefixlength();

	ConsistentMap<UUID, Endpoint> getRegistry();

// 	DistributedSet<MacAddress> getExternalServicesMacAddresses();

	boolean getrespondNDPLocally();

	boolean getcheckLocalNDP();

	String getCtpdIpv4();

	String getBgpCtpdIpv4();

	String getBgpCtpdIpv6();

	String getCtpdIpv6();

// 	UUID getClientUUID(Client device);

	ApplicationId getAppId();

	int getFlowPriority();

	int getClient2ServiceIdleTimeout();

	long getFlowInstallationTimeout();

	ApplicationId setApplicationFlowId(UUID uuidEndpoint);

	ApplicationId getApplicationFlowId(Endpoint endpoint);

	boolean getUseEcmp();

	FlowObjectiveService getFlowObjectiveService();

	ConsistentMap<UUID,Endpoint> getRegisterEndpoints();

	ConsistentMap<String,Key> getRegisterIntents();

	IntentService getIntentService();

	PacketService getPacketService();

	TopologyService getTopologyService();

	LinkService getLinkService();

	MacAddress getLeafDstMac(DeviceId deviceId);
// // 	ConsistentMap<String, PathData> getPathData();

// 	ConsistentMap<String, List<String>> getRouteCollectionData();

	ConsistentMap<IpPrefix, MacAddress> getHostMacMap();

	MacAddress getHostMac(DeviceId deviceId, PortNumber portNumber);

// 	String getMapsInfo();

// 	ConsistentMap<String, String> getLinkEventProcessingMap();

	String getVpdcClientPrefix();

	String getVpdcInternalPrefix();

	String getCtpdMacPrefix();

	String getIp6ServicePrefix();
	String getIp4ServicePrefix();

	int getBypassFlowPriority();

	ClientServiceBypassEndpoint getBypassEndpointFromIp(IpAddress ipAddress);

	ServiceEndpoint ipToBypassServiceEndpoint(IpAddress ipAddress, ClientServiceBypassEndpoint endpoint);

	ConsistentMap<DeviceId, Set<PortNumber>> getHostPorts();

	Set<Device> getDevicesClosFwd();
}
