/*
 * Copyright 2015-present Open Networking Laboratory
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
package org.onosproject.provider.pcep.tunnel.impl;

import java.util.Objects;

import org.onosproject.incubator.net.tunnel.Tunnel;
import org.onosproject.net.ElementId;
import org.onosproject.net.Path;
import org.onosproject.pcepio.types.StatefulIPv4LspIdentifiersTlv;

import com.google.common.base.MoreObjects;

/**
 * To store all tunnel related information from Core and Path computation client.
 */
public class PcepTunnelData {

    private Tunnel tunnel;
    private Path path;
    private int plspId;
    private ElementId elementId;
    private RequestType requestType;
    private boolean rptFlag;

    // data need to store from LSP object
    private boolean lspAFlag;
    private boolean lspDFlag;
    private byte lspOFlag;
    private short tunnelId;
    private int extTunnelId;
    private short lspId;
    private StatefulIPv4LspIdentifiersTlv statefulIpv4IndentifierTlv;

    /**
     * Default constructor.
     */
    public PcepTunnelData() {
        this.elementId = null;
        this.tunnel = null;
        this.path = null;
        this.requestType = null;
        this.rptFlag = false;
        this.plspId = 0;
    }

    /**
     * Constructor to initialize Tunnel, Path and Request type.
     *
     * @param tunnel mpls tunnel
     * @param path Path in network
     * @param requestType request type for tunnel
     */
    public PcepTunnelData(Tunnel tunnel, Path path, RequestType requestType) {
        this.tunnel = tunnel;
        this.path = path;
        this.requestType = requestType;
    }

    /**
     * Constructor to initialize ElemendId, Tunnel, Path and Request type.
     *
     * @param elementId Ip element id
     * @param tunnel mpls tunnel
     * @param path Path in network
     * @param requestType request type for tunnel
     */
    public PcepTunnelData(ElementId elementId, Tunnel tunnel, Path path, RequestType requestType) {
        this.elementId = elementId;
        this.tunnel = tunnel;
        this.path = path;
        this.requestType = requestType;
    }

    /**
     * Constructor to initialize Tunnel and Request type.
     *
     * @param tunnel Tunnel from core
     * @param requestType request type for tunnel
     */
    public PcepTunnelData(Tunnel tunnel, RequestType requestType) {
        this.tunnel = tunnel;
        this.requestType = requestType;
    }

    /**
     * Constructor to initialize ElementId, Tunnel and Request type.
     *
     * @param elementId Ip element id
     * @param tunnel mpls tunnel
     * @param requestType request type for tunnel
     */
    public PcepTunnelData(ElementId elementId, Tunnel tunnel, RequestType requestType) {
        this.elementId = elementId;
        this.tunnel = tunnel;
        this.requestType = requestType;
    }

    /**
     * Sets ip element id.
     *
     * @param elementId Ip element id
     */
    public void setElementId(ElementId elementId) {
        this.elementId = elementId;
    }

    /**
     * Sets tunnel.
     *
     * @param tunnel mpls tunnel
     */
    public void setTunnel(Tunnel tunnel) {
        this.tunnel = tunnel;
    }

    /**
     * Sets Path.
     *
     * @param path Path in network
     */
    public void setPath(Path path) {
        this.path = path;
    }

    /**
     * Request type for tunnel.
     *
     * @param requestType request type for tunnel
     */
    public void setRequestType(RequestType requestType) {
        this.requestType = requestType;
    }

    /**
     * Sets plspid generated from pcc.
     *
     * @param plspId plsp identifier
     */
    public void setPlspId(int plspId) {
        this.plspId = plspId;
    }

    /**
     * Sets A flag from lsp object.
     *
     * @param value A flag value
     */
    public void setLspAFlag(boolean value) {
        this.lspAFlag = value;
    }

    /**
     * Sets OF flag from lsp object.
     *
     * @param value OF flag value
     */
    public void setLspOFlag(byte value) {
        this.lspOFlag = value;
    }

    /**
     * Sets tunnel id from PCC.
     *
     * @param value tunnel id value
     */
    public void setTunnelId(short value) {
        this.tunnelId = value;
    }

    /**
     * Sets extended tunnel id from PCC.
     *
     * @param value extended tunnel id value
     */
    public void setExtTunnelId(int value) {
        this.extTunnelId = value;
    }

