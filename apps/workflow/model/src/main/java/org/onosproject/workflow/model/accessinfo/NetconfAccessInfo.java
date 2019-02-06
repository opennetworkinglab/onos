/*
 * Copyright 2019-present Open Networking Foundation
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
package org.onosproject.workflow.model.accessinfo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NumericNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.base.MoreObjects;
import org.onlab.packet.IpAddress;
import org.onlab.packet.TpPort;
import org.onosproject.workflow.api.WorkflowException;

import java.util.Objects;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Class for NETCONF access information.
 */
public final class NetconfAccessInfo {

    private static final String REMOTE_IP = "remoteIp";
    private static final String PORT = "port";
    private static final String USER = "user";
    private static final String PASSWORD = "password";

    private final IpAddress remoteIp;
    private final TpPort port;
    private final String user;
    private final String password;

    /**
     * Constructor for a given NETCONF access information.
     *
     * @param remoteIp remote ip address
     * @param port port number
     * @param user user name
     * @param password password
     */
    public NetconfAccessInfo(IpAddress remoteIp,
                             TpPort port,
                             String user,
                             String password) {
        this.remoteIp = checkNotNull(remoteIp);
        this.port = checkNotNull(port);
        this.user = checkNotNull(user);
        this.password = checkNotNull(password);
    }

    /**
     * Builds NetconfAccessInfo from json.
     * @param root json root node for NetconfAccessinfo
     * @return NETCONF access information
     * @throws WorkflowException workflow exception
     */
    public static NetconfAccessInfo valueOf(JsonNode root) throws WorkflowException {

        JsonNode node = root.at(ptr(REMOTE_IP));
        if (node == null || !(node instanceof TextNode)) {
            throw new WorkflowException("invalid remoteIp for " + root);
        }
        IpAddress remoteIp = IpAddress.valueOf(node.asText());

        node = root.at(ptr(PORT));
        if (node == null || !(node instanceof NumericNode)) {
            throw new WorkflowException("invalid port for " + root);
        }
        TpPort tpPort = TpPort.tpPort(node.asInt());

        node = root.at(ptr(USER));
        if (node == null || !(node instanceof TextNode)) {
            throw new WorkflowException("invalid user for " + root);
        }
        String strUser = node.asText();

        node = root.at(ptr(PASSWORD));
        if (node == null || !(node instanceof TextNode)) {
            throw new WorkflowException("invalid password for " + root);
        }
        String strPassword = node.asText();

        return new NetconfAccessInfo(remoteIp, tpPort, strUser, strPassword);
    }

    private static String ptr(String field) {
        return "/" + field;
    }

    /**
     * Returns the remote IP address.
     *
     * @return ip address
     */
    public IpAddress remoteIp() {
        return this.remoteIp;
    }

    /**
     * Returns the port number.
     *
     * @return port
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
     * Returns the password.
     * @return password
     */
    public String password() {
        return this.password;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof NetconfAccessInfo) {
            NetconfAccessInfo that = (NetconfAccessInfo) obj;
            return Objects.equals(remoteIp, that.remoteIp) &&
                    Objects.equals(port, that.port) &&
                    Objects.equals(user, that.user);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(remoteIp, port, user);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("remoteIp", remoteIp)
                .add("port", port)
                .add("user", user)
                .toString();
    }
}