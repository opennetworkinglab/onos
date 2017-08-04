/*
 * Copyright 2015-present Open Networking Foundation
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
package org.onosproject.bgpio.protocol.linkstate;

import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import org.onosproject.bgpio.protocol.linkstate.BgpNodeLSNlriVer4.ProtocolType;
import org.onosproject.bgpio.types.BgpValueType;

import com.google.common.base.MoreObjects;

/**
 * This Class stores path Attributes, protocol ID and Identifier of LinkState NLRI.
 */
public class PathAttrNlriDetails {
    private List<BgpValueType> pathAttributes;
    private ProtocolType protocolID;
    private long identifier;

    /**
     * Sets path attribute with specified path attribute.
     *
     * @param pathAttributes in update message
     */
    public void setPathAttribute(List<BgpValueType> pathAttributes) {
        this.pathAttributes = pathAttributes;
    }

    /**
     * Returns path attributes.
     *
     * @return path attributes
     */
    public List<BgpValueType> pathAttributes() {
        return this.pathAttributes;
    }

    /**
     * Sets protocolID with specified protocolID.
     *
     * @param protocolID in linkstate nlri
     */
    public void setProtocolID(ProtocolType protocolID) {
        this.protocolID = protocolID;
    }

    /**
     * Returns protocolID.
     *
     * @return protocolID
     */
    public ProtocolType protocolID() {
        return this.protocolID;
    }

    /**
     * Sets identifier with specified identifier.
     *
     * @param identifier in linkstate nlri
     */
    public void setIdentifier(long identifier) {
        this.identifier = identifier;
    }

    /**
     * Returns Identifier.
     *
     * @return Identifier
     */
    public long identifier() {
        return this.identifier;
    }

    @Override
    public int hashCode() {
        return Objects.hash(pathAttributes, protocolID, identifier);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof PathAttrNlriDetails) {
            int countObjSubTlv = 0;
            int countOtherSubTlv = 0;
            boolean isCommonSubTlv = true;
            PathAttrNlriDetails other = (PathAttrNlriDetails) obj;
            Iterator<BgpValueType> objListIterator = other.pathAttributes.iterator();
            countOtherSubTlv = other.pathAttributes.size();
            countObjSubTlv = pathAttributes.size();
            if (countObjSubTlv != countOtherSubTlv) {
                return false;
            } else {
                while (objListIterator.hasNext() && isCommonSubTlv) {
                    BgpValueType subTlv = objListIterator.next();
                    if (pathAttributes.contains(subTlv) && other.pathAttributes.contains(subTlv)) {
                        isCommonSubTlv = Objects.equals(pathAttributes.get(pathAttributes.indexOf(subTlv)),
                                other.pathAttributes.get(other.pathAttributes.indexOf(subTlv)));
                    } else {
                        isCommonSubTlv = false;
                    }
                }
                return isCommonSubTlv && Objects.equals(identifier, other.identifier)
                        && Objects.equals(protocolID, other.protocolID);
            }
        }
        return false;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("identifier", identifier)
                .add("protocolID", protocolID)
                .add("pathAttributes", pathAttributes)
                .toString();
    }
}