    /**
     * Sets lsp id from pcc.
     *
     * @param value lsp id
     */
    public void setLspId(short value) {
        this.lspId = value;
    }

    /**
     * Sets statefulIpv4Identifiers tlv.
     * @param value statefulIpv4Identifiers tlv
     */
    public void setStatefulIpv4IndentifierTlv(StatefulIPv4LspIdentifiersTlv value) {
        this.statefulIpv4IndentifierTlv = value;
    }

    /**
     * Sets report flag.
     *
     * @param rptFlag report flag
     */
    public void setRptFlag(boolean rptFlag) {
        this.rptFlag = rptFlag;
    }

    /**
     * Sets D flag from lsp object.
     *
     * @param value D flag value
     */
    public void setLspDFlag(boolean value) {
        this.lspDFlag = value;
    }

    /**
     * To get Ip element id.
     *
     * @return Ip elemend id
     */
    public ElementId elementId() {
        return this.elementId;
    }

    /**
     * To get Tunnel.
     *
     * @return tunnel
     */
    public Tunnel tunnel() {
        return this.tunnel;
    }

    /**
     * To get Path.
     *
     * @return path
     */
    public Path path() {
        return this.path;
    }

    /**
     * To get request type.
     *
     * @return request type
     */
    public RequestType requestType() {
        return this.requestType;
    }

    /**
     * To get pLspId.
     *
     * @return pLspId
     */
    public int plspId() {
        return this.plspId;
    }

    /**
     * To get A flag.
     *
     * @return A flag
     */
    public boolean lspAFlag() {
        return this.lspAFlag;
    }

    /**
     * To get OF flag.
     *
     * @return OF flag
     */
    public byte lspOFlag() {
        return this.lspOFlag;
    }

    /**
     * To get tunnel id.
     *
     * @return tunnel id
     */
    public short tunnelId() {
        return this.tunnelId;
    }

    /**
     * To get extended tunnel id.
     *
     * @return extended tunnel id
     */
    public int extTunnelId() {
        return this.extTunnelId;
    }

    /**
     * To get pLspId.
     *
     * @return pLspId
     */
    public short lspId() {
        return this.lspId;
    }

    /**
     * To get D Flag.
     *
     * @return d flag
     */
    public boolean lspDFlag() {
        return this.lspDFlag;
    }

    /**
     * To get statefulIpv4Indentifier tlv.
     *
     * @return statefulIpv4Indentifier tlv
     */
    public StatefulIPv4LspIdentifiersTlv statefulIpv4IndentifierTlv() {
        return this.statefulIpv4IndentifierTlv;
    }

    /**
     * To get report flag.
     *
     * @return report flag
     */
    public boolean rptFlag() {
        return this.rptFlag;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof PcepTunnelData) {
            PcepTunnelData other = (PcepTunnelData) obj;
            return Objects.equals(tunnel, other.tunnel)
                    && Objects.equals(path, other.path)
                    && Objects.equals(plspId, other.plspId)
                    && Objects.equals(elementId, other.elementId)
                    && Objects.equals(requestType, other.requestType)
                    && Objects.equals(rptFlag, other.rptFlag)
                    && Objects.equals(lspAFlag, other.lspAFlag)
                    && Objects.equals(lspDFlag, other.lspDFlag)
                    && Objects.equals(lspOFlag, other.lspOFlag)
                    && Objects.equals(tunnelId, other.tunnelId)
                    && Objects.equals(extTunnelId, other.extTunnelId)
                    && Objects.equals(lspId, other.lspId)
                    && Objects.equals(statefulIpv4IndentifierTlv, other.statefulIpv4IndentifierTlv);
        }

        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(tunnel, path, plspId, elementId, requestType, rptFlag, lspAFlag,
                            lspDFlag, lspOFlag, tunnelId, extTunnelId, lspId, statefulIpv4IndentifierTlv);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass()).add("Tunnel", tunnel)
                .add("Path", path).add("PlspId", plspId).add("ElementId", elementId)
                .add("RequestType", requestType).add("RptFlag", rptFlag).add("LspAFlag", lspAFlag)
                .add("LspDFlag", lspDFlag).add("LspOFlag", lspOFlag).add("TunnelId", tunnelId)
                .add("ExtTunnelid", extTunnelId).add("LspId", lspId)
                .add("StatefulIpv4IndentifierTlv", statefulIpv4IndentifierTlv).toString();
    }
}
