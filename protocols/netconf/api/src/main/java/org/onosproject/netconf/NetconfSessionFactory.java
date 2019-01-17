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

package org.onosproject.netconf;

/**
 * Abstract interface for the creation of a NETCONF session.
 */
@FunctionalInterface
public interface NetconfSessionFactory {

    /**
     * Creates a new NETCONF session for the specified device.
     * @param netconfDeviceInfo information of the device to create the session for.
     * @param netconfController netconf controller object
     * @return Instance of NetconfSession.
     * @throws NetconfException when problems arise establishing the connection.
     */
    NetconfSession createNetconfSession(NetconfDeviceInfo netconfDeviceInfo,
                                        NetconfController netconfController)
            throws NetconfException;
}
