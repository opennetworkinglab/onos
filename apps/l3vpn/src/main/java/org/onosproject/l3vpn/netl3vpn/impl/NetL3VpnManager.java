/*
 * Copyright 2017-present Open Networking Laboratory
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
package org.onosproject.l3vpn.netl3vpn.impl;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onlab.util.AbstractAccumulator;
import org.onlab.util.Accumulator;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.LeadershipEvent;
import org.onosproject.cluster.LeadershipEventListener;
import org.onosproject.cluster.LeadershipService;
import org.onosproject.cluster.NodeId;
import org.onosproject.config.DynamicConfigEvent;
import org.onosproject.config.DynamicConfigListener;
import org.onosproject.config.DynamicConfigService;
import org.onosproject.config.FailedException;
import org.onosproject.config.Filter;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.core.IdGenerator;
import org.onosproject.l3vpn.netl3vpn.AccessInfo;
import org.onosproject.l3vpn.netl3vpn.BgpDriverInfo;
import org.onosproject.l3vpn.netl3vpn.BgpInfo;
import org.onosproject.l3vpn.netl3vpn.DeviceInfo;
import org.onosproject.l3vpn.netl3vpn.FullMeshVpnConfig;
import org.onosproject.l3vpn.netl3vpn.HubSpokeVpnConfig;
import org.onosproject.l3vpn.netl3vpn.InterfaceInfo;
import org.onosproject.l3vpn.netl3vpn.NetL3VpnException;
import org.onosproject.l3vpn.netl3vpn.NetL3VpnStore;
import org.onosproject.l3vpn.netl3vpn.VpnConfig;
import org.onosproject.l3vpn.netl3vpn.VpnInstance;
import org.onosproject.l3vpn.netl3vpn.VpnSiteRole;
import org.onosproject.l3vpn.netl3vpn.VpnType;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Port;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.driver.DriverService;
import org.onosproject.yang.gen.v1.ietfinterfaces.rev20140508.ietfinterfaces.devices.device.Interfaces;
import org.onosproject.yang.gen.v1.ietfl3vpnsvc.rev20160730.ietfl3vpnsvc.DefaultL3VpnSvc;
import org.onosproject.yang.gen.v1.ietfl3vpnsvc.rev20160730.ietfl3vpnsvc.L3VpnSvc;
import org.onosproject.yang.gen.v1.ietfl3vpnsvc.rev20160730.ietfl3vpnsvc.accessvpnpolicy.VpnAttachment;
import org.onosproject.yang.gen.v1.ietfl3vpnsvc.rev20160730.ietfl3vpnsvc.accessvpnpolicy.vpnattachment.AttachmentFlavor;
import org.onosproject.yang.gen.v1.ietfl3vpnsvc.rev20160730.ietfl3vpnsvc.accessvpnpolicy.vpnattachment.attachmentflavor.DefaultVpnId;
import org.onosproject.yang.gen.v1.ietfl3vpnsvc.rev20160730.ietfl3vpnsvc.l3vpnsvc.DefaultSites;
import org.onosproject.yang.gen.v1.ietfl3vpnsvc.rev20160730.ietfl3vpnsvc.l3vpnsvc.Sites;
import org.onosproject.yang.gen.v1.ietfl3vpnsvc.rev20160730.ietfl3vpnsvc.l3vpnsvc.VpnServices;
import org.onosproject.yang.gen.v1.ietfl3vpnsvc.rev20160730.ietfl3vpnsvc.l3vpnsvc.sites.Site;
import org.onosproject.yang.gen.v1.ietfl3vpnsvc.rev20160730.ietfl3vpnsvc.l3vpnsvc.sites.site.SiteNetworkAccesses;
import org.onosproject.yang.gen.v1.ietfl3vpnsvc.rev20160730.ietfl3vpnsvc.l3vpnsvc.sites.site.sitenetworkaccesses.SiteNetworkAccess;
import org.onosproject.yang.gen.v1.ietfl3vpnsvc.rev20160730.ietfl3vpnsvc.l3vpnsvc.vpnservices.VpnSvc;
import org.onosproject.yang.gen.v1.ietfl3vpnsvc.rev20160730.ietfl3vpnsvc.siteattachmentbearer.Bearer;
import org.onosproject.yang.gen.v1.ietfl3vpnsvc.rev20160730.ietfl3vpnsvc.siteattachmentbearer.DefaultBearer;
import org.onosproject.yang.gen.v1.ietfl3vpnsvc.rev20160730.ietfl3vpnsvc.siteattachmentbearer.bearer.DefaultRequestedType;
import org.onosproject.yang.gen.v1.ietfl3vpnsvc.rev20160730.ietfl3vpnsvc.siteattachmentbearer.bearer.RequestedType;
import org.onosproject.yang.gen.v1.ietfl3vpnsvc.rev20160730.ietfl3vpnsvc.siteattachmentipconnection.IpConnection;
import org.onosproject.yang.gen.v1.ietfl3vpnsvc.rev20160730.ietfl3vpnsvc.siterouting.RoutingProtocols;
import org.onosproject.yang.gen.v1.ietfl3vpnsvc.rev20160730.ietfl3vpnsvc.siterouting.routingprotocols.RoutingProtocol;
import org.onosproject.yang.gen.v1.ietfnetworkinstance.rev20160623.ietfnetworkinstance.devices.device.NetworkInstances;
import org.onosproject.yang.gen.v1.l3vpnsvcext.rev20160730.l3vpnsvcext.l3vpnsvc.sites.site.sitenetworkaccesses.sitenetworkaccess.bearer.DefaultAugmentedL3VpnBearer;
import org.onosproject.yang.gen.v1.l3vpnsvcext.rev20160730.l3vpnsvcext.l3vpnsvc.sites.site.sitenetworkaccesses.sitenetworkaccess.bearer.requestedtype.DefaultAugmentedL3VpnRequestedType;
import org.onosproject.yang.gen.v1.l3vpnsvcext.rev20160730.l3vpnsvcext.requestedtypegrouping.requestedtypeprofile.RequestedTypeChoice;
import org.onosproject.yang.gen.v1.l3vpnsvcext.rev20160730.l3vpnsvcext.requestedtypegrouping.requestedtypeprofile.requestedtypechoice.DefaultDot1Qcase;
import org.onosproject.yang.gen.v1.l3vpnsvcext.rev20160730.l3vpnsvcext.requestedtypegrouping.requestedtypeprofile.requestedtypechoice.DefaultPhysicalCase;
import org.onosproject.yang.model.DataNode;
import org.onosproject.yang.model.DefaultModelObjectData;
import org.onosproject.yang.model.ModelConverter;
import org.onosproject.yang.model.ModelObject;
import org.onosproject.yang.model.ModelObjectData;
import org.onosproject.yang.model.ModelObjectId;
import org.onosproject.yang.model.NodeKey;
import org.onosproject.yang.model.ResourceData;
import org.onosproject.yang.model.ResourceId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Timer;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onosproject.config.DynamicConfigEvent.Type.NODE_ADDED;
import static org.onosproject.config.DynamicConfigEvent.Type.NODE_DELETED;
import static org.onosproject.l3vpn.netl3vpn.VpnType.HUB;
import static org.onosproject.l3vpn.netl3vpn.impl.BgpConstructionUtil.createBgpInfo;
import static org.onosproject.l3vpn.netl3vpn.impl.InsConstructionUtil.createInstance;
import static org.onosproject.l3vpn.netl3vpn.impl.InsConstructionUtil.deleteInstance;
import static org.onosproject.l3vpn.netl3vpn.impl.IntConstructionUtil.createInterface;
import static org.onosproject.l3vpn.netl3vpn.impl.NetL3VpnUtil.BEARER_NULL;
import static org.onosproject.l3vpn.netl3vpn.impl.NetL3VpnUtil.CONS_HUNDRED;
import static org.onosproject.l3vpn.netl3vpn.impl.NetL3VpnUtil.DEVICE_INFO_NULL;
import static org.onosproject.l3vpn.netl3vpn.impl.NetL3VpnUtil.EVENT_NULL;
import static org.onosproject.l3vpn.netl3vpn.impl.NetL3VpnUtil.ID_LIMIT;
import static org.onosproject.l3vpn.netl3vpn.impl.NetL3VpnUtil.ID_LIMIT_EXCEEDED;
import static org.onosproject.l3vpn.netl3vpn.impl.NetL3VpnUtil.INT_INFO_NULL;
import static org.onosproject.l3vpn.netl3vpn.impl.NetL3VpnUtil.IP;
import static org.onosproject.l3vpn.netl3vpn.impl.NetL3VpnUtil.IP_INT_INFO_NULL;
import static org.onosproject.l3vpn.netl3vpn.impl.NetL3VpnUtil.MAX_BATCH_MS;
import static org.onosproject.l3vpn.netl3vpn.impl.NetL3VpnUtil.MAX_EVENTS;
import static org.onosproject.l3vpn.netl3vpn.impl.NetL3VpnUtil.MAX_IDLE_MS;
import static org.onosproject.l3vpn.netl3vpn.impl.NetL3VpnUtil.PORT_NAME;
import static org.onosproject.l3vpn.netl3vpn.impl.NetL3VpnUtil.SITE_ROLE_NULL;
import static org.onosproject.l3vpn.netl3vpn.impl.NetL3VpnUtil.SITE_VPN_MISMATCH;
import static org.onosproject.l3vpn.netl3vpn.impl.NetL3VpnUtil.TIMER;
import static org.onosproject.l3vpn.netl3vpn.impl.NetL3VpnUtil.UNKNOWN_EVENT;
import static org.onosproject.l3vpn.netl3vpn.impl.NetL3VpnUtil.VPN_ATTACHMENT_NULL;
import static org.onosproject.l3vpn.netl3vpn.impl.NetL3VpnUtil.VPN_POLICY_NOT_SUPPORTED;
import static org.onosproject.l3vpn.netl3vpn.impl.NetL3VpnUtil.VPN_TYPE_UNSUPPORTED;
import static org.onosproject.l3vpn.netl3vpn.impl.NetL3VpnUtil.getBgpCreateConfigObj;
import static org.onosproject.l3vpn.netl3vpn.impl.NetL3VpnUtil.getIntCreateModObj;
import static org.onosproject.l3vpn.netl3vpn.impl.NetL3VpnUtil.getIntNotAvailable;
import static org.onosproject.l3vpn.netl3vpn.impl.NetL3VpnUtil.getMgmtIpUnAvailErr;
import static org.onosproject.l3vpn.netl3vpn.impl.NetL3VpnUtil.getModIdForL3VpnSvc;
import static org.onosproject.l3vpn.netl3vpn.impl.NetL3VpnUtil.getModIdForSites;
import static org.onosproject.l3vpn.netl3vpn.impl.NetL3VpnUtil.getResourceData;
import static org.onosproject.l3vpn.netl3vpn.impl.NetL3VpnUtil.getRole;
import static org.onosproject.l3vpn.netl3vpn.impl.NetL3VpnUtil.getVpnBgpDelModObj;
import static org.onosproject.l3vpn.netl3vpn.impl.NetL3VpnUtil.getVpnCreateModObj;
import static org.onosproject.l3vpn.netl3vpn.impl.NetL3VpnUtil.getVpnDelModObj;

/**
 * The IETF net l3vpn manager implementation.
 */
