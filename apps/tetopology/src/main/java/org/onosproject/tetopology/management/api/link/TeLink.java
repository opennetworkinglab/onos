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
package org.onosproject.tetopology.management.api.link;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;

import org.onosproject.tetopology.management.api.node.TeStatus;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;

/**
 * Link TE extension.
 * <p>
 * The Set/Get methods below are defined to accept and pass references because
 * the object class is treated as a "composite" object class that holds
 * references to various member objects and their relationships, forming a
 * data tree. Internal routines of the TE topology manager may use the
 * following example methods to construct and manipulate any piece of data in
 * the data tree:
 *<pre>
 * newNode.getTe().setAdminStatus(), or
 * newNode.getSupportingNodeIds().add(nodeId), etc.
 *</pre>
 * Same for constructors where, for example, a child list may be constructed
 * first and passed in by reference to its parent object constructor.
 */
public class TeLink {
    // See org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.
    // topology.rev20160708.ietftetopology
    // .augmentedntlink.te.config.DefaultTeLinkAttributes for reference
    private BigInteger linkIndex;
    private String name;
    private TeStatus adminStatus;
    private TeStatus opStatus;
    private TeLinkAccessType accessType;
    //private administrativeGroup
    private LinkProtectionType linkProtectionType;
    private BigDecimal maxLinkBandwidth;
    private BigDecimal maxResvLinkBandwidth;
    private List<UnreservedBandwidth> unreservedBandwidths;
    private long teDefaultMetric;
    private ExternalDomain externalDomain;
    private List<Long> teSrlgs;
    private boolean isAbstract;
    private UnderlayPath underlayPath;

    /**
     * Creates an instance of TeLink.
     *
     * @param linkIndex TE link index
     */
    public TeLink(BigInteger linkIndex) {
        this.linkIndex = linkIndex;
    }

    /**
     * Sets the name.
     *
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Sets the administrative status.
     *
     * @param adminStatus the adminStatus to set
     */
    public void setAdminStatus(TeStatus adminStatus) {
        this.adminStatus = adminStatus;
    }

    /**
     * Sets the operational status.
     *
     * @param opStatus the opStatus to set
     */
    public void setOpStatus(TeStatus opStatus) {
        this.opStatus = opStatus;
    }

    /**
     * Sets the access type.
     *
     * @param accessType the accessType to set
     */
    public void setAccessType(TeLinkAccessType accessType) {
        this.accessType = accessType;
    }

    /**
     * Sets the protection type.
     *
     * @param type the linkProtectionType to set
     */
    public void setLinkProtectionType(LinkProtectionType type) {
        this.linkProtectionType = type;
    }

    /**
     * Sets the link maximum bandwidth.
     *
     * @param bw the maxLinkBandwidth to set
     */
    public void setMaxLinkBandwidth(BigDecimal bw) {
        this.maxLinkBandwidth = bw;
    }

    /**
     * Sets the link maximum reservable bandwidth.
     *
     * @param bw the maxResvLinkBandwidth to set
     */
    public void setMaxResvLinkBandwidth(BigDecimal bw) {
        this.maxResvLinkBandwidth = bw;
    }

    /**
     * Sets the list of link unreserved bandwidths.
     *
     * @param bwList the unreservedBandwidths to set
     */
    public void setUnreservedBandwidths(List<UnreservedBandwidth> bwList) {
        this.unreservedBandwidths = bwList;
    }

    /**
     * Sets the default metric.
     *
     * @param metric the teDefaultMetric to set
     */
    public void setTeDefaultMetric(long metric) {
        this.teDefaultMetric = metric;
    }

    /**
     * Sets the external domain link.
     *
     * @param extDomain the externalDomain to set
     */
    public void setExternalDomain(ExternalDomain extDomain) {
        this.externalDomain = extDomain;
    }

    /**
     * Sets the list of SRLGs.
     *
     * @param teSrlgs the teSrlgs to set
     */
    public void setTeSrlgs(List<Long> teSrlgs) {
        this.teSrlgs = teSrlgs;
    }

    /**
     * Sets the isAbstract flag.
     *
     * @param isAbstract the isAbstract to set
     */
    public void setIsAbstract(boolean isAbstract) {
        this.isAbstract = isAbstract;
    }

    /**
     * Sets the link underlay path.
     *
     * @param underlayPath the underlay path to set
     */
    public void setUnderlayPath(UnderlayPath underlayPath) {
        this.underlayPath = underlayPath;
    }

