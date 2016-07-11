package org.onosproject.netl3vpn.manager.impl;

import java.util.Collection;
import java.util.Iterator;

import org.onosproject.incubator.net.resource.label.DefaultLabelResource;
import org.onosproject.incubator.net.resource.label.LabelResource;
import org.onosproject.incubator.net.resource.label.LabelResourceAdminService;
import org.onosproject.incubator.net.resource.label.LabelResourceId;
import org.onosproject.incubator.net.resource.label.LabelResourceService;
import org.onosproject.netl3vpn.entity.WebL3vpnInstance;
import org.onosproject.netl3vpn.store.NetL3vpnStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NetL3vpnLabelResource {
    private static final Logger log = LoggerFactory
            .getLogger(NetL3vpnLabelResource.class);
    private static final String RD_PREFIX = "100:";
    private static final String RT_PREFIX = "100:";
    private static final String VRF_PREFIX = "VRF_";
    private LabelResourceAdminService labelRsrcAdminService;
    private LabelResourceService labelRsrcService;
    private NetL3vpnStore netL3vpnStore;
    private WebL3vpnInstance webL3vpnInstance;

    public NetL3vpnLabelResource(LabelResourceAdminService labelRsrcAdminService,
                                 LabelResourceService labelRsrcService,
                                 NetL3vpnStore netL3vpnStore,
                                 WebL3vpnInstance webL3vpnInstance) {
        this.labelRsrcAdminService = labelRsrcAdminService;
        this.labelRsrcService = labelRsrcService;
        this.netL3vpnStore = netL3vpnStore;
        this.webL3vpnInstance = webL3vpnInstance;
    }

    public String allocateResource(String allocateType) {
        long applyNum = 1; // For each vpn only one rd label
        LabelResourceId specificLabelId = null;
        Collection<LabelResource> result = labelRsrcService
                .applyFromGlobalPool(applyNum);
        if (result.size() > 0) {
            // Only one element to retrieve
            Iterator<LabelResource> iterator = result.iterator();
            DefaultLabelResource defaultLabelResource = (DefaultLabelResource) iterator
                    .next();
            specificLabelId = defaultLabelResource.labelResourceId();
            if (specificLabelId == null) {
                log.error("Unable to retrieve {} label for a vpn id {}.",
                          allocateType, webL3vpnInstance.getId());
                return null;
            }
        } else {
            log.error("Unable to allocate {} label for a vpn id {}.",
                      allocateType, webL3vpnInstance.getId());
            return null;
        }

        switch (allocateType) {
        case "rd":
            return RD_PREFIX + specificLabelId.id();
        case "rt":
            return RT_PREFIX + specificLabelId.id();
        case "vrf":
            return VRF_PREFIX + specificLabelId.id();
        default:
            log.error("Unable to allocate {} label for a vpn id {}.",
                      allocateType, webL3vpnInstance.getId());
            return null;
        }
    }
}