@Component(immediate = true)
public class NetL3VpnManager {

    private static final String APP_ID = "org.onosproject.app.l3vpn";
    private static final String L3_VPN_ID_TOPIC = "l3vpn-id";

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final DynamicConfigListener configListener =
            new InternalConfigListener();

    private final Accumulator<DynamicConfigEvent> accumulator =
            new InternalEventAccumulator();

    private final InternalLeadershipListener leadershipEventListener =
            new InternalLeadershipListener();

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DriverService driverService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ModelConverter modelConverter;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DynamicConfigService configService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected NetL3VpnStore l3VpnStore;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected LeadershipService leadershipService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ClusterService clusterService;

    protected IdGenerator l3VpnIdGen;

    private NodeId localNodeId;

    private ApplicationId appId;

    private ResourceId id;

    private ResourceId module;

    private ResourceId sites;

    private boolean isElectedLeader;

    @Activate
    protected void activate() {
        appId = coreService.registerApplication(APP_ID);
        l3VpnIdGen = coreService.getIdGenerator(L3_VPN_ID_TOPIC);
        localNodeId = clusterService.getLocalNode().id();
        leadershipService.addListener(leadershipEventListener);
        leadershipService.runForLeadership(appId.name());
        getResourceId();
        configService.addListener(configListener);
        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        configService.removeListener(configListener);
        leadershipService.withdraw(appId.name());
        leadershipService.removeListener(leadershipEventListener);
        log.info("Stopped");
    }

