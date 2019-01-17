/*
 * Copyright 2016-present Open Networking Foundation
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

import org.onosproject.netconf.NetconfController;
import org.onosproject.netconf.NetconfDevice;
import org.onosproject.netconf.NetconfDeviceInfo;
import org.onosproject.netconf.NetconfException;
import org.onosproject.netconf.NetconfSession;
import org.onosproject.netconf.NetconfSessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default implementation of a NETCONF device.
 */
public class DefaultNetconfDevice implements NetconfDevice {

    public static final Logger log = LoggerFactory
            .getLogger(DefaultNetconfDevice.class);

    private NetconfDeviceInfo netconfDeviceInfo;
    private boolean deviceState = true;
    private NetconfSession netconfSession;
    private boolean isMasterSession = false;
    private NetconfSession netconfProxySession;

    // will block until hello RPC handshake completes
    /**
     * Creates a new default NETCONF device with the information provided.
     * The device gets created only if no exception is thrown while connecting to
     * it and establishing the NETCONF session.
     * The secure transport session will only be created if isMaster is true.
     * @param deviceInfo information about the device to be created.
     * @param isMaster if true create secure transport session, otherwise create proxy session.
     * @param netconfController netconf controller object
     * @throws NetconfException if there are problems in creating or establishing
     * the underlying NETCONF connection and session.
     */
    public DefaultNetconfDevice(NetconfDeviceInfo deviceInfo,
                                boolean isMaster,
                                NetconfController netconfController)
            throws NetconfException {
        netconfDeviceInfo = deviceInfo;
        try {
            if (isMaster) {
                netconfSession = new NetconfSessionMinaImpl(deviceInfo);
                isMasterSession = true;
                netconfProxySession = netconfSession;
            } else {
                netconfProxySession = new NetconfSessionProxyImpl
                        .ProxyNetconfSessionFactory()
                        .createNetconfSession(deviceInfo, netconfController);
            }
        } catch (NetconfException e) {
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
     * The secure transport session will only be created if isMaster is true.
     * @param deviceInfo information about the device to be created.
     * @param factory the factory used to create the session
     * @param isMaster if true create secure transport session, otherwise create proxy session.
     * @param netconfController netconf controller object
     * @throws NetconfException if there are problems in creating or establishing
     * the underlying NETCONF connection and session.
     */
    public DefaultNetconfDevice(NetconfDeviceInfo deviceInfo,
                                NetconfSessionFactory factory,
                                boolean isMaster,
                                NetconfController netconfController)
            throws NetconfException {
        netconfDeviceInfo = deviceInfo;
        try {
            if (isMaster) {
                netconfSession = factory.createNetconfSession(deviceInfo, netconfController);
                isMasterSession = true;
                netconfProxySession = netconfSession;
            } else {
                netconfProxySession = new NetconfSessionProxyImpl
                        .ProxyNetconfSessionFactory()
                        .createNetconfSession(deviceInfo, netconfController);
            }
        } catch (NetconfException e) {
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
        return netconfProxySession;
    }

    @Override
    public void disconnect() {
        deviceState = false;
        try {
            if (isMasterSession) {
                netconfSession.close();
            }
            netconfProxySession.close();
        } catch (NetconfException e) {
            log.warn("Cannot communicate with the device {} session already closed", netconfDeviceInfo);
        }
    }

    @Override
    public boolean isMasterSession() {
        return isMasterSession;
    }

    @Override
    public NetconfDeviceInfo getDeviceInfo() {
        return netconfDeviceInfo;
    }
}
