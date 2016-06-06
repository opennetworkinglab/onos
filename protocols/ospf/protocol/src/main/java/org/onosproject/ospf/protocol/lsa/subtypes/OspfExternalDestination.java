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
import org.onlab.packet.Ip4Address;

/**
 * Defines the OSPF external destination.
 */
public class OspfExternalDestination {

    private boolean isType1orType2Metric;
    private int metric;
    private Ip4Address forwardingAddress;
    private int externalRouterTag;

    /**
     * Gets whether type1 or type 2 metric.
     *
     * @return true if Type1 or false if Type2 metric
     */
    public boolean isType1orType2Metric() {
        return isType1orType2Metric;
    }

    /**
     * Sets whether type1 or Type2 metric.
     *
     * @param isType1orType2Metric is type 1 or type 2 metric
     */
    public void setType1orType2Metric(boolean isType1orType2Metric) {
        this.isType1orType2Metric = isType1orType2Metric;
    }

    /**
     * Gets the metric value.
     *
     * @return metric value
     */
    public int metric() {
        return metric;
    }

    /**
     * Sets the metric value.
     *
     * @param metric metric value
     */
    public void setMetric(int metric) {
        this.metric = metric;
    }

    /**
     * Gets forwarding address.
     *
     * @return forwarding address
     */
    public Ip4Address forwardingAddress() {
        return forwardingAddress;
    }

    /**
     * Sets forwarding address.
     *
     * @param forwardingAddress forwarding address
     */
    public void setForwardingAddress(Ip4Address forwardingAddress) {
        this.forwardingAddress = forwardingAddress;
    }

    /**
     * Gets external router tag.
     *
     * @return external router tag
     */
    public int externalRouterTag() {
        return externalRouterTag;
    }

    /**
     * Sets external router tag.
     *
     * @param externalRouterTag external router tag
     */
    public void setExternalRouterTag(int externalRouterTag) {
        this.externalRouterTag = externalRouterTag;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .omitNullValues()
                .add("isType1orType2Metric", isType1orType2Metric)
                .add("metric", metric)
                .add("forwardingAddress", forwardingAddress)
                .add("externalRouterTag", externalRouterTag)
                .toString();
    }
}