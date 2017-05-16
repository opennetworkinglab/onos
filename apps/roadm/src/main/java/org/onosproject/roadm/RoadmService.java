/*
 * Copyright 2016-present Open Networking Laboratory
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
package org.onosproject.roadm;

import com.google.common.collect.Range;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.OchSignal;
import org.onosproject.net.PortNumber;
import org.onosproject.net.behaviour.protection.ProtectedTransportEndpointState;
import org.onosproject.net.flow.FlowId;

import java.util.Map;
import java.util.Set;

/**
 * ROADM service interface. Provides an interface for ROADM power configuration.
 *
 * This application relies on the PowerConfig and LambdaQuery behaviours.
 *
 * The device's PowerConfig implementation should be parameterized as
 * {@code PowerConfig<Object>} in order to support both Direction and OchSignal.
 * For a reference implementation of PowerConfig, please see
 * OplinkRoadmPowerConfig
 *
 * In this application, a "connection" refers to the selection of a channel
 * to direct from an input to an output port. Connections are implemented
 * using FlowRules with an input port selector, optical channel selector,
 * and output port treatment (see RoadmManager#createConnection()).
 *
 * This application currently only supports fixed grid channels.
 */
public interface RoadmService {

    /**
     * Attempts to manually switch working path to the one specified by {@code index}.
     *
     * @param deviceId DeviceId of the device to configure
     * @param index working path index to switch to
     * @deprecated 1.11.0
     */
    @Deprecated
    void setProtectionSwitchWorkingPath(DeviceId deviceId, int index);

    /**
     * Retrieves protection switch specified port's service status.
     *
     * @param deviceId DeviceId of the device to configure
     * @param portNumber the port
     * @return port service status
     * @deprecated 1.11.0
     */
    @Deprecated
    String getProtectionSwitchPortState(DeviceId deviceId, PortNumber portNumber);

    /**
     * Attempts to config protection switch by specified {@code operation} and {@code index}.
     *
     * @param deviceId DeviceId of the device to configure
     * @param operation switch configuration, automatic, force or manual
     * @param identifier {@link ConnectPoint} for the virtual Port representing protected path endpoint
     * @param index working path index to switch to
     */
    void configProtectionSwitch(DeviceId deviceId, String operation, ConnectPoint identifier, int index);

    /**
     * Retrieves protection switch endpoint states.
     * @param deviceId DeviceId of the device to configure
     * @return map groups of underlying paths
     */
    Map<ConnectPoint, ProtectedTransportEndpointState> getProtectionSwitchStates(DeviceId deviceId);

    /**
     * Set target power for a port if the port has configurable target power.
     *
     * @param deviceId DeviceId of the device to configure
     * @param portNumber PortNumber of the port to configure
     * @param power value to set target power to
     */
    void setTargetPortPower(DeviceId deviceId, PortNumber portNumber, long power);

    /**
     * Returns the target power for a port if the port has configurable target power.
     *
     * @param deviceId DeviceId of the device to configure
     * @param portNumber PortNumber of the port to configure
     * @return the target power if the port has a target power, null otherwise
     */
    Long getTargetPortPower(DeviceId deviceId, PortNumber portNumber);

    /**
     * Sets the attenuation of a connection. This does not check that attenuation
     * is within the acceptable range.
     *
     * @param deviceId DeviceId of the device to configure
     * @param portNumber PortNumber of either the input or output port
     * @param ochSignal channel to set attenuation for
     * @param attenuation attenuation value to set to
     */
    void setAttenuation(DeviceId deviceId, PortNumber portNumber, OchSignal ochSignal, long attenuation);

    /**
     * Returns the attenuation of a connection.
     *
     * @param deviceId DeviceId of the device
     * @param portNumber PortNumber of either the input or output port
     * @param ochSignal channel to search for
     * @return attenuation if found, null otherwise
     */
    Long getAttenuation(DeviceId deviceId, PortNumber portNumber, OchSignal ochSignal);

    /**
     * Returns the current port power.
     *
     * @param deviceId DeviceId of the device
     * @param portNumber PortNumber of the port
     * @return current power if found, null otherwise
     */
    Long getCurrentPortPower(DeviceId deviceId, PortNumber portNumber);

    /**
     * Returns the current channel power.
     *
     * @param deviceId DeviceId of the device
     * @param portNumber PortNumber of either the input or output port of the connection
     * @param ochSignal channel to search for
     * @return channel power if found, null otherwise
     */
    Long getCurrentChannelPower(DeviceId deviceId, PortNumber portNumber, OchSignal ochSignal);

    /**
     * Returns the channels supported by a port.
     *
     * @param deviceId DeviceId of the device
     * @param portNumber PortNumber of the port
     * @return the set of supported channels
     */
    Set<OchSignal> queryLambdas(DeviceId deviceId, PortNumber portNumber);

    /**
     * Creates a new internal connection on a device without attenuation. This does
     * not check that the connection is actually valid (e.g. an input port to an
     * output port).
     *
     * Connections are represented as flows with an input port, output port, and
     * channel.
     *
     * @param deviceId DeviceId of the device to create this connection for
     * @param priority priority of the flow
     * @param isPermanent permanence of the flow
     * @param timeout timeout in seconds
     * @param inPort input port
     * @param outPort output port
     * @param ochSignal channel to use
     * @return FlowId of the FlowRule representing the connection
     */
    FlowId createConnection(DeviceId deviceId, int priority, boolean isPermanent,
                          int timeout, PortNumber inPort, PortNumber outPort, OchSignal ochSignal);

