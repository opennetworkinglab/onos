package org.onosproject.net.resource;

import java.util.Objects;

import org.onosproject.net.Annotations;
import org.onosproject.net.DeviceId;
import org.onosproject.net.provider.ProviderId;

public class DefaultLabelResource implements LabelResource {

    private DeviceId deviceId;
    
    private LabelResourceId labelResourceId;
    
    public DefaultLabelResource(String deviceId,long labelResourceId){
        this.deviceId= DeviceId.deviceId(deviceId);
        this.labelResourceId = LabelResourceId.labelResourceId(labelResourceId);
    }
    
    public DefaultLabelResource(DeviceId deviceId,LabelResourceId labelResourceId){
        this.deviceId= deviceId;
        this.labelResourceId = labelResourceId;
    }

    
    public DeviceId getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(DeviceId deviceId) {
        this.deviceId = deviceId;
    }

    public LabelResourceId getLabelResourceId() {
        return labelResourceId;
    }

    public void setLabelResourceId(LabelResourceId labelResourceId) {
        this.labelResourceId = labelResourceId;
    }

    @Override
    public Annotations annotations() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ProviderId providerId() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int hashCode() {
        // TODO Auto-generated method stub
        return Objects.hashCode(deviceId.toString()+labelResourceId.toString());
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof DefaultLabelResource) {
            DefaultLabelResource that = (DefaultLabelResource) obj;
            return Objects.equals(this.deviceId.toString()+this.labelResourceId.toString(), that.deviceId.toString()+that.labelResourceId.toString());
        }
        return false;
    }

    @Override
    public String toString() {
        // TODO Auto-generated method stub
        return String.valueOf(deviceId.toString()+labelResourceId.toString());
    }

   
    
    

}
