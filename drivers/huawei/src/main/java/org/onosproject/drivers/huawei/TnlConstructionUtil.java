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

import org.onosproject.l3vpn.netl3vpn.TunnelInfo;
import org.onosproject.yang.gen.v1.nel3vpncommtype.rev20141225.nel3vpncommtype.Ipv4Address;
import org.onosproject.yang.gen.v1.nel3vpncommtype.rev20141225.nel3vpncommtype.L3VpncommonL3VpnPrefixType;
import org.onosproject.yang.gen.v1.nel3vpnapi.rev20141225.nel3vpnapi.devices.device.DefaultL3Vpn;
import org.onosproject.yang.gen.v1.nel3vpnapi.rev20141225.nel3vpnapi.devices.device.l3vpn.DefaultL3Vpncomm;
import org.onosproject.yang.gen.v1.nel3vpnapi.rev20141225.nel3vpnapi.devices.device.l3vpn.l3vpncomm.DefaultL3VpnInstances;
import org.onosproject.yang.gen.v1.nel3vpnapi.rev20141225.nel3vpnapi.devices.device.l3vpn.l3vpncomm.l3vpninstances.DefaultL3VpnInstance;
import org.onosproject.yang.gen.v1.nel3vpnapi.rev20141225.nel3vpnapi.devices.device.l3vpn.l3vpncomm.l3vpninstances.L3VpnInstanceKeys;
import org.onosproject.yang.gen.v1.nel3vpnapi.rev20141225.nel3vpnapi.devices.device.l3vpn.l3vpncomm.l3vpninstances.l3vpninstance.DefaultVpnInstAfs;
import org.onosproject.yang.gen.v1.nel3vpnapi.rev20141225.nel3vpnapi.devices.device.l3vpn.l3vpncomm.l3vpninstances.l3vpninstance.vpninstafs.DefaultVpnInstAf;
import org.onosproject.yang.gen.v1.nel3vpnapi.rev20141225.nel3vpnapi.devices.device.l3vpn.l3vpncomm.l3vpninstances.l3vpninstance.vpninstafs.VpnInstAfKeys;
import org.onosproject.yang.gen.v1.netnlm.rev20141225.netnlm.DefaultDevices;
import org.onosproject.yang.gen.v1.netnlm.rev20141225.netnlm.Devices;
import org.onosproject.yang.gen.v1.netnlm.rev20141225.netnlm.devices.DefaultDevice;
import org.onosproject.yang.gen.v1.netnlm.rev20141225.netnlm.devices.Device;
import org.onosproject.yang.gen.v1.netnlm.rev20141225.netnlm.devices.DeviceKeys;
import org.onosproject.yang.gen.v1.netnlm.rev20141225.netnlm.devices.device.DefaultTnlm;
import org.onosproject.yang.gen.v1.netnlm.rev20141225.netnlm.devices.device.Tnlm;
import org.onosproject.yang.gen.v1.netnlm.rev20141225.netnlm.devices.device.tnlm.DefaultTunnelPolicys;
import org.onosproject.yang.gen.v1.netnlm.rev20141225.netnlm.devices.device.tnlm.TunnelPolicys;
import org.onosproject.yang.gen.v1.netnlm.rev20141225.netnlm.devices.device.tnlm.tunnelpolicys.DefaultTunnelPolicy;
import org.onosproject.yang.gen.v1.netnlm.rev20141225.netnlm.devices.device.tnlm.tunnelpolicys.TunnelPolicy;
import org.onosproject.yang.gen.v1.netnlm.rev20141225.netnlm.devices.device.tnlm.tunnelpolicys.TunnelPolicyKeys;
import org.onosproject.yang.gen.v1.netnlm.rev20141225.netnlm.devices.device.tnlm.tunnelpolicys.tunnelpolicy.DefaultTpNexthops;
import org.onosproject.yang.gen.v1.netnlm.rev20141225.netnlm.devices.device.tnlm.tunnelpolicys.tunnelpolicy.TpNexthops;
import org.onosproject.yang.gen.v1.netnlm.rev20141225.netnlm.devices.device.tnlm.tunnelpolicys.tunnelpolicy.tpnexthops.DefaultTpNexthop;
import org.onosproject.yang.gen.v1.netnlm.rev20141225.netnlm.devices.device.tnlm.tunnelpolicys.tunnelpolicy.tpnexthops.TpNexthop;
import org.onosproject.yang.gen.v1.netnlm.rev20141225.netnlm.devices.device.tnlm.tunnelpolicys.tunnelpolicy.tpnexthops.tpnexthop.DefaultTpTunnels;
import org.onosproject.yang.gen.v1.netnlm.rev20141225.netnlm.devices.device.tnlm.tunnelpolicys.tunnelpolicy.tpnexthops.tpnexthop.TpTunnels;
import org.onosproject.yang.gen.v1.netnlm.rev20141225.netnlm.devices.device.tnlm.tunnelpolicys.tunnelpolicy.tpnexthops.tpnexthop.tptunnels.DefaultTpTunnel;
import org.onosproject.yang.gen.v1.netnlm.rev20141225.netnlm.devices.device.tnlm.tunnelpolicys.tunnelpolicy.tpnexthops.tpnexthop.tptunnels.TpTunnel;
import org.onosproject.yang.gen.v1.netnlmtype.rev20141225.netnlmtype.tnlmbasetnlpolicytype.TnlmbaseTnlPolicyTypeEnum;
import org.onosproject.yang.model.DefaultModelObjectData;
import org.onosproject.yang.model.InnerModelObject;
import org.onosproject.yang.model.LeafModelObject;
import org.onosproject.yang.model.ModelObjectData;
import org.onosproject.yang.model.ModelObjectId.Builder;

