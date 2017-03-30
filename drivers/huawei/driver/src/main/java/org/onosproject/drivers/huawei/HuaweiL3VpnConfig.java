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

package org.onosproject.drivers.huawei;

import org.onlab.osgi.ServiceNotFoundException;
import org.onosproject.config.DynamicConfigService;
import org.onosproject.config.FailedException;
import org.onosproject.l3vpn.netl3vpn.BgpDriverInfo;
import org.onosproject.l3vpn.netl3vpn.BgpInfo;
import org.onosproject.net.behaviour.L3VpnConfig;
import org.onosproject.net.driver.AbstractHandlerBehaviour;
import org.onosproject.yang.gen.v1.l3vpn.comm.type.rev20141225.NeL3VpncommType;
import org.onosproject.yang.gen.v1.ne.bgpcomm.rev20141225.NeBgpcomm;
import org.onosproject.yang.gen.v1.ne.bgpcomm.type.rev20141225.NeBgpcommType;
import org.onosproject.yang.gen.v1.ne.l3vpn.api.rev20141225.NeL3VpnApi;
import org.onosproject.yang.gen.v1.ne.l3vpn.comm.rev20141225.NeL3Vpncomm;
import org.onosproject.yang.model.DataNode;
import org.onosproject.yang.model.ModelObjectData;
import org.onosproject.yang.model.ResourceId;
import org.onosproject.yang.model.YangModel;
import org.onosproject.yang.model.YangModuleId;
import org.onosproject.yang.runtime.DefaultAppModuleInfo;
import org.onosproject.yang.runtime.ModelRegistrationParam;
import org.onosproject.yang.runtime.YangModelRegistry;

import java.util.Iterator;

import static org.onosproject.drivers.huawei.BgpConstructionUtil.getCreateBgp;
import static org.onosproject.drivers.huawei.BgpConstructionUtil.getDeleteBgp;
import static org.onosproject.drivers.huawei.DriverUtil.DEVICES;
import static org.onosproject.drivers.huawei.DriverUtil.NAMESPACE;
import static org.onosproject.drivers.huawei.DriverUtil.SERVICE_NOT_FOUND;
import static org.onosproject.drivers.huawei.DriverUtil.SLASH;
import static org.onosproject.drivers.huawei.InsConstructionUtil.getCreateVpnIns;
import static org.onosproject.drivers.huawei.InsConstructionUtil.getDeleteVpnIns;
import static org.onosproject.drivers.huawei.IntConstructionUtil.getCreateInt;
import static org.onosproject.yang.runtime.DefaultModelRegistrationParam.builder;
import static org.onosproject.yang.runtime.helperutils.YangApacheUtils.getYangModel;

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
            registerModel();
        } catch (ServiceNotFoundException e) {
            throw new ServiceNotFoundException(SERVICE_NOT_FOUND);
        }
    }

    /**
     * Registers the huawei generated classes to the YANG model.
     */
    private void registerModel() {
        YangModel model = getYangModel(NeBgpcomm.class);
        Iterator<YangModuleId> it = model.getYangModulesId().iterator();

        //Create model registration param.
        ModelRegistrationParam.Builder b = builder().setYangModel(model);
        YangModuleId id;
        while (it.hasNext()) {
            id = it.next();
            switch (id.moduleName()) {
                case "ne-bgpcomm":
                    b.addAppModuleInfo(id, new DefaultAppModuleInfo(
                            NeBgpcomm.class, null));
                    break;
                case "ne-bgpcomm-type":
                    b.addAppModuleInfo(id, new DefaultAppModuleInfo(
                            NeBgpcommType.class, null));
                    break;
                case "ne-l3vpn-api":
                    b.addAppModuleInfo(id, new DefaultAppModuleInfo(
                            NeL3VpnApi.class, null));
                    break;
                case "ne-l3vpncomm":
                    b.addAppModuleInfo(id, new DefaultAppModuleInfo(
                            NeL3Vpncomm.class, null));
                    break;
                case "ne-l3vpncomm-type":
                    b.addAppModuleInfo(id, new DefaultAppModuleInfo(
                            NeL3VpncommType.class, null));
                    break;
                default:
                    break;
            }
        }
        ModelRegistrationParam regParam = b.build();
        modelRegistry.registerModel(regParam);
    }

    @Override
    public Object createInstance(Object objectData) {
        if (modelRegistry == null) {
            init();
        }
        return getCreateVpnIns((ModelObjectData) objectData,
                               isDevicesPresent());
    }

    @Override
    public Object bindInterface(Object objectData) {
        return getCreateInt((ModelObjectData) objectData);
    }

    @Override
    public Object createBgpInfo(Object bgpInfo, Object bgpConfig) {
        return getCreateBgp((BgpInfo) bgpInfo, (BgpDriverInfo) bgpConfig);
    }


    @Override
    public Object deleteInstance(Object objectData) {
        return getDeleteVpnIns((ModelObjectData) objectData);
    }

    @Override
    public Object unbindInterface(Object objectData) {
        //TODO:To be committed.
        return null;
    }

    @Override
    public Object deleteBgpInfo(Object bgpInfo, Object bgpConfig) {
        return getDeleteBgp((BgpInfo) bgpInfo, (BgpDriverInfo) bgpConfig);
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
