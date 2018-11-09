/*
 * Copyright 2018-present Open Networking Foundation
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
package org.onosproject.incubator.net.resource.label;

import java.util.Collection;
import java.util.Objects;

import com.google.common.annotations.Beta;
import org.onosproject.net.DeviceId;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableSet;

/**
 * Represents for a label request.
 */
@Beta
public class LabelResourceRequest {

    private final DeviceId deviceId;
    private final Type type;
    private final long applyNum;
    private ImmutableSet<LabelResource> releaseCollection;

    /**
     * Creates LabelResourceRequest object.
     * @param deviceId device identifier
     * @param type request type
     * @param applyNum apply the number of labels
     * @param releaseCollection Set of released label
     */
    public LabelResourceRequest(DeviceId deviceId,
                                Type type,
                                long applyNum,
                                ImmutableSet<LabelResource> releaseCollection) {
        this.deviceId = deviceId;
        this.type = type;
        this.applyNum = applyNum;
        this.releaseCollection = releaseCollection;
    }
    /**
     * Returns a device id.
     * @return DeviceId
     */
    public DeviceId deviceId() {
        return deviceId;
    }

    /**
     * Returns request type.
     * @return Type
     */
    public Type type() {
        return type;
    }

    /**
     * Returns apply label number.
     * @return label number
     */
    public long applyNum() {
        return applyNum;
    }

    /**
     * Returns the collection of release labels.
     * @return Collection of DefaultLabelResource
     */
    public Collection<LabelResource> releaseCollection() {
        return releaseCollection;
    }

    /**
     * Request type.
     */
    public enum Type {
        APPLY, //apple label request
        RELEASE //release label request
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.deviceId, this.applyNum, this.type,
                            this.releaseCollection);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof LabelResourceRequest) {
            LabelResourceRequest that = (LabelResourceRequest) obj;
            return Objects.equals(this.deviceId, that.deviceId)
                    && Objects.equals(this.applyNum, that.applyNum)
                    && Objects.equals(this.type, that.type)
                    && Objects.equals(this.releaseCollection,
                                      that.releaseCollection);
        }
        return false;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this).add("deviceId", this.deviceId)
                .add("applyNum", this.applyNum).add("type", this.type)
                .add("releaseCollection", this.releaseCollection).toString();
    }
}
