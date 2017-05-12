/*
 * Copyright 2016 Open Networking Laboratory
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

package org.onosproject.drivers.oplink;

import com.google.common.collect.Range;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.onosproject.driver.extensions.OplinkAttenuation;
import org.onosproject.net.Device;
import org.onosproject.net.Direction;
import org.onosproject.net.OchSignal;
import org.onosproject.net.PortNumber;
import org.onosproject.net.behaviour.PowerConfig;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.driver.AbstractHandlerBehaviour;
import org.onosproject.net.flow.DefaultFlowRule;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.FlowEntry;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.flow.TrafficTreatment;
import org.slf4j.Logger;

import java.util.Optional;

import static org.onosproject.drivers.oplink.OplinkOpticalUtility.POWER_MULTIPLIER;
import static org.onosproject.drivers.oplink.OplinkOpticalUtility.RANGE_ATT;
import static org.onosproject.drivers.oplink.OplinkOpticalUtility.RANGE_GENERAL;
import static org.onosproject.drivers.oplink.OplinkNetconfUtility.*;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Get current or target port/channel power from an Oplink optical netconf device.
 * Set target port power or channel attenuation to an optical netconf device.
 */
public class OplinkOpticalPowerConfig<T> extends AbstractHandlerBehaviour
        implements PowerConfig<T> {

    // key
    public static final String KEY_CHNUM = "wavelength-number";
    public static final String KEY_CHPWR = "wavelength-power";
    public static final String KEY_CHSTATS = "wavelength-stats";
    public static final String KEY_OCMSTATS = "ocm-stats";
    public static final String KEY_PORTDIRECT_RX = "rx";
    public static final String KEY_PORTDIRECT_TX = "tx";
    public static final String KEY_PORTTARPWR = "port-target-power";
    public static final String KEY_PORTCURPWR = "port-current-power";
    public static final String KEY_PORTPROPERTY = "port-property";
    public static final String KEY_PORTPWRCAPMINRX = "port-power-capability-min-rx";
    public static final String KEY_PORTPWRCAPMAXRX = "port-power-capability-max-rx";
    public static final String KEY_PORTPWRCAPMINTX = "port-power-capability-min-tx";
    public static final String KEY_PORTPWRCAPMAXTX = "port-power-capability-max-tx";
    public static final String KEY_PORTS_PORT = String.format("%s.%s", KEY_DATA_PORTS, KEY_PORT);
    public static final String KEY_PORTS_PORT_PROPERTY = String.format("%s.%s", KEY_PORTS_PORT, KEY_PORTPROPERTY);
    // log
    private static final Logger log = getLogger(OplinkOpticalPowerConfig.class);

    @Override
    public Optional<Long> getTargetPower(PortNumber port, T component) {
        return Optional.ofNullable(acquireTargetPower(port, component));
    }

    @Override
    public void setTargetPower(PortNumber port, T component, long power) {
        if (component instanceof OchSignal) {
            setChannelTargetPower(port, (OchSignal) component, power);
        } else {
            setPortTargetPower(port, power);
        }
    }

    @Override
    public Optional<Long> currentPower(PortNumber port, T component) {
        return Optional.ofNullable(acquireCurrentPower(port, component));
    }

    @Override
    public Optional<Range<Long>> getTargetPowerRange(PortNumber port, T component) {
        return Optional.ofNullable(getTxPowerRange(port, component));
    }

    @Override
    public Optional<Range<Long>> getInputPowerRange(PortNumber port, T component) {
        return Optional.ofNullable(getRxPowerRange(port, component));
    }

    private String getPortPowerFilter(PortNumber port, String selection) {
        return new StringBuilder(xmlOpen(KEY_OPENOPTICALDEV_XMLNS))
                .append(xmlOpen(KEY_PORTS))
                .append(xml(KEY_PORTID, Long.toString(port.toLong())))
                .append(xmlOpen(KEY_PORT))
                .append(xmlEmpty(selection))
                .append(xmlClose(KEY_PORT))
                .append(xmlClose(KEY_PORTS))
                .append(xmlClose(KEY_OPENOPTICALDEV))
                .toString();
    }

    private String getChannelPowerFilter(PortNumber port, OchSignal channel) {
        return new StringBuilder(xmlOpen(KEY_OPENOPTICALDEV_XMLNS))
                .append(xmlOpen(KEY_PORTS))
                .append(xml(KEY_PORTID, Long.toString(port.toLong())))
                .append(xmlOpen(KEY_PORT))
                .append(xmlOpen(KEY_OCMSTATS))
                .append(xml(KEY_CHNUM, Integer.toString(channel.spacingMultiplier())))
                .append(xmlEmpty(KEY_CHSTATS))
                .append(xmlClose(KEY_OCMSTATS))
                .append(xmlClose(KEY_PORT))
                .append(xmlClose(KEY_PORTS))
                .append(xmlClose(KEY_OPENOPTICALDEV))
                .toString();
    }

    private String getChannelAttenuationFilter(PortNumber port, OchSignal channel) {
        return new StringBuilder(xmlOpen(KEY_OPENOPTICALDEV_XMLNS))
                .append(xmlOpen(KEY_CONNS))
                .append(xml(KEY_CONNID, Integer.toString(channel.spacingMultiplier())))
                .append(xmlEmpty(KEY_CHATT))
                .append(xmlClose(KEY_CONNS))
                .append(xmlClose(KEY_OPENOPTICALDEV))
                .toString();
    }

    private String getPowerRangeFilter(PortNumber port, String direction) {
        return new StringBuilder(xmlOpen(KEY_OPENOPTICALDEV_XMLNS))
                .append(xmlOpen(KEY_PORTS))
                .append(xml(KEY_PORTID, Long.toString(port.toLong())))
                .append(xmlOpen(KEY_PORT))
                .append(xml(KEY_PORTDIRECT, direction))
                .append(xmlEmpty(KEY_PORTPROPERTY))
                .append(xmlClose(KEY_PORT))
                .append(xmlClose(KEY_PORTS))
                .append(xmlClose(KEY_OPENOPTICALDEV))
                .toString();
    }

    private Long acquireTargetPower(PortNumber port, T component) {
        if (component instanceof OchSignal) {
            return acquireChannelAttenuation(port, (OchSignal) component);
        }
        log.debug("Get port{} target power...", port);
        return acquirePortPower(port, KEY_PORTTARPWR);
    }

    private Long acquireCurrentPower(PortNumber port, T component) {
        if (component instanceof OchSignal) {
            return acquireChannelPower(port, (OchSignal) component);
        }
        log.debug("Get port{} current power...", port);
        return acquirePortPower(port, KEY_PORTCURPWR);
    }

    private Long acquirePortPower(PortNumber port, String selection) {
        String reply = netconfGet(handler(), getPortPowerFilter(port, selection));
        HierarchicalConfiguration info = configAt(reply, KEY_PORTS_PORT);
        if (info == null) {
            return null;
        }
        return (long) (info.getDouble(selection) * POWER_MULTIPLIER);
    }

    private Long acquireChannelAttenuation(PortNumber port, OchSignal channel) {
        log.debug("Get port{} channel{} attenuation...", port, channel.channelSpacing());
        String reply = netconfGet(handler(), getChannelAttenuationFilter(port, channel));
        HierarchicalConfiguration info = configAt(reply, KEY_CONNS);
        if (info == null) {
            return null;
        }
        return (long) (info.getDouble(KEY_CHATT) * POWER_MULTIPLIER);
    }

    private Long acquireChannelPower(PortNumber port, OchSignal channel) {
        log.debug("Get port{} channel{} power...", port, channel.channelSpacing());
        String reply = netconfGet(handler(), getChannelPowerFilter(port, channel));
        HierarchicalConfiguration info = configAt(reply, KEY_DATA_CONNS);
        if (info == null) {
            return null;
        }
        return (long) (info.getDouble(KEY_CHPWR) * POWER_MULTIPLIER);
    }

    private boolean setPortTargetPower(PortNumber port, long power) {
        log.debug("Set port{} target power...", port);
        String cfg = new StringBuilder(xmlOpen(KEY_OPENOPTICALDEV_XMLNS))
                .append(xmlOpen(KEY_PORTS))
                .append(xml(KEY_PORTID, Long.toString(port.toLong())))
                .append(xmlOpen(KEY_PORT))
                .append(xml(KEY_PORTTARPWR, Long.toString(power)))
                .append(xmlClose(KEY_PORT))
                .append(xmlClose(KEY_PORTS))
                .append(xmlClose(KEY_OPENOPTICALDEV))
                .toString();
        return netconfEditConfig(handler(), CFG_MODE_MERGE, cfg);
    }

    private boolean setChannelTargetPower(PortNumber port, OchSignal channel, long power) {
        log.debug("Set port{} channel{} attenuation.", port, channel.channelSpacing());
        FlowRuleService service = handler().get(FlowRuleService.class);
        Iterable<FlowEntry> entries = service.getFlowEntries(data().deviceId());
        for (FlowEntry entry : entries) {
            OplinkCrossConnect crossConnect  = OplinkOpticalUtility.fromFlowRule(this, entry);
            // The channel port might be input port or output port.
            if ((port.equals(crossConnect.getInPort()) || port.equals(crossConnect.getOutPort())) &&
                    channel.spacingMultiplier() == crossConnect.getChannel()) {
                log.debug("Flow is found, modify the flow with attenuation.");
                // Modify attenuation in treatment
                TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                        .setOutput(crossConnect.getOutPort())
                        .extension(new OplinkAttenuation((int) power), data().deviceId())
                        .build();
                // Apply the new flow rule
                service.applyFlowRules(DefaultFlowRule.builder()
                        .forDevice(data().deviceId())
                        .makePermanent()
                        .withSelector(entry.selector())
                        .withTreatment(treatment)
                        .withPriority(entry.priority())
                        .withCookie(entry.id().value())
                        .build());
                return true;
            }
        }
        return false;
    }

    private Range<Long> getPowerRange(PortNumber port, String directionKey, String minKey, String maxKey) {
        // TODO
        // Optical protection switch does not support power range configuration, it'll reply error.
        // To prevent replying error log flooding from netconf session when polling all ports information,
        // use general power range of [-60, 60] instead.
        if (handler().get(DeviceService.class).getDevice(data().deviceId()).type()
                == Device.Type.FIBER_SWITCH) {
            return RANGE_GENERAL;
        }
        String reply = netconfGet(handler(), getPowerRangeFilter(port, directionKey));
        HierarchicalConfiguration info = configAt(reply, KEY_PORTS_PORT_PROPERTY);
        if (info == null) {
            return null;
        }
        long minPower = (long) (info.getDouble(minKey) * POWER_MULTIPLIER);
        long maxPower = (long) (info.getDouble(maxKey) * POWER_MULTIPLIER);
        return Range.closed(minPower, maxPower);
    }

    private Range<Long> getTxPowerRange(PortNumber port, T component) {
        if (component instanceof Direction) {
            log.debug("Get target port{} power range...", port);
            return getPowerRange(port, KEY_PORTDIRECT_TX, KEY_PORTPWRCAPMINTX, KEY_PORTPWRCAPMAXTX);
        } else {
            log.debug("Get channel attenuation range...");
            return RANGE_ATT;
        }
    }

    private Range<Long> getRxPowerRange(PortNumber port, T component) {
        log.debug("Get input port{} power range...", port);
        return getPowerRange(port, KEY_PORTDIRECT_RX, KEY_PORTPWRCAPMINRX, KEY_PORTPWRCAPMAXRX);
    }
}