import static org.onosproject.drivers.huawei.DriverUtil.UNSUPPORTED_MODEL_LVL;
import static org.onosproject.drivers.huawei.DriverUtil.getData;
import static org.onosproject.drivers.huawei.InsConstructionUtil.getModObjIdDriDevice;
import static org.onosproject.l3vpn.netl3vpn.ModelIdLevel.TP_HOP;
import static org.onosproject.yang.gen.v1.nel3vpncommtype.rev20141225.nel3vpncommtype.l3vpncommonl3vpnprefixtype.L3VpncommonL3VpnPrefixTypeEnum.IPV4UNI;
import static org.onosproject.yang.gen.v1.nel3vpnapi.rev20141225.nel3vpnapi.devices.device.l3vpn.l3vpncomm.l3vpninstances.l3vpninstance.vpninstafs.VpnInstAf.LeafIdentifier.TNLPOLICYNAME;
import static org.onosproject.yang.gen.v1.netnlmtype.rev20141225.netnlmtype.TnlmbaseTnlPolicyType.of;
import static org.onosproject.yang.model.ModelObjectId.builder;

/**
 * Representation of utility for tunnel creation and deletion.
 */
public final class TnlConstructionUtil {

    /**
     * Error message for unsupported device type.
     */
    private static final String UNSUPPORTED_DEV_TYPE = "Levels other than " +
            "devices and device are not permitted.";

    /**
     * Error message for unsupported tunnel policy type.
     */
    private static final String UNSUPPORTED_TNL_POL_TYPE = "Levels other" +
            " than tnlm and tnl policy are not permitted.";

    // No instantiation.
    private TnlConstructionUtil() {
    }

    /**
     * Returns the created model object data of devices or device level from
     * the tunnel info.
     *
     * @param tnlInfo tunnel info
     * @return driver model object data
     */
    static ModelObjectData getCreateTnlDev(TunnelInfo tnlInfo) {
        Device device = new DefaultDevice();
        device.deviceid(tnlInfo.devId());

        switch (tnlInfo.level()) {
            case DEVICES:
                Devices devices = new DefaultDevices();
                devices.addToDevice(device);
                return getData(null, (InnerModelObject) devices);

            case DEVICE:
                Builder id = getDevicesId();
                return getData(id.build(), (InnerModelObject) device);

            default:
                throw new IllegalArgumentException(UNSUPPORTED_DEV_TYPE);
        }
    }

    /**
     * Returns the created model object data of tunnel policy from the tunnel
     * info.
     *
     * @param tnlInfo tunnel info
     * @return driver model object data
     */
    static ModelObjectData getCreateTnlPol(TunnelInfo tnlInfo) {
        Builder id = getDeviceId(tnlInfo.devId());
        TunnelPolicy tnlPol = new DefaultTunnelPolicy();
        tnlPol.tnlPolicyName(tnlInfo.polName());
        tnlPol.tnlPolicyType(of(TnlmbaseTnlPolicyTypeEnum.of(2)));

        switch (tnlInfo.level()) {
            case TNL_M:
                Tnlm tnlm = new DefaultTnlm();
                TunnelPolicys tnlPolicys = new DefaultTunnelPolicys();
                tnlPolicys.addToTunnelPolicy(tnlPol);
                tnlm.tunnelPolicys(tnlPolicys);
                return getData(id.build(), (InnerModelObject) tnlm);

            case TNL_POL:
                id = getTunnelPolicysId(id);
                return getData(id.build(), (InnerModelObject) tnlPol);

            default:
                throw new IllegalArgumentException(UNSUPPORTED_TNL_POL_TYPE);
        }
    }