    /**
     * Returns id as string. If the id is not in the freed list a new id is
     * generated else the id from the freed list is used.
     *
     * @return id
     */
    private String getIdFromGen() {
        Long value;
        Iterable<Long> freeIds = l3VpnStore.getFreedIdList();
        Iterator<Long> it = freeIds.iterator();
        if (it.hasNext()) {
            value = it.next();
            l3VpnStore.removeIdFromFreeList(value);
        } else {
            value = l3VpnIdGen.getNewId();
        }
        if (value > ID_LIMIT) {
            throw new RuntimeException(ID_LIMIT_EXCEEDED);
        }
        return CONS_HUNDRED + String.valueOf(value);
    }

    /**
     * Returns the resource id, after constructing model object id and
     * converting it.
     */
    private void getResourceId() {

        ModelObjectId moduleId = ModelObjectId.builder().build();
        module = getResourceVal(moduleId);

        ModelObjectId svcId = getModIdForL3VpnSvc();
        id = getResourceVal(svcId);

        ModelObjectId sitesId = getModIdForSites();
        sites = getResourceVal(sitesId);
    }

    /**
     * Returns resource id from model converter.
     *
     * @param modelId model object id
     * @return resource id
     */
    private ResourceId getResourceVal(ModelObjectId modelId) {
        DefaultModelObjectData.Builder data = DefaultModelObjectData.builder()
                .identifier(modelId);
        ResourceData resData = modelConverter.createDataNode(data.build());
        return resData.resourceId();
    }

    /**
     * Processes create request from the store, by taking the root object.
     * The root object is then used for l3VPN processing.
     *
     * @param storeId store resource id
     * @param node    data node
     */
    private void processCreateFromStore(ResourceId storeId, DataNode node) {
        if (isElectedLeader) {
            List<NodeKey> keys = storeId.nodeKeys();
            List<ModelObject> objects = null;
            if (keys.size() == 1) {
                objects = getModelObjects(node, module);
            } else if (keys.size() == 2) {
                objects = getModelObjects(node, id);
            }
            if (objects != null) {
                for (ModelObject obj : objects) {
                    if (obj instanceof DefaultL3VpnSvc) {
                        DefaultL3VpnSvc l3VpnSvc = (DefaultL3VpnSvc) obj;
                        createGlobalConfig(l3VpnSvc);
                    } else if (obj instanceof DefaultSites) {
                        DefaultSites sites = (DefaultSites) obj;
                        createInterfaceConfig(sites);
                    }
                }
            }
        }
    }

