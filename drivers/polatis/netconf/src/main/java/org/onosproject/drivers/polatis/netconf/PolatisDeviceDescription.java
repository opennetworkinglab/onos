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

package org.onosproject.drivers.polatis.netconf;

import org.apache.commons.configuration.HierarchicalConfiguration;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.onosproject.net.AnnotationKeys;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DefaultDeviceDescription;
import org.onosproject.net.device.DeviceDescription;
import org.onosproject.net.device.DeviceDescriptionDiscovery;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.device.PortDescription;
import org.onosproject.net.driver.AbstractHandlerBehaviour;

import static org.onosproject.net.optical.device.OmsPortHelper.omsPortDescription;

import org.onlab.packet.ChassisId;
import org.onlab.util.Frequency;
import org.onlab.util.Spectrum;

import org.slf4j.Logger;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onosproject.net.Device.Type.FIBER_SWITCH;

import static org.onosproject.drivers.polatis.netconf.PolatisNetconfUtility.configAt;
import static org.onosproject.drivers.polatis.netconf.PolatisNetconfUtility.configsAt;
import static org.onosproject.drivers.polatis.netconf.PolatisNetconfUtility.netconfGet;
import static org.onosproject.drivers.polatis.netconf.PolatisNetconfUtility.subscribe;
import static org.onosproject.drivers.polatis.netconf.PolatisNetconfUtility.xmlOpen;
import static org.onosproject.drivers.polatis.netconf.PolatisNetconfUtility.xmlClose;
import static org.onosproject.drivers.polatis.netconf.PolatisNetconfUtility.xmlEmpty;
import static org.onosproject.drivers.polatis.netconf.PolatisNetconfUtility.KEY_PORT;
import static org.onosproject.drivers.polatis.netconf.PolatisNetconfUtility.KEY_PORTID;
import static org.onosproject.drivers.polatis.netconf.PolatisNetconfUtility.KEY_PORTCONFIG;
import static org.onosproject.drivers.polatis.netconf.PolatisNetconfUtility.KEY_PRODINF;
import static org.onosproject.drivers.polatis.netconf.PolatisNetconfUtility.KEY_PORTCONFIG_XMLNS;
import static org.onosproject.drivers.polatis.netconf.PolatisNetconfUtility.KEY_PRODINF_XMLNS;
import static org.onosproject.drivers.polatis.netconf.PolatisNetconfUtility.KEY_DATA_PRODINF;
import static org.onosproject.drivers.polatis.netconf.PolatisNetconfUtility.KEY_DATA_PORTCONFIG;

/**
 * Representation of device information and ports via NETCONF for all Polatis
 * optical circuit switches.
 */
public class PolatisDeviceDescription extends AbstractHandlerBehaviour
    implements DeviceDescriptionDiscovery {

    public static final String DEFAULT_MANUFACTURER = "Polatis";
    public static final String DEFAULT_DESCRIPTION_DATA = "Unknown";
    public static final String KEY_MANUFACTURER = "manufacturer";
    public static final String KEY_HWVERSION = "model-name";
    public static final String KEY_SWVERSION = "software-version";
    public static final String KEY_SERIALNUMBER = "serial-number";
    public static final String KEY_PORTSTATUS = "status";
    public static final String PORT_ENABLED = "ENABLED";
    public static final String KEY_PORTLABEL = "label";
    public static final int POLATIS_NUM_OF_WAVELENGTHS = 39;

    private final Logger log = getLogger(getClass());

    /**
     * Discovers device details, for polatis device by getting the system
     * information.
     *
     * @return device description
     */
    @Override
    public DeviceDescription discoverDeviceDetails() {
        return parseProductInformation();
    }

    private DeviceDescription parseProductInformation() {
        DeviceService devsvc = checkNotNull(handler().get(DeviceService.class));
        DeviceId devid = handler().data().deviceId();
        Device dev = devsvc.getDevice(devid);
        if (dev == null) {
            return new DefaultDeviceDescription(devid.uri(), FIBER_SWITCH,
                    DEFAULT_MANUFACTURER, DEFAULT_DESCRIPTION_DATA,
                    DEFAULT_DESCRIPTION_DATA, DEFAULT_DESCRIPTION_DATA,
                    new ChassisId());
        }
        String reply = netconfGet(handler(), getProductInformationFilter());
        subscribe(handler());
        HierarchicalConfiguration cfg = configAt(reply, KEY_DATA_PRODINF);
        return new DefaultDeviceDescription(dev.id().uri(), FIBER_SWITCH,
                cfg.getString(KEY_MANUFACTURER), cfg.getString(KEY_HWVERSION),
                cfg.getString(KEY_SWVERSION), cfg.getString(KEY_SERIALNUMBER),
                dev.chassisId());
    }

    private String getProductInformationFilter() {
        return new StringBuilder(xmlOpen(KEY_PRODINF_XMLNS))
                .append(xmlClose(KEY_PRODINF))
                .toString();
    }

    /**
     * Discovers port details, for polatis device.
     *
     * @return port list
     */
    @Override
    public List<PortDescription> discoverPortDetails() {
        String reply = netconfGet(handler(), getPortsFilter());
        List<PortDescription> descriptions = parsePorts(reply);
        return ImmutableList.copyOf(descriptions);
    }

    private String getPortsFilter() {
        return new StringBuilder(xmlOpen(KEY_PORTCONFIG_XMLNS))
                .append(xmlOpen(KEY_PORT))
                .append(xmlEmpty(KEY_PORTID))
                .append(xmlEmpty(KEY_PORTSTATUS))
                .append(xmlEmpty(KEY_PORTLABEL))
                .append(xmlClose(KEY_PORT))
                .append(xmlClose(KEY_PORTCONFIG))
                .toString();
    }

    private List<PortDescription> parsePorts(String content) {
        List<HierarchicalConfiguration> subtrees = configsAt(content, KEY_DATA_PORTCONFIG);
        List<PortDescription> portDescriptions = Lists.newArrayList();
        for (HierarchicalConfiguration portConfig : subtrees) {
            portDescriptions.add(parsePort(portConfig));
        }
        return portDescriptions;
    }

    private PortDescription parsePort(HierarchicalConfiguration cfg) {
        PortNumber portNumber = PortNumber.portNumber(cfg.getLong(KEY_PORTID));
        DefaultAnnotations annotations = DefaultAnnotations.builder()
                .set(AnnotationKeys.PORT_NAME, cfg.getString(KEY_PORTLABEL))
                .build();
        return omsPortDescription(portNumber,
                cfg.getString(KEY_PORTSTATUS).equals(PORT_ENABLED),
                Spectrum.O_BAND_MIN, Spectrum.L_BAND_MAX,
                Frequency.ofGHz((Spectrum.O_BAND_MIN.asGHz() -
                        Spectrum.L_BAND_MAX.asGHz()) /
                    POLATIS_NUM_OF_WAVELENGTHS), annotations);
    }
}