    /**
     * Creates a new internal connection on a device with attenuation. This does
     * not check that the connection is actually valid (e.g. an input port to an
     * output port, attenuation if within the acceptable range).
     *
     * Connections are represented as flows with an input port, output port, and
     * channel. Implementation of attenuation is up to the vendor.
     *
     * @param deviceId DeviceId of the device to create this connection for
     * @param priority priority of the flow
     * @param isPermanent permanence of the flow
     * @param timeout timeout in seconds
     * @param inPort input port
     * @param outPort output port
     * @param ochSignal channel to use
     * @param attenuation attenuation of the connection
     * @return FlowId of the FlowRule representing the connection
     */
    FlowId createConnection(DeviceId deviceId, int priority, boolean isPermanent,
                          int timeout, PortNumber inPort, PortNumber outPort,
                          OchSignal ochSignal, long attenuation);

    /**
     * Removes an internal connection from a device by matching the FlowId and
     * removing the flow representing the connection. This will remove any flow
     * from any device so FlowId should correspond with a connection flow.
     *
     * @param deviceId DeviceId of the device to remove the connection from
     * @param flowId FlowId of the flow representing the connection to remove
     */
    void removeConnection(DeviceId deviceId, FlowId flowId);

    /**
     * Returns true if the target power for this port can be configured.
     *
     * @param deviceId DeviceId of the device
     * @param portNumber PortNumber of the port to check
     * @return true if the target power for this port can be configured, false
     * otherwise
     */
    boolean hasPortTargetPower(DeviceId deviceId, PortNumber portNumber);

    /**
     * Returns true if value is within the acceptable target power range of the port.
     * Returns false if the port does not have a configurable target
     * power.
     *
     * @param deviceId DeviceId of the device to check
     * @param portNumber PortNumber of the port to check
     * @param power value to check
     * @return true if value is within the acceptable target power range, false
     * otherwise
     */
    boolean portTargetPowerInRange(DeviceId deviceId, PortNumber portNumber, long power);

    /**
     * Returns true if value is within the acceptable attenuation range of a
     * connection, and always returns false if the connection does not support
     * attenuation. The attenuation range is determined by either the input
     * or output port of the connection.
     *
     * @param deviceId DeviceId of the device to check
     * @param portNumber PortNumber of either the input or output port of the connection
     * @param att value to check
     * @return true if value is within the acceptable attenuation range, false
     * otherwise
     */
    boolean attenuationInRange(DeviceId deviceId, PortNumber portNumber, long att);

    /**
     * Returns true if the port is an input port.
     *
     * @param deviceId DeviceId of the device to check
     * @param portNumber PortNumber of the port to check
     * @return true if the port is an input port, false otherwise
     */
    boolean validInputPort(DeviceId deviceId, PortNumber portNumber);

    /**
     * Returns true if the port is an output port.
     *
     * @param deviceId DeviceId of the device to check
     * @param portNumber PortNumber of the port to check
     * @return true if the port is an output port, false otherwise
     */
    boolean validOutputPort(DeviceId deviceId, PortNumber portNumber);

    /**
     * Returns true if the channel is supported by the port. The port can be either
     * an input or output port.
     *
     * @param deviceId DeviceId of the device to check
     * @param portNumber PortNumber of the port to check
     * @param ochSignal channel to check
     * @return true if the channel is supported by the port, false otherwise
     */
    boolean validChannel(DeviceId deviceId, PortNumber portNumber, OchSignal ochSignal);

    /**
     * Returns true if the channel is not being used by a connection on the
     * device.
     *
     * @param deviceId DeviceId of the device to check
     * @param ochSignal channel to check
     * @return true if the channel is not in use, false otherwise
     */
    boolean channelAvailable(DeviceId deviceId, OchSignal ochSignal);

    /**
     * Returns true if the connection from the input port to the output port is
     * valid. This currently only checks if the given input and output ports are,
     * respectively, valid input and output ports.
     *
     * @param deviceId DeviceId of the device to check
     * @param inPort input port of the connection
     * @param outPort output port of the connection
     * @return true if the connection is valid, false otherwise
     */
    boolean validConnection(DeviceId deviceId, PortNumber inPort, PortNumber outPort);

    /**
     * Returns the acceptable target port power range for a port.
     *
     * @param deviceId DeviceId of the device
     * @param portNumber PortNumber of the port
     * @return range if found, null otherwise
     */
    Range<Long> targetPortPowerRange(DeviceId deviceId, PortNumber portNumber);

    /**
     * Returns the acceptable attenuation range for a connection.
     *
     * @param deviceId DeviceId of the device
     * @param portNumber PortNumber of either the input or output port
     * @param ochSignal channel to check
     * @return range if found, null otherwise
     */
    Range<Long> attenuationRange(DeviceId deviceId, PortNumber portNumber, OchSignal ochSignal);

    /**
     * Returns the expected input power range for an input port.
     *
     * @param deviceId DeviceId of the device
     * @param portNumber PortNumber of an input port
     * @return range if found, null otherwise
     */
    Range<Long> inputPortPowerRange(DeviceId deviceId, PortNumber portNumber);
}
