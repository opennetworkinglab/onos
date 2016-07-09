package org.onosproject.netl3vpn.manager.impl;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.incubator.net.resource.label.LabelResourceAdminService;
import org.onosproject.incubator.net.resource.label.LabelResourceService;
import org.onosproject.ne.NeData;
import org.onosproject.ne.manager.L3vpnNeService;
import org.onosproject.net.device.DeviceService;
import org.onosproject.netl3vpn.entity.L3VpnAllocateRes;
import org.onosproject.netl3vpn.entity.WebL3vpnInstance;
import org.onosproject.netl3vpn.manager.NetL3vpnService;
import org.onosproject.netl3vpn.store.NetL3vpnStore;
import org.onosproject.yang.gen.v1.net.l3vpn.rev20160701.netl3vpn.instances.Instance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides implementation of NetL3vpnService.
 */
@Component(immediate = true)
@Service
public class NetL3vpnManager implements NetL3vpnService {
    private static final String INSTANCE_NOT_NULL = "Instance can not be null";
    private static final String APP_ID = "org.onosproject.app.l3vpn.net";
    private static final String RT = "rt";
    private static final String RD = "rd";
    private static final String VRF = "vrf";
    protected static final Logger log = LoggerFactory
            .getLogger(NetL3vpnManager.class);

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;
    
    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected LabelResourceAdminService labelRsrcAdminService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected LabelResourceService labelRsrcService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected NetL3vpnStore netL3vpnStore;

    private ApplicationId appId;
    private WebL3vpnInstance webL3vpnInstance;
    private L3VpnAllocateRes l3VpnAllocateRes;
    private L3vpnNeService l3vpnNeService;

    @Activate
    public void activate() {
        appId = coreService.registerApplication(APP_ID);
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        log.info("Stopped");
    }

    @Override
    public boolean createL3vpn(Instance instance) {
        checkNotNull(instance, INSTANCE_NOT_NULL);
        // TODO check ne status
        webL3vpnInstance = new NetL3vpnParse(instance)
                .cfgParse();
        if (webL3vpnInstance == null) {
            log.debug("Parsing the web vpn instance failed,whose identifier is {} ", instance.id());
            return false;
        }
        if (netL3vpnStore.existsVpnId(webL3vpnInstance.getId())) {
            log.debug("The l3vpn instance is already exists, whose identifier is {} ",
                      webL3vpnInstance.getId());
            return false;
        }
        netL3vpnStore.addWebL3vpnInstance(webL3vpnInstance.getId(),
                                          webL3vpnInstance);
        if (!handleResource()) {
            log.debug("Handling the resource of l3vpn instance failed.");
            return false;
        }
        NeData neData = new NetL3vpnDecomp(webL3vpnInstance, l3VpnAllocateRes, deviceService).decompNeData();
        // store MD
        return l3vpnNeService.createL3vpn(neData);
    }
    
    public boolean handleResource() {
        if (!checkOccupiedResource()) {
            log.debug("The resource of l3vpn instance is occupied.");
            return false;
        }
        if (!applyResource()) {
            log.debug("Apply resources of l3vpn instance failed.");
            return false;
        }
        return true;
    }

    public boolean checkOccupiedResource() {
        if (netL3vpnStore.existsVpnName(webL3vpnInstance.getId(),
                                        webL3vpnInstance.getName())) {
            log.debug("The l3vpn instance's name {} is already exists.",
                      webL3vpnInstance.getName());
            return false;
        }
        return true;
    }

    public boolean applyResource() {
        NetL3vpnLabelResource netL3vpnLabelResource = new NetL3vpnLabelResource(labelRsrcAdminService,
                                                                                labelRsrcService,
                                                                                netL3vpnStore,
                                                                                webL3vpnInstance);
        String routeTarget = netL3vpnLabelResource.allocateResource(RT);
        String routeDistinguisher = netL3vpnLabelResource.allocateResource(RD);
        String vrfName = netL3vpnLabelResource.allocateResource(VRF);
        if (routeTarget == null || routeDistinguisher == null || vrfName == null) {
            log.debug("Allocate resource for l3 vpn instance failed, the instance id is {}",
                      webL3vpnInstance.getId());
            return false;
        }
        
        List<String> routeTargets = new ArrayList<String>();
        routeTargets.add(routeTarget);
        l3VpnAllocateRes.setRouteDistinguisher(routeDistinguisher);
        l3VpnAllocateRes.setRouteTargets(routeTargets);
        l3VpnAllocateRes.setVrfName(vrfName);
        return true;
    }
}