    /**
     * Processes delete request from the store, by taking the root object.
     * The root object would have got deleted from store. So all the
     * configurations are removed.
     *
     * @param dataNode data node
     */
    private void processDeleteFromStore(DataNode dataNode) {
        if (isElectedLeader) {
            if (dataNode == null) {
                //TODO: Delete for inner nodes.
                deleteGlobalConfig(null);
            }
        }
    }

    /**
     * Returns model objects of the store. The data node read from store
     * gives the particular node. So the node's parent resource id is taken
     * and the data node is given to model converter.
     *
     * @param dataNode data node from store
     * @param appId    parent resource id
     * @return model objects
     */
    public List<ModelObject> getModelObjects(DataNode dataNode,
                                             ResourceId appId) {
        ResourceData data = getResourceData(dataNode, appId);
        ModelObjectData modelData = modelConverter.createModel(data);
        return modelData.modelObjects();
    }

    /**
     * Returns true if the event resource id points to the root level node
     * only and event is for addition and deletion; false otherwise.
     *
     * @param event config event
     * @return true if event is supported; false otherwise
     */
    public boolean isSupported(DynamicConfigEvent event) {
        ResourceId rsId = event.subject();
        List<NodeKey> storeKeys = rsId.nodeKeys();
        List<NodeKey> regKeys = id.nodeKeys();
        List<NodeKey> sitesKeys = sites.nodeKeys();
        if (storeKeys != null) {
            int storeSize = storeKeys.size();
            if (storeSize == 1) {
                return storeKeys.get(0).equals(regKeys.get(1)) &&
                        (event.type() == NODE_ADDED ||
                                event.type() == NODE_DELETED);
            } else if (storeSize == 2) {
                return (storeKeys.get(0).equals(sitesKeys.get(1))) &&
                        storeKeys.get(1).equals(sitesKeys.get(2)) &&
                        (event.type() == NODE_ADDED ||
                                event.type() == NODE_DELETED);
            }
        }
        return false;
    }

    /***
     * Creates all configuration in the standard device model.
     *
     * @param l3VpnSvc l3VPN service object
     */
    void createGlobalConfig(L3VpnSvc l3VpnSvc) {
        if (l3VpnSvc.vpnServices() != null) {
            createVpnServices(l3VpnSvc.vpnServices());
        }
        if (l3VpnSvc.sites() != null) {
            createInterfaceConfig(l3VpnSvc.sites());
        }
    }

    /**
     * Creates the VPN instances from the VPN services object, if only that
     * VPN instance is not already created.
     *
     * @param vpnSvcs VPN services object
     */
    private void createVpnServices(VpnServices vpnSvcs) {
        if (vpnSvcs != null && vpnSvcs.vpnSvc() != null) {
            List<VpnSvc> svcList = vpnSvcs.vpnSvc();
            for (VpnSvc svc : svcList) {
                String vpnName = svc.vpnId().string();
                l3VpnStore.addVpnInsIfAbsent(vpnName, new VpnInstance(vpnName));
            }
        }
    }

    /**
     * Creates interface configuration from the site network access if
     * available.
     *
     * @param sites sites object
     */
    private void createInterfaceConfig(Sites sites) {
        if (sites.site() != null) {
            List<Site> sitesList = sites.site();
            for (Site site : sitesList) {
                if (site.siteNetworkAccesses() != null) {
                    SiteNetworkAccesses accesses = site.siteNetworkAccesses();
                    List<SiteNetworkAccess> accessList =
                            accesses.siteNetworkAccess();
                    for (SiteNetworkAccess access : accessList) {
                        createFromAccess(access, site.siteId().string());
                    }
                }
            }
        }
    }

    /**
     * Creates the interface and VPN related configurations from the access
     * and site id value.
     *
     * @param access site network access
     * @param siteId site id
     */
    private void createFromAccess(SiteNetworkAccess access, String siteId) {
        Map<AccessInfo, InterfaceInfo> intMap = l3VpnStore.getInterfaceInfo();
        Map<String, VpnInstance> insMap = l3VpnStore.getVpnInstances();
        String accessId = access.siteNetworkAccessId().string();
        AccessInfo info = new AccessInfo(siteId, accessId);

        if (intMap.get(info) == null) {
            VpnSiteRole siteRole = getSiteRole(access.vpnAttachment());
            VpnInstance instance = insMap.get(siteRole.name());
            if (instance == null) {
                throw new NetL3VpnException(SITE_VPN_MISMATCH);
            }
            buildFromAccess(instance, info, access, siteRole);
        }
    }

    /**
     * Returns the VPN site role from the VPN attachment.
     *
     * @param attach VPN attachment
     * @return VPN site role
     */
    private VpnSiteRole getSiteRole(VpnAttachment attach) {
        if (attach == null || attach.attachmentFlavor() == null) {
            throw new NetL3VpnException(VPN_ATTACHMENT_NULL);
        }
        AttachmentFlavor flavor = attach.attachmentFlavor();
        if (!(flavor instanceof DefaultVpnId)) {
            throw new NetL3VpnException(VPN_POLICY_NOT_SUPPORTED);
        }
        DefaultVpnId vpnId = (DefaultVpnId) flavor;
        if (vpnId.siteRole() == null) {
            throw new NetL3VpnException(SITE_ROLE_NULL);
        }
        VpnType role = getRole(vpnId.siteRole());
        return new VpnSiteRole(String.valueOf(vpnId.vpnId()), role);
    }

