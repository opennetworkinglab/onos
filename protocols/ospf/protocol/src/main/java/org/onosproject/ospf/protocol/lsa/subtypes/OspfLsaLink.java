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
package org.onosproject.ospf.protocol.lsa.subtypes;

import com.google.common.base.MoreObjects;

/**
 * Representation of an OSPF LSA link.
 */
public class OspfLsaLink {

    private String linkId;
    private String linkData;
    private int linkType;
    private int metric;
    private int tos;

    /**
     * Gets link id.
     *
     * @return link id
     */
    public String linkId() {
        return linkId;
    }

    /**
     * Sets link id.
     *
     * @param linkId link id
     */
    public void setLinkId(String linkId) {
        this.linkId = linkId;
    }

    /**
     * Gets link data.
     *
     * @return link data
     */
    public String linkData() {
        return linkData;
    }

    /**
     * Sets link data.
     *
     * @param linkData link data
     */
    public void setLinkData(String linkData) {
        this.linkData = linkData;
    }

    /**
     * Gets link type.
     *
     * @return link type
     */
    public int linkType() {
        return linkType;
    }

    /**
     * Sets link type.
     *
     * @param linkType link type
     */
    public void setLinkType(int linkType) {
        this.linkType = linkType;
    }

    /**
     * Gets metric value.
     *
     * @return metric.
     */
    public int metric() {
        return metric;
    }

    /**
     * Sets metric value.
     *
     * @param metric metric
     */
    public void setMetric(int metric) {
        this.metric = metric;
    }

    /**
     * Gets tos.
     *
     * @return tos
     */
    public int tos() {
        return tos;
    }

    /**
     * Sets tos.
     *
     * @param tos tos
     */
    public void setTos(int tos) {
        this.tos = tos;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .omitNullValues()
                .add("linkID", linkId)
                .add("linkData", linkData)
                .add("linkType", linkType)
                .add("metric", metric)
                .add("tos", tos)
                .toString();
    }
}
