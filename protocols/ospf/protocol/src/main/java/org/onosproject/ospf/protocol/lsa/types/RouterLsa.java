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
package org.onosproject.ospf.protocol.lsa.types;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.primitives.Bytes;
import org.jboss.netty.buffer.ChannelBuffer;
import org.onosproject.ospf.exceptions.OspfErrorType;
import org.onosproject.ospf.exceptions.OspfParseException;
import org.onosproject.ospf.protocol.lsa.LsaHeader;
import org.onosproject.ospf.protocol.lsa.subtypes.OspfLsaLink;
import org.onosproject.ospf.protocol.util.OspfUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

/**
 * Representation of a Router LSA, and the fields and methods to access them.
 */
public class RouterLsa extends LsaHeader {
    /*
        0                   1                   2                   3
        0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
       +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
       |            LS age             |     Options   |       1       |
       +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
       |                        Link State ID                          |
       +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
       |                     Advertising Router                        |
       +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
       |                     LS sequence number                        |
       +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
       |         LS checksum           |             length            |
       +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
       |    0    |V|E|B|        0      |            # links            |
       +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
       |                          Link ID                              |
       +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
       |                         Link Data                             |
       +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
       |     Type      |     # TOS     |            metric             |
       +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
       |                              ...                              |
       +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
       |      TOS      |        0      |          TOS  metric          |
       +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
       |                          Link ID                              |
       +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
       |                         Link Data                             |
       +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
       |                              ...                              |
     */
    private static final Logger log =
            LoggerFactory.getLogger(RouterLsa.class);
    private boolean isVirtualEndPoint;
    private boolean isAsBoundaryRouter;
    private boolean isAreaBorderRouter;
    private int noLink;
    private List<OspfLsaLink> routerLinks = new ArrayList<>();

    /**
     * Creates an instance of Router LSA.
     */
    public RouterLsa() {
    }

    /**
     * Creates an instance of Router LSA.
     *
     * @param lsaHeader lsa header instance
     */
    public RouterLsa(LsaHeader lsaHeader) {
        populateHeader(lsaHeader);
    }

    /**
     * Sets virtual endpoint or not.
     *
     * @param isVirtualEndPoint true or false
     */
    public void setVirtualEndPoint(boolean isVirtualEndPoint) {
        this.isVirtualEndPoint = isVirtualEndPoint;
    }

    /**
     * Sets if it is an AS boundary router or not.
     *
     * @param isAsBoundaryRouter true if AS boundary router else false
     */
    public void setAsBoundaryRouter(boolean isAsBoundaryRouter) {
        this.isAsBoundaryRouter = isAsBoundaryRouter;
    }

    /**
     * Sets whether it is an ABR or not.
     *
     * @param isAreaBorderRouter true if ABR else false
     */
    public void setAreaBorderRouter(boolean isAreaBorderRouter) {
        this.isAreaBorderRouter = isAreaBorderRouter;
    }

    /**
     * Gets number of links.
     *
     * @return number of links
     */
    public int noLink() {
        return noLink;
    }

    /**
     * Sets number of links.
     *
     * @param noLink number of links
     */
    public void setNoLink(int noLink) {
        this.noLink = noLink;
    }


    /**
     * Adds router link.
     *
     * @param lsaLink LSA link
     */
    public void addRouterLink(OspfLsaLink lsaLink) {
        if (!this.routerLinks.contains(lsaLink)) {
            this.routerLinks.add(lsaLink);
        }
    }

    /**
     * Gets router link.
     *
     * @return routerLinks LSA link list
     */
    public List<OspfLsaLink> routerLink() {
        return this.routerLinks;
    }

