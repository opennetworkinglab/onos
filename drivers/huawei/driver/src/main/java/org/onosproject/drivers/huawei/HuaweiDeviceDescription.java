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

import com.google.common.collect.ImmutableList;
import org.apache.commons.lang.StringUtils;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.device.DefaultDeviceDescription;
import org.onosproject.net.device.DeviceDescription;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.device.PortDescription;
import org.onosproject.net.device.PortStatistics;
import org.onosproject.net.device.PortStatisticsDiscovery;
import org.onosproject.net.driver.AbstractHandlerBehaviour;
import org.onosproject.netconf.NetconfController;
import org.onosproject.netconf.NetconfException;
import org.onosproject.netconf.NetconfSession;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onosproject.drivers.huawei.DriverUtil.DEV_INFO_FAILURE;
import static org.onosproject.drivers.huawei.DriverUtil.RPC_CLOSE;
import static org.onosproject.drivers.huawei.DriverUtil.RPC_CLOSE_FILTER;
import static org.onosproject.drivers.huawei.DriverUtil.RPC_CLOSE_GET;
import static org.onosproject.drivers.huawei.DriverUtil.RPC_CLOSE_IFM;
import static org.onosproject.drivers.huawei.DriverUtil.RPC_FILTER;
import static org.onosproject.drivers.huawei.DriverUtil.RPC_GET;
import static org.onosproject.drivers.huawei.DriverUtil.RPC_IFM;
import static org.onosproject.drivers.huawei.DriverUtil.RPC_IFS;
import static org.onosproject.drivers.huawei.DriverUtil.RPC_MSG;
import static org.onosproject.drivers.huawei.DriverUtil.RPC_SYS;
import static org.onosproject.net.Device.Type.ROUTER;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Representation of device information and ports via NETCONF for huawei
 * routers.
 */
public class HuaweiDeviceDescription extends AbstractHandlerBehaviour
        implements PortStatisticsDiscovery {

    private final Logger log = getLogger(getClass());

    /**
     * Constructs huawei device description.
     */
    public HuaweiDeviceDescription() {
    }

    /**
     * Discovers device details, for huawei device by getting the system
     * information.
     *
     * @return device description
     */
    @Override
    public DeviceDescription discoverDeviceDetails() {
        NetconfSession session = getNetconfSession();
        String sysInfo;
        try {
            sysInfo = session.get(getVersionReq());
        } catch (IOException e) {
            throw new IllegalArgumentException(
                    new NetconfException(DEV_INFO_FAILURE));
        }

        String[] details = parseSysInfoXml(sysInfo);
        DeviceService devSvc = checkNotNull(handler().get(DeviceService.class));
        DeviceId devId = handler().data().deviceId();
        Device dev = devSvc.getDevice(devId);
        return new DefaultDeviceDescription(dev.id().uri(), ROUTER,
                                            details[0], details[1],
                                            details[2], details[3],
                                            dev.chassisId());
    }

    /**
     * Discovers interface details, for huawei device.
     *
     * @return port list
     */
    @Override
    public List<PortDescription> discoverPortDetails() {
        return ImmutableList.copyOf(parseInterfaceXml(getInterfaces()));
    }

    /**
     * Returns the NETCONF session of the device.
     *
     * @return session
     */
    private NetconfSession getNetconfSession() {
        NetconfController controller = checkNotNull(
                handler().get(NetconfController.class));
        return controller.getDevicesMap().get(handler().data().deviceId())
                .getSession();
    }

    /**
     * Returns the rpc request message for fetching system details in huawei
     * device.
     *
     * @return rpc request message
     */
    private String getVersionReq() {
        StringBuilder rpc = new StringBuilder(RPC_MSG);
        rpc.append(RPC_GET);
        rpc.append(RPC_FILTER);
        rpc.append(RPC_SYS);
        rpc.append(RPC_CLOSE_FILTER);
        rpc.append(RPC_CLOSE_GET);
        rpc.append(RPC_CLOSE);
        return rpc.toString();
    }

    /**
     * Parses system info received from huawei device.
     *
     * @param sysInfo system info
     * @return parsed values
     */
    private String[] parseSysInfoXml(String sysInfo) {
        HuaweiXmlParser parser = new HuaweiXmlParser(sysInfo);
        parser.parseSysInfo();
        return parser.getInfo();
    }

    /**
     * Returns the rpc request message for fetching interface details in
     * huawei device.
     *
     * @return rpc request message
     */
    private String getInterfacesReq() {
        StringBuilder rpc = new StringBuilder(RPC_MSG);
        rpc.append(RPC_GET);
        rpc.append(RPC_FILTER);
        rpc.append(RPC_IFM);
        rpc.append(RPC_IFS);
        rpc.append(RPC_CLOSE_IFM);
        rpc.append(RPC_CLOSE_FILTER);
        rpc.append(RPC_CLOSE_GET);
        rpc.append(RPC_CLOSE);
        return rpc.toString();
    }

    /**
     * Parses interfaces received from huawei device.
     *
     * @param interfaces interfaces
     * @return port list
     */
    private List<PortDescription> parseInterfaceXml(String interfaces) {
        HuaweiXmlParser parser = new HuaweiXmlParser(interfaces);
        parser.parseInterfaces();
        return parser.getPorts();
    }

    @Override
    public Collection<PortStatistics> discoverPortStatistics() {
        String interfaces = getInterfaces();
        if (StringUtils.isNotBlank(interfaces)) {
            Collection<PortStatistics> portStats = getPortStatistics(interfaces);
            return ImmutableList.copyOf(portStats);
        }
        return null;
    }

    private String getInterfaces() {
        NetconfSession session = getNetconfSession();
        String interfaces = null;
        try {
            interfaces = session.get(getInterfacesReq());
        } catch (IOException e) {
            log.info("Failed to retrive interface {} ", e.getMessage());
        }
        return interfaces;
    }

    private Collection<PortStatistics> getPortStatistics(String ifs) {
        HuaweiXmlParser parser = new HuaweiXmlParser(ifs);
        return parser.parsePortsStatistics(handler().data().deviceId());
    }
}