    /**
     * Builds the required details for device standard model from the site
     * network access info available.
     *
     * @param instance VPN instance
     * @param info     access info
     * @param access   network access
     * @param role     VPN site role
     */
    private void buildFromAccess(VpnInstance instance, AccessInfo info,
                                 SiteNetworkAccess access, VpnSiteRole role) {
        Bearer bearer = access.bearer();
        if (bearer == null) {
            throw new NetL3VpnException(BEARER_NULL);
        }

        RequestedType reqType = bearer.requestedType();
        IpConnection connect = access.ipConnection();
        RoutingProtocols pro = access.routingProtocols();

        if (reqType == null || connect == null) {
            throw new NetL3VpnException(IP_INT_INFO_NULL);
        }
        buildDeviceDetails(instance, info, role, bearer, connect,
                           reqType, pro);
    }

    /**
     * Builds the device details such as, VPN instance value if it is for
     * the first time, interface values and BGP info if available in service.
     *
     * @param instance VPN instance
     * @param accInfo  access info
     * @param role     VPN site role
     * @param bearer   bearer object
     * @param connect  ip connect object
     * @param reqType  requested type
     * @param pro      routing protocol
     */
    private void buildDeviceDetails(VpnInstance instance, AccessInfo accInfo,
                                    VpnSiteRole role, Bearer bearer,
                                    IpConnection connect, RequestedType reqType,
                                    RoutingProtocols pro) {
        Map<AccessInfo, InterfaceInfo> interMap = l3VpnStore.getInterfaceInfo();
        InterfaceInfo intInfo = interMap.get(accInfo);
        if (intInfo != null) {
            return;
        }

        DeviceInfo info = buildDevVpnIns(bearer, instance, role, connect);
        String portName = getInterfaceName(info, reqType);
        buildDevVpnInt(info, instance, connect, portName, accInfo);

        if (pro != null && pro.routingProtocol() != null) {
            buildBgpInfo(pro.routingProtocol(), info,
                         role.name(), connect, accInfo);
        }
        InterfaceInfo interInfo = new InterfaceInfo(info, portName,
                                                    instance.vpnName());
        l3VpnStore.addInterfaceInfo(accInfo, interInfo);
        l3VpnStore.addVpnIns(instance.vpnName(), instance);
    }

    /**
     * Builds device VPN instance with the service objects. It returns
     *
     * @param bearer  bearer object
     * @param ins     VPN instance
     * @param role    VPN site role
     * @param connect ip connection
     * @return return
     */
    private DeviceInfo buildDevVpnIns(Bearer bearer, VpnInstance ins,
                                      VpnSiteRole role, IpConnection connect) {
        DefaultAugmentedL3VpnBearer augBearer = ((DefaultBearer) bearer)
                .augmentation(DefaultAugmentedL3VpnBearer.class);
        DeviceId id = getDeviceId(augBearer);
        Map<DeviceId, DeviceInfo> devices = ins.devInfo();
        DeviceInfo info = null;
        if (devices != null) {
            info = devices.get(id);
        }
        if (info == null) {
            info = createVpnInstance(id, role, ins, connect);
        }
        return info;
    }

    /**
     * Returns the device id from the bearer augment attachment of service.
     * If the attachment in augment is not available it throws error.
     *
     * @param attach augmented bearer
     * @return device id
     */
    private DeviceId getDeviceId(DefaultAugmentedL3VpnBearer attach) {
        if (attach == null || attach.bearerAttachment() == null ||
                attach.bearerAttachment().peMgmtIp() == null ||
                attach.bearerAttachment().peMgmtIp().string() == null) {
            throw new NetL3VpnException(DEVICE_INFO_NULL);
        }
        String ip = attach.bearerAttachment().peMgmtIp().string();
        return getId(ip);
    }

    /**
     * Returns the device id whose management ip address matches with the ip
     * received.
     *
     * @param ip ip address
     * @return device id
     */
    public DeviceId getId(String ip) {
        for (Device device : deviceService.getAvailableDevices()) {
            String val = device.annotations().value(IP);
            if (ip.equals(val)) {
                return device.id();
            }
        }
        throw new NetL3VpnException(getMgmtIpUnAvailErr(ip));
    }

    /**
     * Creates the VPN instance by constructing standard device model of
     * instances. It adds the RD and RT values to the VPN instance.
     *
     * @param id   device id
     * @param role VPN site role
     * @param inst VPN instance
     * @param ip   ip connection
     * @return device info
     */
    private DeviceInfo createVpnInstance(DeviceId id, VpnSiteRole role,
                                         VpnInstance inst, IpConnection ip) {
        Map<AccessInfo, InterfaceInfo> intMap = l3VpnStore.getInterfaceInfo();
        generateRdRt(inst, role);
        DeviceInfo info = new DeviceInfo(id);

        NetworkInstances instances = createInstance(inst, role, ip);
        ModelObjectData devMod = getVpnCreateModObj(intMap, instances,
                                                    id.toString());
        ModelObjectData driMod = info.processCreateInstance(driverService,
                                                            devMod);
        ResourceData resData = modelConverter.createDataNode(driMod);
        addToStore(resData);
        l3VpnStore.addVpnIns(inst.vpnName(), inst);
        inst.addDevInfo(id, info);
        return info;
    }

