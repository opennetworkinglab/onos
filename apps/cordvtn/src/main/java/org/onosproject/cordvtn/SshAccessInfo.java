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
package org.onosproject.cordvtn;

import com.google.common.base.MoreObjects;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.TpPort;

import java.util.Objects;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Representation of SSH access information.
 */
public final class SshAccessInfo {

    private final Ip4Address remoteIp;
    private final TpPort port;
    private final String user;
    private final String privateKey;

    /**
     * Creates a new SSH access information.
     *
     * @param remoteIp ssh remote ip address
     * @param port ssh port number
     * @param user user name
     * @param privateKey path of ssh private key
     */
    public SshAccessInfo(Ip4Address remoteIp, TpPort port, String user, String privateKey) {
        this.remoteIp = checkNotNull(remoteIp);
        this.port = checkNotNull(port);
        this.user = checkNotNull(user);
        this.privateKey = checkNotNull(privateKey);
    }

    /**
     * Returns the remote IP address.
     *
     * @return ip address
     */
    public Ip4Address remoteIp() {
        return this.remoteIp;
    }

    /**
     * Returns the port number.
     *
     * @return ssh port
     */
    public TpPort port() {
        return this.port;
    }

    /**
     * Returns the user name.
     *
     * @return user name
     */
    public String user() {
        return this.user;
    }

    /**
     * Returns the private key path.
     *
     * @return privateKey
     */
    public String privateKey() {
        return privateKey;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof SshAccessInfo) {
            SshAccessInfo that = (SshAccessInfo) obj;
            if (Objects.equals(remoteIp, that.remoteIp) &&
                    Objects.equals(port, that.port) &&
                    Objects.equals(user, that.user) &&
                    Objects.equals(privateKey, that.privateKey)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(remoteIp, port, user, privateKey);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("remoteIp", remoteIp)
                .add("port", port)
                .add("user", user)
                .add("privateKey", privateKey)
                .toString();
    }
}
