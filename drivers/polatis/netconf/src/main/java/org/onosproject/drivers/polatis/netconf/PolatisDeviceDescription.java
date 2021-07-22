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
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.device.DefaultDeviceDescription;
import org.onosproject.net.device.DeviceDescription;
import org.onosproject.net.device.DeviceDescriptionDiscovery;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.device.PortDescription;
import org.onosproject.net.driver.AbstractHandlerBehaviour;

import org.onlab.packet.ChassisId;

import org.slf4j.Logger;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onosproject.net.Device.Type.FIBER_SWITCH;

import static org.onosproject.drivers.polatis.netconf.PolatisUtility.getPortsFilter;
import static org.onosproject.drivers.polatis.netconf.PolatisUtility.getProdInfoFilter;
import static org.onosproject.drivers.polatis.netconf.PolatisUtility.parsePorts;
import static org.onosproject.drivers.polatis.netconf.PolatisUtility.KEY_MANUFACTURER;
import static org.onosproject.drivers.polatis.netconf.PolatisUtility.KEY_HWVERSION;
import static org.onosproject.drivers.polatis.netconf.PolatisUtility.KEY_SWVERSION;
import static org.onosproject.drivers.polatis.netconf.PolatisUtility.KEY_SERIALNUMBER;
import static org.onosproject.drivers.polatis.netconf.PolatisUtility.KEY_INPUTPORTS;
import static org.onosproject.drivers.polatis.netconf.PolatisUtility.KEY_OUTPUTPORTS;

import static org.onosproject.drivers.polatis.netconf.PolatisNetconfUtility.configAt;
import static org.onosproject.drivers.polatis.netconf.PolatisNetconfUtility.netconfGet;
import static org.onosproject.drivers.polatis.netconf.PolatisNetconfUtility.subscribe;
import static org.onosproject.drivers.polatis.netconf.PolatisNetconfUtility.KEY_DATA_PRODINF;

/**
 * Representation of device information and ports via NETCONF for all Polatis
 * optical circuit switches.
 */
public class PolatisDeviceDescription extends AbstractHandlerBehaviour
    implements DeviceDescriptionDiscovery {

    private final Logger log = getLogger(getClass());

    /**
     * Discovers device details, for polatis device by getting the system
     * information.
     *
     * @return device description
     */
    @Override
    public DeviceDescription discoverDeviceDetails() {
        log.debug("Discovering Polatis device detais...");
        return parseProductInformation();
    }

    private DeviceDescription parseProductInformation() {
        DeviceService devsvc = checkNotNull(handler().get(DeviceService.class));
        DeviceId devID = handler().data().deviceId();
        String reply = netconfGet(handler(), getProdInfoFilter());
        subscribe(handler());
        HierarchicalConfiguration cfg = configAt(reply, KEY_DATA_PRODINF);
        String hw = cfg.getString(KEY_HWVERSION);
        String numInputPorts = "0";
        String numOutputPorts = "0";
        if (!hw.equals("")) {
            Pattern patternSize = Pattern.compile("\\d+x[\\dC]+");
            Matcher matcher = patternSize.matcher(hw);
            if (matcher.find()) {
                String switchSize = matcher.group();
                log.debug("Got switch size: " + switchSize);
                Pattern patternNumber = Pattern.compile("[\\dC]+");
                matcher = patternNumber.matcher(switchSize);
                if (matcher.find()) {
                    numInputPorts = matcher.group();
                    log.debug("numInputPorts=" + numInputPorts);
                    if (matcher.find()) {
                        if (!matcher.group().equals("CC")) {
                            numOutputPorts = matcher.group();
                        }
                    }
                log.debug("numOutputPorts=" + numOutputPorts);
                }
            }
        } else {
            log.warn("Unable to determine type of Polatis switch " + devID.toString());
        }
        DefaultAnnotations annotations = DefaultAnnotations.builder()
                .set(KEY_INPUTPORTS, numInputPorts)
                .set(KEY_OUTPUTPORTS, numOutputPorts)
                .build();

        return new DefaultDeviceDescription(devID.uri(), FIBER_SWITCH,
                cfg.getString(KEY_MANUFACTURER), cfg.getString(KEY_HWVERSION),
                cfg.getString(KEY_SWVERSION), cfg.getString(KEY_SERIALNUMBER),
                new ChassisId(cfg.getString(KEY_SERIALNUMBER)), true, annotations);
    }

    /**
     * Discovers port details, for polatis device.
     *
     * @return port list
     */
    @Override
    public List<PortDescription> discoverPortDetails() {
        log.debug("Discovering ports on Polatis switch...");
        DeviceService deviceService = handler().get(DeviceService.class);
        DeviceId deviceID = handler().data().deviceId();
        Device device = deviceService.getDevice(deviceID);
        int numInputPorts = Integer.parseInt(device.annotations().value(KEY_INPUTPORTS));
        int numOutputPorts = Integer.parseInt(device.annotations().value(KEY_OUTPUTPORTS));
        String reply = netconfGet(handler(), getPortsFilter());
        List<PortDescription> descriptions = parsePorts(reply, numInputPorts, numOutputPorts);
        return ImmutableList.copyOf(descriptions);
    }
}
