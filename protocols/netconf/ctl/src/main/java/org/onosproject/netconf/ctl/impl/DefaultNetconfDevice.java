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

package org.onosproject.netconf.ctl.impl;

import org.onosproject.netconf.NetconfDevice;
import org.onosproject.netconf.NetconfDeviceInfo;
import org.onosproject.netconf.NetconfException;
import org.onosproject.netconf.NetconfSession;
import org.onosproject.netconf.NetconfSessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Default implementation of a NETCONF device.
 */
public class DefaultNetconfDevice implements NetconfDevice {

    public static final Logger log = LoggerFactory
            .getLogger(DefaultNetconfDevice.class);

    private NetconfDeviceInfo netconfDeviceInfo;
    private boolean deviceState = true;
    private final NetconfSessionFactory sessionFactory;
    private NetconfSession netconfSession;

    // will block until hello RPC handshake completes
    /**
     * Creates a new default NETCONF device with the information provided.
     * The device gets created only if no exception is thrown while connecting to
     * it and establishing the NETCONF session.
     * @param deviceInfo information about the device to be created.
     * @throws NetconfException if there are problems in creating or establishing
     * the underlying NETCONF connection and session.
     */
    public DefaultNetconfDevice(NetconfDeviceInfo deviceInfo)
            throws NetconfException {
        netconfDeviceInfo = deviceInfo;
        sessionFactory = new NetconfSessionMinaImpl.MinaSshNetconfSessionFactory();
        try {
            netconfSession = sessionFactory.createNetconfSession(deviceInfo);
        } catch (IOException e) {
            deviceState = false;
            throw new NetconfException("Cannot create connection and session for device " +
                                               deviceInfo, e);
        }
    }

    // will block until hello RPC handshake completes
    /**
     * Creates a new default NETCONF device with the information provided.
     * The device gets created only if no exception is thrown while connecting to
     * it and establishing the NETCONF session.
     *
     * @param deviceInfo information about the device to be created.
     * @param factory the factory used to create the session
     * @throws NetconfException if there are problems in creating or establishing
     * the underlying NETCONF connection and session.
     */
    public DefaultNetconfDevice(NetconfDeviceInfo deviceInfo, NetconfSessionFactory factory)
            throws NetconfException {
        netconfDeviceInfo = deviceInfo;
        sessionFactory = factory;
        try {
            netconfSession = sessionFactory.createNetconfSession(deviceInfo);
        } catch (IOException e) {
            deviceState = false;
            throw new NetconfException("Cannot create connection and session for device " +
                                               deviceInfo, e);
        }
    }

    @Override
    public boolean isActive() {
        return deviceState;
    }

    @Override
    public NetconfSession getSession() {
        return netconfSession;
    }

    @Override
    public void disconnect() {
        deviceState = false;
        try {
            netconfSession.close();
        } catch (IOException e) {
            log.warn("Cannot communicate with the device {} session already closed", netconfDeviceInfo);
        }
    }

    @Override
    public NetconfDeviceInfo getDeviceInfo() {
        return netconfDeviceInfo;
    }


}