    /**
     * Reads from channel buffer and populate this.
     *
     * @param channelBuffer channelBuffer instance.
     * @throws OspfParseException might throws exception while parsing buffer
     */
    public void readFrom(ChannelBuffer channelBuffer) throws OspfParseException {

        try {
            int veb = channelBuffer.readByte();
            int unUsed = channelBuffer.readByte();
            //Convert the byte to veb bits
            String strVeb = Integer.toBinaryString(veb);
            if (strVeb.length() == 3) {
                this.setVirtualEndPoint((Integer.parseInt(Character.toString(strVeb.charAt(0))) == 1) ? true : false);
                this.setAsBoundaryRouter((Integer.parseInt(Character.toString(strVeb.charAt(1))) == 1) ? true : false);
                this.setAreaBorderRouter((Integer.parseInt(Character.toString(strVeb.charAt(2))) == 1) ? true : false);
            } else if (strVeb.length() == 2) {
                this.setVirtualEndPoint(false);
                this.setAsBoundaryRouter((Integer.parseInt(Character.toString(strVeb.charAt(0))) == 1) ? true : false);
                this.setAreaBorderRouter((Integer.parseInt(Character.toString(strVeb.charAt(1))) == 1) ? true : false);
            } else if (strVeb.length() == 1) {
                this.setVirtualEndPoint(false);
                this.setAsBoundaryRouter(false);
                this.setAreaBorderRouter((Integer.parseInt(Character.toString(strVeb.charAt(0))) == 1) ? true : false);
            }
            this.setNoLink(channelBuffer.readUnsignedShort());
            while (channelBuffer.readableBytes() >= OspfUtil.TWELVE_BYTES) {
                OspfLsaLink ospfLsaLink = new OspfLsaLink();

                byte[] tempByteArray = new byte[OspfUtil.FOUR_BYTES];
                channelBuffer.readBytes(tempByteArray, 0, OspfUtil.FOUR_BYTES);
                ospfLsaLink.setLinkId(InetAddress.getByAddress(tempByteArray).getHostName());
                tempByteArray = new byte[OspfUtil.FOUR_BYTES];
                channelBuffer.readBytes(tempByteArray, 0, OspfUtil.FOUR_BYTES);
                ospfLsaLink.setLinkData(InetAddress.getByAddress(tempByteArray).getHostName());
                ospfLsaLink.setLinkType(channelBuffer.readByte());
                ospfLsaLink.setTos(channelBuffer.readByte());
                ospfLsaLink.setMetric(channelBuffer.readUnsignedShort());
                //add the link
                this.addRouterLink(ospfLsaLink);
            }
        } catch (Exception e) {
            log.debug("Error::RouterLsa:: {}", e.getMessage());
            throw new OspfParseException(OspfErrorType.OSPF_MESSAGE_ERROR, OspfErrorType.BAD_MESSAGE);
        }
    }

    /**
     * Returns instance as bytes.
     *
     * @return instance as bytes
     * @throws OspfParseException might throws exception while parsing packet
     */
    public byte[] asBytes() throws OspfParseException {
        byte[] lsaMessage = null;

        byte[] lsaHeader = getLsaHeaderAsByteArray();
        byte[] lsaBody = getLsaBodyAsByteArray();
        lsaMessage = Bytes.concat(lsaHeader, lsaBody);

        return lsaMessage;
    }

    /**
     * Gets the LSA body as bytes.
     *
     * @return LSA body as bytes
     */
    public byte[] getLsaBodyAsByteArray() {
        List<Byte> bodyLst = new ArrayList<>();

        try {
            int isVirtualEndPointVal = this.isVirtualEndPoint ? 1 : 0;
            int isASBoundaryRouterVal = this.isAsBoundaryRouter ? 1 : 0;
            int isAreaBorderRouterVal = this.isAreaBorderRouter ? 1 : 0;

            StringBuilder sb = new StringBuilder();
            sb.append(Integer.toBinaryString(isVirtualEndPointVal));
            sb.append(Integer.toBinaryString(isASBoundaryRouterVal));
            sb.append(Integer.toBinaryString(isAreaBorderRouterVal));

            //added VEB
            bodyLst.add((byte) Integer.parseInt(sb.toString(), 2));
            //second byte is 0.
            bodyLst.add((byte) 0);
            //Number of links
            bodyLst.addAll(Bytes.asList(OspfUtil.convertToTwoBytes(this.noLink())));

            //add each link details
            for (OspfLsaLink lsaLink : routerLinks) {
                bodyLst.addAll(Bytes.asList(InetAddress.getByName(lsaLink.linkId()).getAddress()));
                bodyLst.addAll(Bytes.asList(InetAddress.getByName(lsaLink.linkData()).getAddress()));
                bodyLst.add((byte) lsaLink.linkType());
                bodyLst.add((byte) lsaLink.tos());
                bodyLst.addAll(Bytes.asList(OspfUtil.convertToTwoBytes(lsaLink.metric())));
            }
        } catch (Exception e) {
            log.debug("Error::getLsrBodyAsByteArray {}", e.getMessage());
            return Bytes.toArray(bodyLst);
        }

        return Bytes.toArray(bodyLst);
    }

    /**
     * Increment the link by 1.
     */
    public void incrementLinkNo() {
        this.noLink++;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .omitNullValues()
                .add("isVirtualEndPoint", isVirtualEndPoint)
                .add("isAsBoundaryRouter", isAsBoundaryRouter)
                .add("isAreaBorderRouter", isAreaBorderRouter)
                .add("noLink", noLink)
                .add("routerLinks", routerLinks)
                .toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        RouterLsa that = (RouterLsa) o;
        return Objects.equal(isVirtualEndPoint, that.isVirtualEndPoint) &&
                Objects.equal(isAsBoundaryRouter, that.isAsBoundaryRouter) &&
                Objects.equal(isAreaBorderRouter, that.isAreaBorderRouter) &&
                Objects.equal(noLink, that.noLink) &&
                Objects.equal(routerLinks, that.routerLinks);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(isVirtualEndPoint, isAsBoundaryRouter, isAreaBorderRouter,
                                noLink, routerLinks);
    }
}