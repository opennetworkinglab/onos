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
package org.onosproject.ofagent.impl;

import com.google.common.base.MoreObjects;
import org.onlab.packet.IpAddress;
import org.onlab.packet.TpPort;
import org.onosproject.ofagent.api.OFController;

import java.util.Objects;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Implementation of the default OpenFlow controller.
 */
public final class DefaultOFController implements OFController {

    private final IpAddress ip;
    private final TpPort port;

    private DefaultOFController(IpAddress ip, TpPort port) {
        this.ip = ip;
        this.port = port;
    }

    /**
     * Returns new OpenFlow controller with the supplied IP and port.
     *
     * @param ip   ip address
     * @param port port number
     * @return openflow controller
     */
    public static DefaultOFController of(IpAddress ip, TpPort port) {
        checkNotNull(ip, "Controller IP address cannot be null");
        checkNotNull(port, "Controller port address cannot be null");
        return new DefaultOFController(ip, port);
    }

    @Override
    public IpAddress ip() {
        return ip;
    }

    @Override
    public TpPort port() {
        return port;
    }

    @Override
    public int hashCode() {
        return Objects.hash(ip, port);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof DefaultOFController) {
            DefaultOFController that = (DefaultOFController) obj;
            if (Objects.equals(ip, that.ip) &&
                    Objects.equals(port, that.port)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("ip", this.ip)
                .add("port", this.port)
                .toString();
    }
}
