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
package org.onosproject.incubator.net.resource.label;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;

import com.google.common.annotations.Beta;
import org.onosproject.net.DeviceId;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableSet;

/**
 * Abstraction of the capacity of device label resource or global label
 * resource. It's contiguous range of label resource. When a application apply
 * some labels of some device, first catch from Set that store
 * available labels, if the size of the Set less than the apply number, then get
 * labels by calculating with three attributes, beginLabel,endLabel and
 * currentUsedMaxLabelId.
 */
@Beta
public class LabelResourcePool {

    private final DeviceId deviceId;
    private final LabelResourceId beginLabel;
    private final LabelResourceId endLabel;
    private final long totalNum; // capacity of label resource pool
    private final long usedNum; // have used label number
    private final LabelResourceId currentUsedMaxLabelId; // the maximal label
                                                        // number id
    private ImmutableSet<LabelResource> releaseLabelId; // Set of released label

    /**
     * Creates a pool by device id,begin label id,end label id.
     *
     * @param deviceId device identifier
     * @param beginLabel represents for the first label id in the range of label
     *            resource pool
     * @param endLabel represents for the last label id in the range of label
     *            resource pool
     */
    public LabelResourcePool(String deviceId, long beginLabel, long endLabel) {
        this(deviceId, beginLabel, endLabel, endLabel - beginLabel + 1, 0L,
             beginLabel, ImmutableSet.copyOf(Collections.emptySet()));
    }

    /**
     * Creates a pool by device id,begin label id,end label id.
     * Used to update a pool in the store.
     *
     * @param deviceId device identifier
     * @param beginLabel represents for the first label id in the range of label
     *            resource pool
     * @param endLabel represents for the last label id in the range of label
     *            resource pool
     * @param totalNum capacity of label resource pool
     * @param usedNum have used label number
     * @param currentUsedMaxLabelId the maximal label number id
     * @param releaseLabelId Set of released label
     */
    public LabelResourcePool(String deviceId, long beginLabel, long endLabel,
                             long totalNum, long usedNum,
                             long currentUsedMaxLabelId,
                             ImmutableSet<LabelResource> releaseLabelId) {
        checkArgument(endLabel >= beginLabel,
                      "endLabel %s must be greater than or equal to beginLabel %s",
                      endLabel, beginLabel);
        this.deviceId = DeviceId.deviceId(deviceId);
        this.beginLabel = LabelResourceId.labelResourceId(beginLabel);
        this.endLabel = LabelResourceId.labelResourceId(endLabel);
        this.totalNum = totalNum;
        this.usedNum = usedNum;
        this.currentUsedMaxLabelId = LabelResourceId
                .labelResourceId(currentUsedMaxLabelId);
        this.releaseLabelId = releaseLabelId;
    }

    /**
     * Returns a device id.
     *
     * @return DeviceId
     */
    public DeviceId deviceId() {
        return deviceId;
    }

    /**
     * Returns a begin Label id.
     *
     * @return begin Label id
     */
    public LabelResourceId beginLabel() {
        return beginLabel;
    }

    /**
     * Returns an end Label id.
     *
     * @return end Label id
     */
    public LabelResourceId endLabel() {
        return endLabel;
    }

    /**
     * Returns a begin Label id.
     *
     * @return current Used Maximal Label Id
     */
    public LabelResourceId currentUsedMaxLabelId() {
        return currentUsedMaxLabelId;
    }

    /**
     * Returns total number.
     *
     * @return the total label number
     */
    public long totalNum() {
        return totalNum;
    }

    /**
     * Returns used number.
     *
     * @return the used label number
     */
    public long usedNum() {
        return usedNum;
    }

    /**
     * Returns the Set of released label before.
     *
     * @return the Set of LabelResource
     */
    public Set<LabelResource> releaseLabelId() {
        return releaseLabelId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.deviceId, this.beginLabel, this.endLabel,
                            this.totalNum, this.usedNum,
                            this.currentUsedMaxLabelId, this.releaseLabelId);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof LabelResourcePool) {
            LabelResourcePool that = (LabelResourcePool) obj;
            return Objects.equals(this.deviceId, that.deviceId)
                    && Objects.equals(this.beginLabel, that.beginLabel)
                    && Objects.equals(this.endLabel, that.endLabel)
                    && Objects.equals(this.totalNum, that.totalNum)
                    && Objects.equals(this.usedNum, that.usedNum)
                    && Objects.equals(this.currentUsedMaxLabelId,
                                      that.currentUsedMaxLabelId)
                    && Objects.equals(this.releaseLabelId, that.releaseLabelId);
        }
        return false;
    }

    @Override
    public String toString() {
        // TODO Auto-generated method stub
        return MoreObjects.toStringHelper(this).add("deviceId", this.deviceId)
                .add("beginLabel", this.beginLabel)
                .add("endLabel", this.endLabel).add("totalNum", this.totalNum)
                .add("usedNum", this.usedNum)
                .add("currentUsedMaxLabelId", this.currentUsedMaxLabelId)
                .add("releaseLabelId", this.releaseLabelId).toString();
    }
}