    /**
     * Adds the resource data that is received from the driver, after
     * converting from the model object data.
     *
     * @param resData resource data
     */
    private void addToStore(ResourceData resData) {
        if (resData != null && resData.dataNodes() != null) {
            List<DataNode> dataNodes = resData.dataNodes();
            for (DataNode node : dataNodes) {
                configService.createNodeRecursive(resData.resourceId(), node);
            }
        }
    }

    /**
     * Generates RD and RT value for the VPN instance for the first time VPN
     * instance creation.
     *
     * @param ins  VPN instance
     * @param role VPN site role
     */
    private void generateRdRt(VpnInstance ins, VpnSiteRole role) {
        ins.type(role.role());
        VpnConfig config = ins.vpnConfig();
        String rd = null;
        if (config == null) {
            rd = getIdFromGen();
        }
        switch (ins.type()) {
            case ANY_TO_ANY:
                if (config == null) {
                    config = new FullMeshVpnConfig(rd);
                    config.rd(rd);
                }
                break;

            case HUB:
            case SPOKE:
                if (config == null) {
                    config = new HubSpokeVpnConfig();
                    config.rd(rd);
                }
                createImpRtVal((HubSpokeVpnConfig) config, ins.type());
                createExpRtVal((HubSpokeVpnConfig) config, ins.type());
                break;

            default:
                throw new NetL3VpnException(VPN_TYPE_UNSUPPORTED);
        }
        ins.vpnConfig(config);
    }

    /**
     * Creates import RT value for HUB and SPOKE, according to the type, if
     * the values are not present.
     *
     * @param config VPN config
     * @param type   VPN type
     */
    private void createImpRtVal(HubSpokeVpnConfig config, VpnType type) {
        if (type == HUB) {
            if (config.hubImpRt() != null) {
                return;
            }
            setHubImpRt(config);
        } else {
            if (config.spokeImpRt() != null) {
                return;
            }
            config.spokeImpRt(config.rd());
        }
    }

    /**
     * Sets the HUB import RT, from the spoke export RT. If it is not
     * available a new ID is generated.
     *
     * @param config VPN config
     */
    public void setHubImpRt(HubSpokeVpnConfig config) {
        String hubImp;
        if (config.spokeExpRt() != null) {
            hubImp = config.spokeExpRt();
        } else {
            hubImp = getIdFromGen();
        }
        config.hubImpRt(hubImp);
    }

    /**
     * Creates export RT value for HUB and SPOKE, according to the type, if
     * the values are not present.
     *
     * @param config VPN config
     * @param type   VPN type
     */
    private void createExpRtVal(HubSpokeVpnConfig config, VpnType type) {
        if (type == HUB) {
            if (config.hubExpRt() != null) {
                return;
            }
            config.hubExpRt(config.rd());
        } else {
            if (config.spokeExpRt() != null) {
                return;
            }
            setSpokeExpRt(config);
        }
    }

    /**
     * Sets the SPOKE export RT, from the hub import RT. If it is not
     * available a new ID is generated.
     *
     * @param config VPN config
     */
    public void setSpokeExpRt(HubSpokeVpnConfig config) {
        String spokeExp;
        if (config.hubImpRt() != null) {
            spokeExp = config.hubImpRt();
        } else {
            spokeExp = getIdFromGen();
        }
        config.spokeExpRt(spokeExp);
    }

    /**
     * Returns the interface name from the requested type service object.
     *
     * @param info    device info
     * @param reqType requested type
     * @return interface name
     */
    private String getInterfaceName(DeviceInfo info, RequestedType reqType) {
        DefaultAugmentedL3VpnRequestedType req =
                ((DefaultRequestedType) reqType).augmentation(
                        DefaultAugmentedL3VpnRequestedType.class);
        if (req == null || req.requestedTypeProfile() == null ||
                req.requestedTypeProfile().requestedTypeChoice() == null) {
            throw new NetL3VpnException(INT_INFO_NULL);
        }
        RequestedTypeChoice reqChoice = req.requestedTypeProfile()
                .requestedTypeChoice();
        return getNameFromChoice(reqChoice, info.deviceId());
    }

