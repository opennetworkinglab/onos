package org.onosproject.net.resource;

import java.util.Collection;
import java.util.Objects;

import org.onosproject.net.DeviceId;

import com.google.common.base.MoreObjects;

public class LabelResourceRequest {

    private DeviceId deviceId;
    private Type type;
    private long applyNum;
    private Collection<DefaultLabelResource> releaseCollection;

    public LabelResourceRequest(DeviceId deviceId,
                                Type type,
                                long applyNum,
                                Collection<DefaultLabelResource> releaseCollection) {
        this.deviceId = deviceId;
        this.type = type;
        this.applyNum = applyNum;
        this.releaseCollection = releaseCollection;
    }

    public DeviceId getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(DeviceId deviceId) {
        this.deviceId = deviceId;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public long getApplyNum() {
        return applyNum;
    }

    public void setApplyNum(long applyNum) {
        this.applyNum = applyNum;
    }

    public Collection<DefaultLabelResource> getReleaseCollection() {
        return releaseCollection;
    }

    public void setReleaseCollection(Collection<DefaultLabelResource> releaseCollection) {
        this.releaseCollection = releaseCollection;
    }

    public enum Type {
        APPLY, RELEASE
    }

    @Override
    public int hashCode() {
        // TODO Auto-generated method stub
        return Objects.hashCode(this.deviceId.toString() + this.applyNum
                + this.type + this.releaseCollection.size()
                + this.releaseCollection.toString());
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof LabelResourceRequest) {
            LabelResourceRequest that = (LabelResourceRequest) obj;
            return Objects.equals(this.deviceId.toString() + this.applyNum
                                          + this.type
                                          + this.releaseCollection.size()
                                          + this.releaseCollection.toString(),
                                  that.deviceId.toString() + that.applyNum
                                          + that.type
                                          + that.releaseCollection.size()
                                          + that.releaseCollection.toString());
        }
        return false;
    }

    @Override
    public String toString() {
        // TODO Auto-generated method stub
        return MoreObjects.toStringHelper(this)
                .add("deviceId", this.deviceId.toString())
                .add("applyNum", this.applyNum).add("type", this.type)
                .add("releaseCollectionSize", this.releaseCollection.size())
                .add("releaseCollection", this.releaseCollection.toString())
                .toString();
    }
}