    /**
     * Returns the created model object data of tunnel from the tunnel info.
     *
     * @param tnlInfo tunnel info
     * @return driver model object data
     */
    static ModelObjectData getCreateTnl(TunnelInfo tnlInfo) {
        TunnelPolicyKeys key = new TunnelPolicyKeys();
        key.tnlPolicyName(tnlInfo.polName());

        Builder id = getDeviceId(tnlInfo.devId());
        id = getTunnelPolicysId(id);
        id = id.addChild(DefaultTunnelPolicy.class, key);
        TpNexthop tpHop = new DefaultTpNexthop();
        TpTunnels tunnels = new DefaultTpTunnels();
        TpTunnel tunnel = new DefaultTpTunnel();
        tunnel.tunnelName(tnlInfo.tnlName());
        tunnel.autoTunnel(true);
        tunnels.addToTpTunnel(tunnel);
        tpHop.tpTunnels(tunnels);
        tpHop.nexthopIpaddr(Ipv4Address.of(tnlInfo.desIp()));

        if (tnlInfo.level() == TP_HOP) {
            id.addChild(DefaultTpNexthops.class);
            return getData(id.build(), (InnerModelObject) tpHop);
        } else {
            TpNexthops tpHops = new DefaultTpNexthops();
            tpHops.addToTpNexthop(tpHop);
            return getData(id.build(), (InnerModelObject) tpHops);
        }
    }

    /**
     * Returns the created model object data of binding the tunnel policy to
     * the VPN from the tunnel policy name and device id.
     *
     * @param tnlInfo tunnel info
     * @return driver model object data
     */
    static ModelObjectData getBindTnl(TunnelInfo tnlInfo) {
        L3VpnInstanceKeys vpnKey = new L3VpnInstanceKeys();
        vpnKey.vrfName(tnlInfo.polName());
        VpnInstAfKeys afKeys = new VpnInstAfKeys();
        afKeys.afType(L3VpncommonL3VpnPrefixType.of(IPV4UNI));

        Builder id = getModObjIdDriDevice(tnlInfo.devId());
        id.addChild(DefaultL3Vpn.class);
        id.addChild(DefaultL3Vpncomm.class);
        id.addChild(DefaultL3VpnInstances.class);
        id.addChild(DefaultL3VpnInstance.class, vpnKey);
        id.addChild(DefaultVpnInstAfs.class);
        id.addChild(DefaultVpnInstAf.class, afKeys);

        LeafModelObject leaf = new LeafModelObject();
        leaf.leafIdentifier(TNLPOLICYNAME);
        leaf.addValue(tnlInfo.polName());
        return DefaultModelObjectData.builder().addModelObject(leaf)
                .identifier(id.build()).build();
    }

    /**
     * Returns the driver model object data for delete, according to the
     * levels it has to be constructed for tunnel policy.
     *
     * @param tnlInfo tunnel info
     * @return driver model object data
     */
    static ModelObjectData getDeleteTnl(TunnelInfo tnlInfo) {
        Builder id = getDeviceId(tnlInfo.devId());
        switch (tnlInfo.level()) {
            case DEVICES:
                return getData(getDevicesId().build(), new DefaultDevice());

            case DEVICE:
                return getData(id.build(), new DefaultTnlm());

            case TNL_POL:
                id = getTunnelPolicysId(id);
                TunnelPolicyKeys polKey = new TunnelPolicyKeys();
                polKey.tnlPolicyName(tnlInfo.polName());
                id = id.addChild(DefaultTunnelPolicy.class, polKey);
                return getData(id.build(), new DefaultTpNexthops());

            default:
                throw new IllegalArgumentException(UNSUPPORTED_MODEL_LVL);
        }
    }

    /**
     * Returns the model object id of tunnel with devices and device.
     *
     * @param id device id
     * @return model object id
     */
    private static Builder getDeviceId(String id) {
        DeviceKeys devId = new DeviceKeys();
        devId.deviceid(id);
        return getDevicesId().addChild(DefaultDevice.class, devId);
    }

    /**
     * Returns the model object id of tunnel with devices.
     *
     * @return model object id
     */
    private static Builder getDevicesId() {
        return builder().addChild(DefaultDevices.class);
    }

    /**
     * Returns the model object id of tunnel policies.
     *
     * @param id model object id
     * @return model object id with tunnel policies
     */
    public static Builder getTunnelPolicysId(Builder id) {
        return id.addChild(DefaultTnlm.class)
                .addChild(DefaultTunnelPolicys.class);
    }
}