    /**
     * Returns the interface name from the type choice provided.
     *
     * @param choice service choice
     * @param id     device id
     * @return interface name
     */
    private String getNameFromChoice(RequestedTypeChoice choice, DeviceId id) {
        if (choice == null) {
            throw new NetL3VpnException(INT_INFO_NULL);
        }
        String intName;
        if (choice instanceof DefaultDot1Qcase) {
            if (((DefaultDot1Qcase) choice).dot1q() == null ||
                    ((DefaultDot1Qcase) choice).dot1q()
                            .physicalIf() == null) {
                throw new NetL3VpnException(INT_INFO_NULL);
            }
            intName = ((DefaultDot1Qcase) choice).dot1q().physicalIf();
        } else {
            if (((DefaultPhysicalCase) choice).physical() == null ||
                    ((DefaultPhysicalCase) choice).physical()
                            .physicalIf() == null) {
                throw new NetL3VpnException(INT_INFO_NULL);
            }
            intName = ((DefaultPhysicalCase) choice).physical().physicalIf();
        }
        return getPortName(intName, id);
    }

    /**
     * Returns the port name when it the port is available in the device.
     *
     * @param intName interface name
     * @param id      device id
     * @return port name
     */
    private String getPortName(String intName, DeviceId id) {
        List<Port> ports = deviceService.getPorts(id);
        for (Port port : ports) {
            String pName = port.annotations().value(PORT_NAME);
            if (pName.equals(intName)) {
                return intName;
            }
        }
        throw new NetL3VpnException(getIntNotAvailable(intName));
    }

    /**
     * Builds the interface for the device binding with the VPN instance.
     *
     * @param info    device info
     * @param ins     VPN instance
     * @param connect IP connection
     * @param pName   port name
     * @param access  access info
     */
    private void buildDevVpnInt(DeviceInfo info, VpnInstance ins,
                                IpConnection connect, String pName,
                                AccessInfo access) {
        Map<AccessInfo, InterfaceInfo> intMap = l3VpnStore.getInterfaceInfo();
        info.addAccessInfo(access);
        info.addIfName(pName);
        Interfaces interfaces = createInterface(pName, ins.vpnName(),
                                                connect);
        ModelObjectData devMod = getIntCreateModObj(
                info.ifNames(), interfaces, info.deviceId().toString());
        ModelObjectData driMod = info.processCreateInterface(driverService,
                                                             devMod);
        ResourceData resData = modelConverter.createDataNode(driMod);
        addToStore(resData);
    }

    /**
     * Builds the BGP information from the routes that are given from the
     * service.
     *
     * @param routes  routing protocol
     * @param info    device info
     * @param name    VPN name
     * @param connect IP connection
     * @param access  access info
     */
    private void buildBgpInfo(List<RoutingProtocol> routes, DeviceInfo info,
                              String name, IpConnection connect,
                              AccessInfo access) {
        Map<BgpInfo, DeviceId> bgpMap = l3VpnStore.getBgpInfo();
        BgpInfo intBgp = createBgpInfo(routes, info, name, connect, access);
        if (intBgp != null) {
            intBgp.vpnName(name);
            BgpDriverInfo config = getBgpCreateConfigObj(
                    bgpMap, info.deviceId().toString(), info.bgpInfo(), intBgp);
            ModelObjectData driData = info.processCreateBgpInfo(
                    driverService, intBgp, config);
            l3VpnStore.addBgpInfo(info.bgpInfo(), info.deviceId());
            ResourceData resData = modelConverter.createDataNode(driData);
            addToStore(resData);
        }
    }

    /**
     * Creates all configuration in the standard device model.
     *
     * @param l3VpnSvc l3 VPN service
     */
    void deleteGlobalConfig(L3VpnSvc l3VpnSvc) {
        deleteGlobalVpn(l3VpnSvc);
        //TODO: Site and access deletion needs to be added.
    }

    /**
     * Deletes the global VPN from the device model and delete from the device.
     *
     * @param l3VpnSvc L3 VPN service
     */
    private void deleteGlobalVpn(L3VpnSvc l3VpnSvc) {
        Map<String, VpnInstance> insMap = l3VpnStore.getVpnInstances();
        //TODO: check for VPN delete deleting interface from store.
        if (l3VpnSvc == null || l3VpnSvc.vpnServices() == null ||
                l3VpnSvc.vpnServices().vpnSvc() == null) {
            for (Map.Entry<String, VpnInstance> vpnMap : insMap.entrySet()) {
                deleteVpnInstance(vpnMap.getValue(), false);
            }
            return;
        }
        List<VpnSvc> vpnList = l3VpnSvc.vpnServices().vpnSvc();
        for (Map.Entry<String, VpnInstance> vpnMap : insMap.entrySet()) {
            boolean isPresent = isVpnPresent(vpnMap.getKey(), vpnList);
            if (!isPresent) {
                deleteVpnInstance(vpnMap.getValue(), false);
            }
        }
    }

