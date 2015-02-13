package org.onosproject.net.resource;

import java.util.LinkedList;
import java.util.Objects;
import java.util.Queue;

import org.onosproject.net.DeviceId;

import com.google.common.base.MoreObjects;

/**
 * Abstraction of the capacity of device label resource.
 */
public class LabelResourcePool {

    private DeviceId deviceId;
    private LabelResourceId beginLabel;
    private LabelResourceId endLabel;
    private long totalNum;
    private long usedNum;
    private LabelResourceId currentUsedMaxLabelId;
    private Queue<DefaultLabelResource> releaseLabelId;

    public LabelResourcePool(String deviceId, long beginLabel, long endLabel) {
        this.deviceId = DeviceId.deviceId(deviceId);
        this.beginLabel = LabelResourceId.labelResourceId(beginLabel);
        this.endLabel = LabelResourceId.labelResourceId(endLabel);
        this.totalNum = endLabel - beginLabel + 1;
        this.usedNum = 0L;
        this.currentUsedMaxLabelId = LabelResourceId.labelResourceId(beginLabel);
        this.releaseLabelId = new LinkedList<DefaultLabelResource>();
    }

    public DeviceId getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(DeviceId deviceId) {
        this.deviceId = deviceId;
    }

    public LabelResourceId getBeginLabel() {
        return beginLabel;
    }

    public void setBeginLabel(LabelResourceId beginLabel) {
        this.beginLabel = beginLabel;
    }

    public LabelResourceId getEndLabel() {
        return endLabel;
    }

    public void setEndLabel(LabelResourceId endLabel) {
        this.endLabel = endLabel;
    }

    public LabelResourceId getCurrentUsedMaxLabelId() {
        return currentUsedMaxLabelId;
    }

    public void setCurrentUsedMaxLabelId(LabelResourceId currentUsedMaxLabelId) {
        this.currentUsedMaxLabelId = currentUsedMaxLabelId;
    }

    public long getTotalNum() {
        return totalNum;
    }

    public void setTotalNum(long totalNum) {
        this.totalNum = totalNum;
    }

    public long getUsedNum() {
        return usedNum;
    }

    public void setUsedNum(long usedNum) {
        this.usedNum = usedNum;
    }

    public Queue<DefaultLabelResource> getReleaseLabelId() {
        return releaseLabelId;
    }

    public void setReleaseLabelId(Queue<DefaultLabelResource> releaseLabelId) {
        this.releaseLabelId = releaseLabelId;
    }

    @Override
    public int hashCode() {
        // TODO Auto-generated method stub
        return Objects.hashCode(this.deviceId.toString() + this.beginLabel.toString()
                + this.endLabel.toString() + this.totalNum + this.usedNum
                + this.currentUsedMaxLabelId.toString() + this.releaseLabelId.toString());
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof LabelResourcePool) {
            LabelResourcePool that = (LabelResourcePool) obj;
            return Objects.equals(this.deviceId.toString() + this.beginLabel.toString()
                                          + this.endLabel.toString() + this.totalNum
                                          + this.usedNum
                                          + this.currentUsedMaxLabelId.toString()
                                          + this.releaseLabelId.toString(),
                                  that.deviceId.toString() + that.beginLabel
                                          + that.endLabel + that.totalNum
                                          + that.usedNum
                                          + that.currentUsedMaxLabelId
                                          + that.releaseLabelId.toString());
        }
        return false;
    }

    @Override
    public String toString() {
        // TODO Auto-generated method stub
        return MoreObjects.toStringHelper(this)
                .add("deviceId", this.deviceId.toString())
                .add("beginLabel", this.beginLabel.toString())
                .add("endLabel", this.endLabel.toString()).add("totalNum", this.totalNum)
                .add("usedNum", this.usedNum)
                .add("currentUsedMaxLabelId", this.currentUsedMaxLabelId.toString())
                .add("releaseLabelId", this.releaseLabelId.toString())
                .toString();
    }
}
