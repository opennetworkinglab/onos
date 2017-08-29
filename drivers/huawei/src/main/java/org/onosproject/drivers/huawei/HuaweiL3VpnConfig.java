/*
 * Copyright 2017-present Open Networking Foundation
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

package org.onosproject.drivers.huawei;

import org.onlab.osgi.ServiceNotFoundException;
import org.onosproject.config.DynamicConfigService;
import org.onosproject.config.FailedException;
import org.onosproject.l3vpn.netl3vpn.BgpDriverInfo;
import org.onosproject.l3vpn.netl3vpn.BgpInfo;
import org.onosproject.l3vpn.netl3vpn.L3VpnConfig;
import org.onosproject.l3vpn.netl3vpn.TunnelInfo;
import org.onosproject.net.driver.AbstractHandlerBehaviour;
import org.onosproject.yang.model.DataNode;
import org.onosproject.yang.model.ModelObjectData;
import org.onosproject.yang.model.ResourceId;
import org.onosproject.yang.runtime.YangModelRegistry;

import static org.onosproject.drivers.huawei.BgpConstructionUtil.getCreateBgp;
import static org.onosproject.drivers.huawei.BgpConstructionUtil.getDeleteBgp;
import static org.onosproject.drivers.huawei.DriverUtil.DEVICES;
import static org.onosproject.drivers.huawei.DriverUtil.NAMESPACE;
import static org.onosproject.drivers.huawei.DriverUtil.SERVICE_NOT_FOUND;
import static org.onosproject.drivers.huawei.DriverUtil.SLASH;
import static org.onosproject.drivers.huawei.InsConstructionUtil.getCreateVpnIns;
import static org.onosproject.drivers.huawei.InsConstructionUtil.getDeleteVpnIns;
import static org.onosproject.drivers.huawei.IntConstructionUtil.getCreateInt;
import static org.onosproject.drivers.huawei.TnlConstructionUtil.getBindTnl;
import static org.onosproject.drivers.huawei.TnlConstructionUtil.getCreateTnl;
import static org.onosproject.drivers.huawei.TnlConstructionUtil.getCreateTnlDev;
import static org.onosproject.drivers.huawei.TnlConstructionUtil.getCreateTnlPol;
import static org.onosproject.drivers.huawei.TnlConstructionUtil.getDeleteTnl;

/**
 * Configures l3vpn on Huawei devices.
 */
public class HuaweiL3VpnConfig extends AbstractHandlerBehaviour
        implements L3VpnConfig {

    /**
     * YANG model registry.
     */
    protected YangModelRegistry modelRegistry;

    /**
     * Dynamic config service.
     */
    protected DynamicConfigService configService;

    /**
     * Constructs huawei L3VPN config.
     */
    public HuaweiL3VpnConfig() {
    }

    /**
     * Takes the YANG model registry service and registers the driver YANG.
     * If service is not available it throws exception.
     */
    private void init() {
        try {
            modelRegistry = handler().get(YangModelRegistry.class);
            configService = handler().get(DynamicConfigService.class);
        } catch (ServiceNotFoundException e) {
            throw new ServiceNotFoundException(SERVICE_NOT_FOUND);
        }
    }

    @Override
    public ModelObjectData createInstance(ModelObjectData objectData) {
        if (modelRegistry == null) {
            init();
        }
        return getCreateVpnIns(objectData, isDevicesPresent());
    }

    @Override
    public ModelObjectData bindInterface(ModelObjectData objectData) {
        return getCreateInt(objectData);
    }

    @Override
    public ModelObjectData createBgpInfo(BgpInfo bgpInfo,
                                         BgpDriverInfo bgpConfig) {
        return getCreateBgp(bgpInfo, bgpConfig);
    }

    @Override
    public ModelObjectData createTnlDev(TunnelInfo tnlInfo) {
        return getCreateTnlDev(tnlInfo);
    }

    @Override
    public ModelObjectData createTnlPol(TunnelInfo tnlInfo) {
        return getCreateTnlPol(tnlInfo);
    }

    @Override
    public ModelObjectData createTnl(TunnelInfo tnlInfo) {
        return getCreateTnl(tnlInfo);
    }

    @Override
    public ModelObjectData bindTnl(TunnelInfo tnlInfo) {
        return getBindTnl(tnlInfo);
    }

    @Override
    public ModelObjectData deleteInstance(ModelObjectData objectData) {
        return getDeleteVpnIns(objectData);
    }

    @Override
    public ModelObjectData unbindInterface(ModelObjectData objectData) {
        //TODO:To be committed.
        return null;
    }

    @Override
    public ModelObjectData deleteTnl(TunnelInfo tnlInfo) {
        return getDeleteTnl(tnlInfo);
    }

    @Override
    public ModelObjectData deleteBgpInfo(BgpInfo bgpInfo,
                                         BgpDriverInfo bgpConfig) {
        return getDeleteBgp(bgpInfo, bgpConfig);
    }

    /**
     * Returns true if devices, which is the root node present in store;
     * false otherwise.
     *
     * @return true if devices available; false otherwise
     */
    private boolean isDevicesPresent() {
        ResourceId resId = ResourceId.builder()
                .addBranchPointSchema(SLASH, null)
                .addBranchPointSchema(DEVICES, NAMESPACE).build();
        try {
            DataNode node = configService.readNode(resId, null);
            if (node != null) {
                return true;
            }
        } catch (FailedException e) {
            return false;
        }
        return false;
    }
}