    /**
     * Returns true if the VPN in the distributed map is also present in the
     * service; false otherwise.
     *
     * @param vpnName VPN name from store
     * @param vpnList VPN list from service
     * @return true if VPN available; false otherwise
     */
    private boolean isVpnPresent(String vpnName, List<VpnSvc> vpnList) {
        for (VpnSvc svc : vpnList) {
            if (svc.vpnId().string().equals(vpnName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Deletes the VPN instance by constructing standard device model of
     * instances.
     *
     * @param instance     VPN instance
     * @param isIntDeleted if interface already removed.
     */
    private void deleteVpnInstance(VpnInstance instance, boolean isIntDeleted) {
        Map<DeviceId, DeviceInfo> devices = instance.devInfo();
        if (devices != null) {
            for (Map.Entry<DeviceId, DeviceInfo> device : devices.entrySet()) {
                NetworkInstances ins = deleteInstance(instance.vpnName());
                DeviceInfo dev = device.getValue();
                if (!isIntDeleted) {
                    remVpnBgp(dev);
                    remInterfaceFromMap(dev);
                }
                Map<AccessInfo, InterfaceInfo> intMap =
                        l3VpnStore.getInterfaceInfo();
                String id = dev.deviceId().toString();
                ModelObjectData devMod = getVpnDelModObj(intMap, ins, id);
                ModelObjectData driMod = dev.processDeleteInstance(
                        driverService, devMod);
                ResourceData resData = modelConverter.createDataNode(driMod);
                deleteFromStore(resData);
            }
            l3VpnStore.removeVpnInstance(instance.vpnName());
        }
    }

    /**
     * Removes the BGP information for that complete VPN instance.
     *
     * @param dev device info
     */
    private void remVpnBgp(DeviceInfo dev) {
        BgpInfo devBgp = dev.bgpInfo();
        if (devBgp != null) {
            l3VpnStore.removeBgpInfo(devBgp);
            BgpInfo delInfo = new BgpInfo();
            delInfo.vpnName(devBgp.vpnName());
            String id = dev.deviceId().toString();
            Map<BgpInfo, DeviceId> bgpMap = l3VpnStore.getBgpInfo();
            BgpDriverInfo driConfig = getVpnBgpDelModObj(bgpMap, id);
            ModelObjectData driData = dev.processDeleteBgpInfo(
                    driverService, delInfo, driConfig);
            ResourceData resData = modelConverter.createDataNode(driData);
            deleteFromStore(resData);
            l3VpnStore.removeBgpInfo(devBgp);
        }
    }

    /**
     * Deletes the resource data that is received from the driver, after
     * converting from the model object data.
     *
     * @param resData resource data
     */
    private void deleteFromStore(ResourceData resData) {
        if (resData != null) {
            configService.deleteNodeRecursive(resData.resourceId());
        }
    }

    /**
     * Removes the interface from the app distributed map, if the driver
     * interfaces are already removed from the store.
     *
     * @param deviceInfo device info
     */
    private void remInterfaceFromMap(DeviceInfo deviceInfo) {
        List<AccessInfo> accesses = deviceInfo.accesses();
        if (accesses != null) {
            for (AccessInfo access : accesses) {
                l3VpnStore.removeInterfaceInfo(access);
            }
        }
        deviceInfo.ifNames(null);
        deviceInfo.accesses(null);
    }

    /**
     * Signals that the leadership has changed.
     *
     * @param isLeader true if this instance is now the leader, otherwise false
     */
    private void leaderChanged(boolean isLeader) {
        log.debug("Leader changed: {}", isLeader);
        isElectedLeader = isLeader;
    }

    /**
     * Representation of internal listener, listening for dynamic config event.
     */
    private class InternalConfigListener implements DynamicConfigListener {

        @Override
        public boolean isRelevant(DynamicConfigEvent event) {
            return isSupported(event);
        }

        @Override
        public void event(DynamicConfigEvent event) {
            accumulator.add(event);
        }
    }

    /**
     * Accumulates events to allow processing after a desired number of
     * events were accumulated.
     */
    private class InternalEventAccumulator extends
            AbstractAccumulator<DynamicConfigEvent> {

        /**
         * Constructs the event accumulator with timer and event limit.
         */
        protected InternalEventAccumulator() {
            super(new Timer(TIMER), MAX_EVENTS, MAX_BATCH_MS, MAX_IDLE_MS);
        }

        @Override
        public void processItems(List<DynamicConfigEvent> events) {
            for (DynamicConfigEvent event : events) {
                checkNotNull(event, EVENT_NULL);
                Filter filter = new Filter();
                DataNode node;
                try {
                    node = configService.readNode(event.subject(), filter);
                } catch (FailedException e) {
                    node = null;
                }
                switch (event.type()) {
                    case NODE_ADDED:
                        processCreateFromStore(event.subject(), node);
                        break;

                    case NODE_DELETED:
                        processDeleteFromStore(node);
                        break;

                    default:
                        log.warn(UNKNOWN_EVENT, event.type());
                        break;
                }
            }
        }
    }

    /**
     * A listener for leadership events.
     */
    private class InternalLeadershipListener implements LeadershipEventListener {

        @Override
        public boolean isRelevant(LeadershipEvent event) {
            return event.subject().topic().equals(appId.name());
        }

        @Override
        public void event(LeadershipEvent event) {
            switch (event.type()) {
                case LEADER_CHANGED:
                case LEADER_AND_CANDIDATES_CHANGED:
                    if (localNodeId.equals(event.subject().leaderNodeId())) {
                        log.info("Net l3vpn manager gained leadership");
                        leaderChanged(true);
                    } else {
                        log.info("Net l3vpn manager leader changed. New " +
                                         "leader is {}", event.subject()
                                         .leaderNodeId());
                        leaderChanged(false);
                    }
                default:
                    break;
            }
        }
    }
}
