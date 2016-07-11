package org.onosproject.netl3vpn.manager.impl;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.List;

import org.onosproject.netl3vpn.entity.WebAc;
import org.onosproject.netl3vpn.entity.WebL3vpnInstance;
import org.onosproject.netl3vpn.entity.WebL3vpnInstance.TopoModeType;
import org.onosproject.netl3vpn.util.ConvertUtil;
import org.onosproject.yang.gen.v1.net.l3vpn.rev20160701.netl3vpn.acgroup.acs.Ac;
import org.onosproject.yang.gen.v1.net.l3vpn.rev20160701.netl3vpn.instances.Instance;
import org.onosproject.yang.gen.v1.net.l3vpn.rev20160701.netl3vpn.instances.instance.nes.Ne;

public class NetL3vpnParse {
    private static final String INSTANCE_NOT_NULL = "Instance can not be null";

    private Instance instance;

    public NetL3vpnParse(Instance instance) {
        this.instance = instance;
    }

    public WebL3vpnInstance cfgParse() {
        checkNotNull(instance, INSTANCE_NOT_NULL);
        WebL3vpnInstance webL3vpnInstance = new WebL3vpnInstance();
        webL3vpnInstance.setId(instance.id());
        webL3vpnInstance.setName(instance.name());
        webL3vpnInstance.setMode(TopoModeType.valueOf("FullMesh"));
        List<String> neIdList = new ArrayList<String>();
        for (Ne ne : instance.nes().ne()) {
            neIdList.add(ne.id());
        }
        webL3vpnInstance.setNeIdList(neIdList);
        List<WebAc> webAcs = new ArrayList<WebAc>();
        for (Ac ac : instance.acs().ac()) {
            webAcs.add(ConvertUtil.convertToWebAc(ac));
        }
        webL3vpnInstance.setAcList(webAcs);
        return webL3vpnInstance;
    }
}
