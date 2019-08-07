/*
 * Copyright 2018 Open Networking Foundation
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

package org.onosproject.drivers.polatis.openflow;

import com.google.common.collect.Range;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.behaviour.PowerConfig;
import org.onosproject.net.driver.AbstractHandlerBehaviour;

import org.onosproject.openflow.controller.Dpid;
import org.onosproject.openflow.controller.OpenFlowController;
import org.onosproject.openflow.controller.OpenFlowSwitch;

import org.projectfloodlight.openflow.protocol.OFPortConfig;
import org.projectfloodlight.openflow.protocol.OFPortDesc;
import org.projectfloodlight.openflow.protocol.OFPortDescProp;
import org.projectfloodlight.openflow.protocol.OFPortDescPropOptical;
import org.projectfloodlight.openflow.protocol.OFPortMod;
import org.projectfloodlight.openflow.protocol.OFPortModProp;
import org.projectfloodlight.openflow.protocol.OFPortModPropOptical;
import org.projectfloodlight.openflow.protocol.ver14.OFOpticalPortFeaturesSerializerVer14;
import org.projectfloodlight.openflow.types.OFPort;

import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.onosproject.openflow.controller.Dpid.dpid;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Get current or target port/channel power from an openflow device.
 * Set target port power or channel attenuation to an openflow device.
 */
public class OpenFlowPowerConfig<T> extends AbstractHandlerBehaviour
        implements PowerConfig<T> {

    private static final Logger log = getLogger(OpenFlowPowerConfig.class);

    @Override
    public Optional<Double> getTargetPower(PortNumber port, T component) {
        // TODO: OpenFlow does not seem to have the concept of retrieving this
        // information as only the current power is returned in the port stats
        // reply. This can be different from the configured value. Perhaps, the
        // settings in annotations or building a lookup table with the latest
        // settings could be options.
        return Optional.empty();
    }

    @Override
    public void setTargetPower(PortNumber port, T component, double power) {
        setPortTargetPower(port, power);
    }

    @Override
    public Optional<Double> currentPower(PortNumber port, T component) {
        // TODO: Ideally, this needs to read the port stats output for real-time
        // data or as a short-term workaround, it could get the last read value
        // from the port stats polling.
        return null;
    }

    @Override
    public Optional<Range<Double>> getTargetPowerRange(PortNumber port, T component) {
        for (OFPortDesc pd : getPortDescs()) {
            if (pd.getPortNo().getPortNumber() == port.toLong()) {
                for (OFPortDescProp prop : pd.getProperties()) {
                    if (prop instanceof OFPortDescPropOptical) {
                        OFPortDescPropOptical oprop = (OFPortDescPropOptical) prop;
                        double txMin = oprop.getTxPwrMin();
                        double txMax = oprop.getTxPwrMax();
                        return Optional.of(Range.closed(txMin, txMax));
                    }
                }
            }
        }
        return Optional.empty();
    }

    private List<OFPortDesc> getPortDescs() {
        final Dpid dpid = dpid(handler().data().deviceId().uri());
        OpenFlowSwitch sw = handler().get(OpenFlowController.class).getSwitch(dpid);
        return sw.getPorts();
    }

    @Override
    public Optional<Range<Double>> getInputPowerRange(PortNumber port, T component) {
        log.warn("Unsupported as OpenFlow does not seem to have the concept of input (presumably rx) power range.");
        return Optional.empty();
    }

    @Override
    public List<PortNumber> getPorts(T component) {
        List<PortNumber> ports = new ArrayList<>();
        for (OFPortDesc pd : getPortDescs()) {
            for (OFPortDescProp prop : pd.getProperties()) {
                // Note: Power monitor detection can actually be more complex
                // than this. It is possible that the power is not
                // configurable, but it is readable. In this case, the best
                // bet is probably to check rx/tx power info valid in the
                // port stats reply, unfortunately.
                if (prop instanceof OFPortDescPropOptical) {
                    ports.add(PortNumber.portNumber(pd.getPortNo().getPortNumber()));
                    break;
                }
            }
        }
        return ports;
    }

    private OFPortMod.Builder makePortMod(OpenFlowSwitch sw, PortNumber portNumber,
                                          boolean enable) {
        OFPortMod.Builder pmb = sw.factory().buildPortMod();
        OFPort port = OFPort.of((int) portNumber.toLong());
        pmb.setPortNo(port);
        Set<OFPortConfig> portConfig = EnumSet.noneOf(OFPortConfig.class);
        if (!enable) {
            portConfig.add(OFPortConfig.PORT_DOWN);
        }
        pmb.setConfig(portConfig);
        Set<OFPortConfig> portMask = EnumSet.noneOf(OFPortConfig.class);
        portMask.add(OFPortConfig.PORT_DOWN);
        pmb.setMask(portMask);
        pmb.setAdvertise(0x0);
        for (OFPortDesc pd : sw.getPorts()) {
            if (pd.getPortNo().equals(port)) {
                pmb.setHwAddr(pd.getHwAddr());
                break;
            }
        }
        return pmb;
    }

    private boolean setPortTargetPower(PortNumber port, double power) {
        DeviceId deviceId = handler().data().deviceId();
        final Dpid dpid = dpid(deviceId.uri());
        OpenFlowSwitch sw = handler().get(OpenFlowController.class).getSwitch(dpid);
        if (sw == null || !sw.isConnected()) {
            log.error("Failed to change port on device {}", deviceId);
            return false;
        }
        boolean enable = false;
        for (OFPortDesc pd : getPortDescs()) {
            if (pd.getPortNo().getPortNumber() == port.toLong()) {
                enable = pd.getConfig().contains(OFPortConfig.PORT_DOWN);
                break;
            }
        }
        OFPortMod.Builder pmb = makePortMod(sw, port, enable);
        double configure = OFOpticalPortFeaturesSerializerVer14.TX_PWR_VAL;
        OFPortModPropOptical.Builder property = sw.factory().buildPortModPropOptical();
        property.setTxPwr((long) power);

        List<OFPortModProp> properties = new ArrayList<>();
        properties.add(property.build());
        pmb.setProperties(properties);

        sw.sendMsg(Collections.singletonList(pmb.build()));
        // TODO: We would need to report false in case of port mod failure.
        return true;
    }
}
