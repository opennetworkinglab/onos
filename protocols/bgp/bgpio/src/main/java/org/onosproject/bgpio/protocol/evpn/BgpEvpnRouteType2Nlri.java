/*
 * Copyright 2017-present Open Networking Foundation
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

package org.onosproject.bgpio.protocol.evpn;

import com.google.common.base.MoreObjects;
import org.jboss.netty.buffer.ChannelBuffer;
import org.onlab.packet.MacAddress;
import org.onosproject.bgpio.exceptions.BgpParseException;
import org.onosproject.bgpio.types.BgpEvpnEsi;
import org.onosproject.bgpio.types.BgpEvpnLabel;
import org.onosproject.bgpio.types.RouteDistinguisher;
import org.onosproject.bgpio.util.Constants;
import org.onosproject.bgpio.util.Validation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;

public class BgpEvpnRouteType2Nlri implements BgpEvpnNlriData {

    /*
     * REFERENCE : RFC 7432 BGP MPLS-Based Ethernet VPN
         +---------------------------------------+
         | RD (8 octets) |
         +---------------------------------------+
         |Ethernet Segment Identifier (10 octets)|
         +---------------------------------------+
         | Ethernet Tag ID (4 octets) |
         +---------------------------------------+
         | MAC Address Length (1 octet) |
         +---------------------------------------+
         | MAC Address (6 octets) |
         +---------------------------------------+
         | IP Address Length (1 octet) |
         +---------------------------------------+
         | IP Address (0, 4, or 16 octets) |
         +---------------------------------------+
         | MPLS Label1 (3 octets) |
         +---------------------------------------+
         | MPLS Label2 (0 or 3 octets) |
         +---------------------------------------+

      Figure : A MAC/IP Advertisement route type specific EVPN NLRI

     */

    public static final short TYPE = Constants.BGP_EVPN_MAC_IP_ADVERTISEMENT;
    private static final Logger log = LoggerFactory.getLogger(BgpEvpnRouteType2Nlri.class);
    // unit of length is bit
    public static final short IPV4_ADDRESS_LENGTH = 32;
    public static final short MAC_ADDRESS_LENGTH = 48;
    private RouteDistinguisher rd;
    private BgpEvpnEsi esi;
    private int ethernetTagID;
    private byte macAddressLength;
    private MacAddress macAddress;
    private byte ipAddressLength;
    private InetAddress ipAddress;
    private BgpEvpnLabel mplsLabel1;
    private BgpEvpnLabel mplsLabel2;

    /**
     * Resets parameters.
     */
    public BgpEvpnRouteType2Nlri() {
        this.rd = null;
        this.esi = null;
        this.ethernetTagID = 0;
        this.macAddressLength = 0;
        this.macAddress = null;
        this.ipAddressLength = 0;
        this.ipAddress = null;
        this.mplsLabel1 = null;
        this.mplsLabel2 = null;
    }

    /**
     * Creates the Evpn route type 2 route.
     *
     * @param rd            route distinguisher
     * @param esi           esi
     * @param ethernetTagID ethernet tag id
     * @param macAddress    mac
     * @param ipAddress     ip
     * @param mplsLabel1    label
     * @param mplsLabel2    label
     */
    public BgpEvpnRouteType2Nlri(RouteDistinguisher rd,
                                 BgpEvpnEsi esi,
                                 int ethernetTagID, MacAddress macAddress,
                                 InetAddress ipAddress, BgpEvpnLabel mplsLabel1,
                                 BgpEvpnLabel mplsLabel2) {
        this.rd = rd;
        this.esi = esi;
        this.ethernetTagID = ethernetTagID;
        this.macAddressLength = MAC_ADDRESS_LENGTH;
        this.macAddress = macAddress;
        if (ipAddress != null) {
            this.ipAddressLength = IPV4_ADDRESS_LENGTH;
            this.ipAddress = ipAddress;
        } else {
            this.ipAddressLength = 0;
            this.ipAddress = null;
        }
        this.mplsLabel1 = mplsLabel1;
        this.mplsLabel2 = mplsLabel2;
    }

    /**
     * Reads the Evpn type 2 attributes.
     *
     * @param cb channel buffer
     * @return type2 route
     * @throws BgpParseException parse exception
     */
    public static BgpEvpnRouteType2Nlri read(ChannelBuffer cb) throws BgpParseException {
        if (cb.readableBytes() == 0) {
            return null;
        }
        RouteDistinguisher rd = RouteDistinguisher.read(cb);
        BgpEvpnEsi esi = BgpEvpnEsi.read(cb);
        int ethernetTagID = cb.readInt();
        byte macAddressLength = cb.readByte();
        MacAddress macAddress = Validation.toMacAddress(macAddressLength / 8, cb);
        byte ipAddressLength = cb.readByte();
        InetAddress ipAddress = null;
        if (ipAddressLength > 0) {
            ipAddress = Validation.toInetAddress(ipAddressLength / 8, cb);
        }
        BgpEvpnLabel mplsLabel1 = BgpEvpnLabel.read(cb);
        BgpEvpnLabel mplsLabel2 = null;
        if (cb.readableBytes() > 0) {
            mplsLabel2 = BgpEvpnLabel.read(cb);
        }

        return new BgpEvpnRouteType2Nlri(rd, esi, ethernetTagID, macAddress,
                                         ipAddress, mplsLabel1,
                                         mplsLabel2);
    }

    @Override
    public int write(ChannelBuffer cb) {
        int iLenStartIndex = cb.writerIndex();
        cb.writeLong(rd.getRouteDistinguisher());
        esi.write(cb);
        cb.writeInt(ethernetTagID);
        cb.writeByte(macAddressLength);
        cb.writeBytes(macAddress.toBytes());
        cb.writeByte(ipAddressLength);
        if (ipAddressLength > 0) {
            cb.writeBytes(ipAddress.getAddress());
        }
        mplsLabel1.write(cb);
        if (mplsLabel2 != null) {
            mplsLabel2.write(cb);
        }
        return cb.writerIndex() - iLenStartIndex;
    }

    /**
     * Returns the rd.
     *
     * @return rd route distinguisher
     */
    public RouteDistinguisher getRouteDistinguisher() {
        return rd;
    }

    /**
     * Returns the esi.
     *
     * @return esi ethernet segment identifier
     */
    public BgpEvpnEsi getEthernetSegmentidentifier() {
        return esi;
    }

    /**
     * Returns the ethernet tag id.
     *
     * @return macAddress macadress
     */
    public int getEthernetTagID() {
        return ethernetTagID;
    }

    public MacAddress getMacAddress() {
        return macAddress;
    }

    /**
     * Returns the ip address.
     *
     * @return ipAddress ipaddress
     */
    public InetAddress getIpAddress() {
        return ipAddress;
    }

    /**
     * Returns the mpls label.
     *
     * @return mplsLabel1 mpls label
     */
    public BgpEvpnLabel getMplsLable1() {
        return mplsLabel1;
    }

    /**
     * Returns the mpls label.
     *
     * @return mplsLabel2 mpls label
     */
    public BgpEvpnLabel getMplsLable2() {
        return mplsLabel2;
    }

    /**
     * Set the rd.
     *
     * @param rd route distinguisher
     */
    public void setRouteDistinguisher(RouteDistinguisher rd) {
        this.rd = rd;
    }

    /**
     * Set the ESI.
     *
     * @param esi esi
     */
    public void setEthernetSegmentidentifier(BgpEvpnEsi esi) {
        this.esi = esi;
    }

    /**
     * Set the ethernet tag id.
     *
     * @param ethernetTagID ethernet tag id.
     */
    public void setEthernetTagID(int ethernetTagID) {
        this.ethernetTagID = ethernetTagID;
    }

    /**
     * Set the mac address.
     *
     * @param macAddress mac
     */
    public void setMacAddress(MacAddress macAddress) {
        this.macAddress = macAddress;
    }

    /**
     * Set the ip address.
     *
     * @param ipAddress ip
     */
    public void setIpAddress(InetAddress ipAddress) {
        this.ipAddress = ipAddress;
    }

    /**
     * Set the mpls label.
     *
     * @param mplsLabel1 label
     */
    public void setMplsLable1(BgpEvpnLabel mplsLabel1) {
        this.mplsLabel1 = mplsLabel1;
    }

    /**
     * Set the mpls label.
     *
     * @param mplsLabel2 label
     */
    public void setMplsLable2(BgpEvpnLabel mplsLabel2) {
        this.mplsLabel2 = mplsLabel2;
    }

    @Override
    public BgpEvpnRouteType getType() {
        return BgpEvpnRouteType.MAC_IP_ADVERTISEMENT;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass()).omitNullValues()
                .add("rd ", rd)
                .add("esi", esi)
                .add("ethernetTagID", ethernetTagID)
                .add("macAddressLength", macAddressLength)
                .add("macAddress ", macAddress)
                .add("ipAddressLength", ipAddressLength)
                .add("ipAddress", ipAddress)
                .add("mplsLabel1 ", mplsLabel1)
                .add("mplsLabel2", mplsLabel2).toString();
    }

}