    /**
     * Returns the link index.
     *
     * @return link index
     */
    public BigInteger linkIndex() {
        return linkIndex;
    }

    /**
     * Returns the name.
     *
     * @return name of the TE link
     */
    public String name() {
        return name;
    }

    /**
     * Returns the administrative status.
     *
     * @return link admin status
     */
    public TeStatus adminStatus() {
        return adminStatus;
    }

    /**
     * Returns the operational status.
     *
     * @return link operational status
     */
    public TeStatus opStatus() {
        return opStatus;
    }

    /**
     * Returns the access type.
     *
     * @return link access type
     */
    public TeLinkAccessType accessType() {
        return accessType;
    }

    /**
     * Returns the link protection type.
     *
     * @return link protection type
     */
    public LinkProtectionType linkProtectionType() {
        return linkProtectionType;
    }

    /**
     * Returns the link maximum bandwidth.
     *
     * @return link maximum bandwidth
     */
    public BigDecimal maxLinkBandwidth() {
        return maxLinkBandwidth;
    }

    /**
     * Returns the maximum reservable bandwidth.
     *
     * @return link maximum reservable bandwidth
     */
    public BigDecimal maxResvLinkBandwidth() {
        return maxResvLinkBandwidth;
    }

    /**
     * Returns the  list of link unreserved bandwidths.
     *
     * @return link unreserved bandwidth
     */
    public List<UnreservedBandwidth> unreservedBandwidths() {
        return unreservedBandwidths;
    }

    /**
     * Returns the te default metric.
     *
     * @return link TE metric
     */
    public long teDefaultMetric() {
        return teDefaultMetric;
    }

    /**
     * Returns the external domain link.
     *
     * @return external domain
     */
    public ExternalDomain externalDomain() {
        return externalDomain;
    }

    /**
     * Returns the list of SRLGs.
     *
     * @return link SRLG
     */
    public List<Long> teSrlgs() {
        return teSrlgs;
    }

    /**
     * Returns the flag isAbstract.
     *
     * @return true or false if link is abstract
     */
    public boolean isAbstract() {
        return isAbstract;
    }

    /**
     * Returns the underlay path data.
     *
     * @return link underlay TE path
     */
    public UnderlayPath underlayPath() {
        return underlayPath;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(linkIndex, name, adminStatus, opStatus, accessType,
                linkProtectionType, maxLinkBandwidth, maxResvLinkBandwidth, unreservedBandwidths,
                teDefaultMetric, externalDomain, teSrlgs, isAbstract, underlayPath);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (object instanceof TeLink) {
            TeLink that = (TeLink) object;
            return Objects.equal(this.linkIndex, that.linkIndex) &&
                    Objects.equal(this.name, that.name) &&
                    Objects.equal(this.adminStatus, that.adminStatus) &&
                    Objects.equal(this.opStatus, that.opStatus) &&
                    Objects.equal(this.accessType, that.accessType) &&
                    Objects.equal(this.linkProtectionType, that.linkProtectionType) &&
                    Objects.equal(this.maxLinkBandwidth, that.maxLinkBandwidth) &&
                    Objects.equal(this.maxResvLinkBandwidth, that.maxResvLinkBandwidth) &&
                    Objects.equal(this.unreservedBandwidths, that.unreservedBandwidths) &&
                    Objects.equal(this.teDefaultMetric, that.teDefaultMetric) &&
                    Objects.equal(this.externalDomain, that.externalDomain) &&
                    Objects.equal(this.teSrlgs, that.teSrlgs) &&
                    Objects.equal(this.isAbstract, that.isAbstract) &&
                    Objects.equal(this.underlayPath, that.underlayPath);
        }
        return false;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("linkIndex", linkIndex)
                .add("name", name)
                .add("adminStatus", adminStatus)
                .add("opStatus", opStatus)
                .add("accessType", accessType)
                .add("linkProtectionType", linkProtectionType)
                .add("maxLinkBandwidth", maxLinkBandwidth)
                .add("maxResvLinkBandwidth", maxResvLinkBandwidth)
                .add("unreservedBandwidths", unreservedBandwidths)
                .add("teDefaultMetric", teDefaultMetric)
                .add("externalDomain", externalDomain)
                .add("teSrlgs", teSrlgs)
                .add("isAbstract", isAbstract)
                .add("underlayPath", underlayPath)
                .toString();
    }


}
