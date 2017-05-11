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

package org.onosproject.l3vpn.netl3vpn;

/**
 * Representation of exception that needs to be handled by net l3 VPN.
 */
public class NetL3VpnException extends RuntimeException {

    /**
     * Creates net l3 VPN exception with an exception message.
     *
     * @param excMsg message
     */
    public NetL3VpnException(String excMsg) {
        super(excMsg);
    }

    /**
     * Creates net l3 VPN exception with a cause for it.
     *
     * @param cause cause
     */
    public NetL3VpnException(Throwable cause) {
        super(cause);
    }
}
