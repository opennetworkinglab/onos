/*
 * Copyright 2015 Open Networking Laboratory
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
import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;

import java.util.Objects;

public final class CordService {

    enum ServiceType {
        PRIVATE,
        PRIVATE_DIRECT,
        PRIVATE_INDIRECT,
        PUBLIC_DIRECT,
        PUBLIC_INDIRECT
    }

    private final CordServiceId id;
    private final long segmentationId;
    private final ServiceType serviceType;
    private final IpPrefix serviceIpRange;
    private final IpAddress serviceIp;

    /**
     * Default constructor.
     *
     * @param id service id, which is identical to OpenStack network id
     * @param segmentationId segmentation id, which is identical to VNI
     * @param serviceType service type
     * @param serviceIpRange service ip range
     * @param serviceIp service ip
     */
    public CordService(CordServiceId id, long segmentationId, ServiceType serviceType,
                   IpPrefix serviceIpRange, IpAddress serviceIp) {
        this.id = id;
        this.segmentationId = segmentationId;
        this.serviceType = serviceType;
        this.serviceIpRange = serviceIpRange;
        this.serviceIp = serviceIp;
    }

    /**
     * Returns service ID.
     *
     * @return service id
     */
    public CordServiceId id() {
        return id;
    }

    /**
     * Returns segmentation ID of this service.
     *
     * @return segmentation id
     */
    public long segmentationId() {
        return segmentationId;
    }

    /**
     * Returns service type.
     *
     * @return service type
     */
    public ServiceType serviceType() {
        return serviceType;
    }

    /**
     * Returns service IP range.
     *
     * @return CIDR
     */
    public IpPrefix serviceIpRange() {
        return serviceIpRange;
    }

    /**
     * Returns service IP address.
     *
     * @return ip address
     */
    public IpAddress serviceIp() {
        return serviceIp;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof CordService)) {
            return false;
        }
        final CordService other = (CordService) obj;
        return Objects.equals(this.id, other.id);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("id", id)
                .add("segmentationId", segmentationId)
                .add("serviceType", serviceType)
                .add("serviceIpRange", serviceIpRange)
                .add("serviceIp", serviceIp)
                .toString();
    }
}
