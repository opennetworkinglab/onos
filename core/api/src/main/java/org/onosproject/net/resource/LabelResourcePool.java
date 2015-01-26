package org.onosproject.net.resource;

import java.util.LinkedList;
import java.util.Objects;
import java.util.Queue;

import org.onosproject.net.DeviceId;

import com.google.common.base.MoreObjects;

public class LabelResourcePool {

    private DeviceId deviceId;
    private long beginLabel;
    private long endLabel;
    private long totalNum;
    private long usedNum;
    private long currentUsedMaxLabelId;
    private Queue<DefaultLabelResource> releaseLabelId;

    public LabelResourcePool(String deviceId, long beginLabel, long endLabel) {
        this.deviceId = DeviceId.deviceId(deviceId);
        this.beginLabel = beginLabel;
        this.endLabel = endLabel;
        this.totalNum = endLabel - beginLabel + 1;
        this.usedNum = 0l;
        this.currentUsedMaxLabelId = beginLabel;
        this.releaseLabelId = new LinkedList<DefaultLabelResource>();
    }

    public DeviceId getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(DeviceId deviceId) {
        this.deviceId = deviceId;
    }

    public long getBeginLabel() {
        return beginLabel;
    }

    public void setBeginLabel(long beginLabel) {
        this.beginLabel = beginLabel;
    }

    public long getEndLabel() {
        return endLabel;
    }

    public void setEndLabel(long endLabel) {
        this.endLabel = endLabel;
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

    public long getCurrentUsedMaxLabelId() {
        return currentUsedMaxLabelId;
    }

    public void setCurrentUsedMaxLabelId(long currentUsedMaxLabelId) {
        this.currentUsedMaxLabelId = currentUsedMaxLabelId;
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
        return Objects.hashCode(this.deviceId.toString() + this.beginLabel
                                + this.endLabel
                                + this.totalNum
                                + this.usedNum+this.currentUsedMaxLabelId+this.releaseLabelId.toString());
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof LabelResourcePool) {
            LabelResourcePool that = (LabelResourcePool) obj;
            return Objects.equals(this.deviceId.toString() + this.beginLabel
                                          + this.endLabel
                                          + this.totalNum
                                          + this.usedNum+this.currentUsedMaxLabelId+this.releaseLabelId.toString(),
                                          that.deviceId.toString() + that.beginLabel
                                          + that.endLabel
                                          + that.totalNum
                                          + that.usedNum+that.currentUsedMaxLabelId+that.releaseLabelId.toString());
        }
        return false;
    }

    @Override
    public String toString() {
        // TODO Auto-generated method stub
        return MoreObjects.toStringHelper(this)
                .add("deviceId", this.deviceId.toString())
                .add("beginLabel", this.beginLabel).add("endLabel", this.endLabel)
                .add("totalNum", this.totalNum)
                .add("usedNum", this.usedNum)
                .add("currentUsedMaxLabelId", this.currentUsedMaxLabelId)
                .add("releaseLabelId", this.releaseLabelId.toString())
                .toString();
    }
}
