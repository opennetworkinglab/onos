/*
 * Copyright 2015-2016 Open Networking Laboratory
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
import org.onlab.packet.TpPort;
import org.onosproject.net.DeviceId;

import java.util.Comparator;
import java.util.Objects;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Representation of a compute infrastructure node for CORD VTN service.
 */
public final class CordVtnNode {

    private final String hostname;
    private final NetworkAddress hostMgmtIp;
    private final NetworkAddress localMgmtIp;
    private final NetworkAddress dpIp;
    private final TpPort ovsdbPort;
    private final SshAccessInfo sshInfo;
    private final DeviceId bridgeId;
    private final String dpIntf;
    private final CordVtnNodeState state;

    public static final Comparator<CordVtnNode> CORDVTN_NODE_COMPARATOR =
            (node1, node2) -> node1.hostname().compareTo(node2.hostname());

    /**
     * Creates a new node.
     *
     * @param hostname hostname
     * @param hostMgmtIp host management network address
     * @param localMgmtIp local management network address
     * @param dpIp data plane network address
     * @param ovsdbPort port number for OVSDB connection
     * @param sshInfo SSH access information
     * @param bridgeId integration bridge identifier
     * @param dpIntf data plane interface name
     * @param state cordvtn node state
     */
    public CordVtnNode(String hostname, NetworkAddress hostMgmtIp, NetworkAddress localMgmtIp,
                       NetworkAddress dpIp, TpPort ovsdbPort, SshAccessInfo sshInfo,
                       DeviceId bridgeId, String dpIntf, CordVtnNodeState state) {
        this.hostname = checkNotNull(hostname, "hostname cannot be null");
        this.hostMgmtIp = checkNotNull(hostMgmtIp, "hostMgmtIp cannot be null");
        this.localMgmtIp = checkNotNull(localMgmtIp, "localMgmtIp cannot be null");
        this.dpIp = checkNotNull(dpIp, "dpIp cannot be null");
        this.ovsdbPort = checkNotNull(ovsdbPort, "ovsdbPort cannot be null");
        this.sshInfo = checkNotNull(sshInfo, "sshInfo cannot be null");
        this.bridgeId = checkNotNull(bridgeId, "bridgeId cannot be null");
        this.dpIntf = checkNotNull(dpIntf, "dpIntf cannot be null");
        this.state = state;
    }

    /**
     * Returns cordvtn node with new state.
     *
     * @param node cordvtn node
     * @param state cordvtn node init state
     * @return cordvtn node
     */
    public static CordVtnNode getUpdatedNode(CordVtnNode node, CordVtnNodeState state) {
        return new CordVtnNode(node.hostname,
                               node.hostMgmtIp, node.localMgmtIp, node.dpIp,
                               node.ovsdbPort,
                               node.sshInfo,
                               node.bridgeId,
                               node.dpIntf, state);
    }

    /**
     * Returns the hostname.
     *
     * @return hostname
     */
    public String hostname() {
        return this.hostname;
    }

    /**
     * Returns the host management network address.
     *
     * @return network address
     */
    public NetworkAddress hostMgmtIp() {
        return this.hostMgmtIp;
    }

    /**
     * Returns the local management network address.
     *
     * @return network address
     */
    public NetworkAddress localMgmtIp() {
        return this.localMgmtIp;
    }

    /**
     * Returns the data plane network address.
     *
     * @return network address
     */
    public NetworkAddress dpIp() {
        return this.dpIp;
    }

    /**
     * Returns the port number used for OVSDB connection.
     *
     * @return port number
     */
    public TpPort ovsdbPort() {
        return this.ovsdbPort;
    }

    /**
     * Returns the SSH access information.
     *
     * @return ssh access information
     */
    public SshAccessInfo sshInfo() {
        return this.sshInfo;
    }

    /**
     * Returns the identifier of the integration bridge.
     *
     * @return device id
     */
    public DeviceId intBrId() {
        return this.bridgeId;
    }

    /**
     * Returns the identifier of the OVSDB device.
     *
     * @return device id
     */
    public DeviceId ovsdbId() {
        return DeviceId.deviceId("ovsdb:" + this.hostMgmtIp.ip().toString());
    }

    /**
     * Returns data plane interface name.
     *
     * @return data plane interface name
     */
    public String dpIntf() {
        return this.dpIntf;
    }

    /**
     * Returns the state of the node.
     *
     * @return state
     */
    public CordVtnNodeState state() {
        return this.state;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        // hostname here is a network hostname and it is intended to be
        // unique throughout the service.
        if (obj instanceof CordVtnNode) {
            CordVtnNode that = (CordVtnNode) obj;
            if (Objects.equals(hostname, that.hostname)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(hostname);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("hostname", hostname)
                .add("hostMgmtIp", hostMgmtIp)
                .add("localMgmtIp", localMgmtIp)
                .add("dpIp", dpIp)
                .add("port", ovsdbPort)
                .add("sshInfo", sshInfo)
                .add("bridgeId", bridgeId)
                .add("dpIntf", dpIntf)
                .add("state", state)
                .toString();
    }
}
