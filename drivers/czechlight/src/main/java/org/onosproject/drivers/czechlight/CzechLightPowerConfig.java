/*
 * Copyright 2019-2020 Jan Kundrát, CESNET, <jan.kundrat@cesnet.cz> and Open Networking Foundation
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

package org.onosproject.drivers.czechlight;

import com.google.common.collect.Range;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.onosproject.net.PortNumber;
import org.onosproject.net.OchSignal;
import org.onosproject.net.behaviour.PowerConfig;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.driver.AbstractHandlerBehaviour;

import java.util.Arrays;
import java.util.Optional;

import org.onosproject.netconf.DatastoreId;
import org.onosproject.netconf.NetconfController;
import org.onosproject.netconf.NetconfException;
import org.onosproject.netconf.NetconfSession;
import org.slf4j.Logger;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.slf4j.LoggerFactory.getLogger;

public class CzechLightPowerConfig<T> extends AbstractHandlerBehaviour
        implements PowerConfig<T> {

    private final Logger log = getLogger(getClass());


    @Override
    public Optional<Double> getTargetPower(PortNumber port, T component) {
        return Optional.empty();
    }

    //Used by the ROADM app to set the "attenuation" parameter
    @Override
    public void setTargetPower(PortNumber port, T component, double power) {
        switch (deviceType()) {
            case LINE_DEGREE:
            case ADD_DROP_FLEX:
                if (!(component instanceof OchSignal)) {
                    log.error("Cannot set target power on anything but a Media Channel");
                    return;
                }
                HierarchicalConfiguration xml;
                try {
                    xml = doGetSubtree(CzechLightDiscovery.CHANNEL_DEFS_FILTER + CzechLightDiscovery.MC_ROUTING_FILTER);
                } catch (NetconfException e) {
                    log.error("Cannot read data from NETCONF: {}", e);
                    return;
                }
                final var allChannels = MediaChannelDefinition.parseChannelDefinitions(xml);
                final var och = ((OchSignal) component);
                final var channel = allChannels.entrySet().stream()
                        .filter(entry -> MediaChannelDefinition.mcMatches(entry, och))
                        .findAny()
                        .orElse(null);
                if (channel == null) {
                    log.error("Cannot map OCh definition {} to a channel from the channel plan", och);
                    return;
                }
                final String element = port.toLong() == CzechLightDiscovery.PORT_COMMON ? "add" : "drop";
                log.debug("{}: setting power for MC {} to {}",
                        data().deviceId(), channel.getKey().toUpperCase(), power);
                var sb = new StringBuilder();
                sb.append(CzechLightDiscovery.XML_MC_OPEN);
                sb.append("<channel>");
                sb.append(channel.getKey());
                sb.append("</channel>");
                sb.append("<");
                sb.append(element);
                sb.append("><power>");
                sb.append(power);
                sb.append("</power></");
                sb.append(element);
                sb.append(">");
                sb.append(CzechLightDiscovery.XML_MC_CLOSE);
                doEditConfig("merge", sb.toString());
                return;
            default:
                log.error("Target power is only supported on WSS-based devices");
                return;
        }
    }

    @Override
    public Optional<Double> currentPower(PortNumber port, T component) {
        if (component instanceof OchSignal) {
            // FIXME: this should be actually very easy for MCs that are routed...
            log.debug("per-MC power not implemented yet");
            return Optional.empty();
        }
        switch (deviceType()) {
            case LINE_DEGREE:
            case ADD_DROP_FLEX:
                if (port.toLong() == CzechLightDiscovery.PORT_COMMON) {
                    return Optional.ofNullable(fetchLeafDouble(CzechLightDiscovery.NS_CZECHLIGHT_ROADM_DEVICE,
                            "aggregate-power/common-out"));
                } else {
                    return Optional.ofNullable(fetchLeafSum(CzechLightDiscovery.NS_CZECHLIGHT_ROADM_DEVICE,
                            "media-channels[drop/port = '" +
                                    CzechLightDiscovery.leafPortName(deviceType(), port.toLong()) +
                                    "']/power/leaf-out"));
                }
            case COHERENT_ADD_DROP:
                if (component instanceof OchSignal) {
                    log.debug("Coherent Add/Drop: cannot query per-MC channel power");
                    return Optional.empty();
                }
                if (port.toLong() == CzechLightDiscovery.PORT_COMMON) {
                    return Optional.ofNullable(fetchLeafDouble(CzechLightDiscovery.NS_CZECHLIGHT_COHERENT_A_D,
                            "aggregate-power/express-out"));
                } else {
                    return Optional.ofNullable(fetchLeafDouble(CzechLightDiscovery.NS_CZECHLIGHT_COHERENT_A_D,
                            "aggregate-power/drop"));
                }
            case INLINE_AMP:
                return Optional.ofNullable(fetchLeafDouble(CzechLightDiscovery.NS_CZECHLIGHT_INLINE_AMP,
                        inlineAmpStageNameFor(port) + "/optical-power/output"));
            default:
                assert false : "unhandled device type";
        }
        return Optional.empty();
    }

    @Override
    public Optional<Double> currentInputPower(PortNumber port, T component)  {
        if (component instanceof OchSignal) {
            log.debug("per-MC power not implemented yet");
            return Optional.empty();
        }
        switch (deviceType()) {
            case LINE_DEGREE:
            case ADD_DROP_FLEX:
                if (port.toLong() == CzechLightDiscovery.PORT_COMMON) {
                    return Optional.ofNullable(fetchLeafDouble(CzechLightDiscovery.NS_CZECHLIGHT_ROADM_DEVICE,
                            "aggregate-power/common-in"));
                } else {
                    return Optional.ofNullable(fetchLeafSum(CzechLightDiscovery.NS_CZECHLIGHT_ROADM_DEVICE,
                            "media-channels[add/port = '" +
                                    CzechLightDiscovery.leafPortName(deviceType(), port.toLong()) +
                                    "']/power/leaf-in"));
                }
            case COHERENT_ADD_DROP:
                if (component instanceof OchSignal) {
                    log.debug("Coherent Add/Drop: cannot query per-MC channel power");
                    return Optional.empty();
                }
                if (port.toLong() == CzechLightDiscovery.PORT_COMMON) {
                    return Optional.ofNullable(fetchLeafDouble(CzechLightDiscovery.NS_CZECHLIGHT_COHERENT_A_D,
                            "aggregate-power/express-in"));
                } else {
                    return Optional.ofNullable(fetchLeafDouble(CzechLightDiscovery.NS_CZECHLIGHT_COHERENT_A_D,
                            "client-ports[port='" + Long.toString(port.toLong()) + "']/input-power"));
                }
            case INLINE_AMP:
                return Optional.ofNullable(fetchLeafDouble(CzechLightDiscovery.NS_CZECHLIGHT_INLINE_AMP,
                        inlineAmpStageNameFor(port) + "/optical-power/input"));
            default:
                assert false : "unhandled device type";
        }
        return Optional.empty();
    }

    @Override
    public Optional<Range<Double>> getTargetPowerRange(PortNumber portNumber, T component) {
        switch (deviceType()) {
            case LINE_DEGREE:
            case ADD_DROP_FLEX:
                if (component instanceof OchSignal) {
                    // not all values might be actually set, it's complicated, so at least return some limit
                    return Optional.ofNullable(Range.closed(-25.0, 5.0));
                }
            default:
                // pass
        }
        return Optional.empty();
    }

    @Override
    public Optional<Range<Double>> getInputPowerRange(PortNumber portNumber, T component) {
        switch (deviceType()) {
            case LINE_DEGREE:
            case ADD_DROP_FLEX:
                if (component instanceof OchSignal) {
                    // not all values might be actually set, it's complicated, so at least return some limit
                    return Optional.ofNullable(Range.closed(-30.0, +10.0));
                }
            default:
                // pass
        }
        return Optional.empty();
    }

    private CzechLightDiscovery.DeviceType deviceType() {
        var annotations = this.handler().get(DeviceService.class).getDevice(handler().data().deviceId()).annotations();
        return CzechLightDiscovery.DeviceType.valueOf(annotations.value(CzechLightDiscovery.DEVICE_TYPE_ANNOTATION));
    }

    private Double fetchLeafDouble(final String namespace, final String xpath) {
        try {
            final var res = doGetXPath("M", namespace, "/M:" + xpath);
            final var key = CzechLightDiscovery.xpathToXmlKey(xpath);
            if (!res.containsKey(key)) {
                log.error("<get> reply does not contain data for key '{}'", key);
                return null;
            }
            return res.getDouble(key);
        } catch (NetconfException e) {
            log.error("Cannot read data from NETCONF: {}", e);
            return null;
        }
    }

    private Double fetchLeafSum(final String namespace, final String xpath) {
        try {
            final var data = doGetXPath("M", namespace, "/M:" + xpath);
            final var key = CzechLightDiscovery.xpathToXmlKey(xpath);
            final var power = Arrays.stream(data.getStringArray(key))
                    .map(s -> Double.valueOf(s))
                    .map(dBm -> CzechLightDiscovery.dbmToMilliwatts(dBm))
                    .reduce(0.0, Double::sum);
            log.debug(" -> power lin {}, dBm: {}", power, CzechLightDiscovery.milliwattsToDbm(power));
            return CzechLightDiscovery.milliwattsToDbm(power);
        } catch (NetconfException e) {
            log.error("Cannot read data from NETCONF: {}", e);
            return null;
        }
    }

    private String inlineAmpStageNameFor(final PortNumber port) {
        return port.toLong() == CzechLightDiscovery.PORT_INLINE_WEST ? "west-to-east" : "east-to-west";
    }

    private HierarchicalConfiguration doGetXPath(final String prefix, final String namespace, final String xpathFilter)
            throws NetconfException {
        NetconfSession session = getNetconfSession();
        if (session == null) {
            log.error("Cannot request NETCONF session for {}", data().deviceId());
            return null;
        }
        return CzechLightDiscovery.doGetXPath(session, prefix, namespace, xpathFilter);
    }

    private HierarchicalConfiguration doGetSubtree(final String subtreeXml) throws NetconfException {
        NetconfSession session = getNetconfSession();
        if (session == null) {
            log.error("Cannot request NETCONF session for {}", data().deviceId());
            return null;
        }
        return CzechLightDiscovery.doGetSubtree(session, subtreeXml);
    }

    public boolean doEditConfig(String mode, String cfg) {
        NetconfSession session = getNetconfSession();
        if (session == null) {
            log.error("Cannot request NETCONF session for {}", data().deviceId());
            return false;
        }

        try {
            return session.editConfig(DatastoreId.RUNNING, mode, cfg);
        } catch (NetconfException e) {
            throw new IllegalStateException(new NetconfException("Failed to edit configuration.", e));
        }
    }

    private NetconfSession getNetconfSession() {
        NetconfController controller =
                checkNotNull(handler().get(NetconfController.class));
        return controller.getNetconfDevice(data().deviceId()).getSession();
    }
}
