package org.onosproject.net.flowext;

import java.util.Objects;

import org.onosproject.net.DeviceId;

/**
 * Describes flow rule extension entry, extension means any kind of flow entry.
 */
public class FlowRuleExtEntry {

    // DeviceId contains the information of protocol and device serial number
   private DeviceId deviceId;


   // ClassT is what class the flowEntryExtension can be decode to, used for GUI or CLI
   private Class<?> classT;

   /*
    * This OpenFlow flowEntry, maybe privacy by any device vendor.
    * it maybe contains other information such as this entry is to-add or to-remove
    * except the info of deviceId and flow entry.
    */
   private byte[] flowEntryExtension;


   public FlowRuleExtEntry(DeviceId deviceId, Class<?> classT, byte[] data) {
           this.setDeviceId(deviceId);
           this.setClassT(classT);
           this.setFlowEntryExt(data);
   }

   public DeviceId getDeviceId() {
      return deviceId;
   }

   public void setDeviceId(DeviceId deviceId) {
      this.deviceId = deviceId;
   }

   public byte[] getFlowEntryExt() {
     return flowEntryExtension;
   }

   public void setFlowEntryExt(byte[] flowEntryExtension) {
     this.flowEntryExtension = flowEntryExtension;
   }

   public Class<?> getClassT() {
       return classT;
   }

   public void setClassT(Class<?> classT) {
       this.classT = classT;
   }

   @Override
   public int hashCode() {
       return Objects.hash(deviceId, classT, toString());
   }

   @Override
   public boolean equals(Object obj) {
        if (obj instanceof FlowRuleExtEntry) {
            FlowRuleExtEntry entry = (FlowRuleExtEntry) obj;
            String flowEntryExtension1 = new String(flowEntryExtension);
            String flowEntryExtension2 = new String(entry.getFlowEntryExt());
            return flowEntryExtension1.equals(flowEntryExtension2);
        } else {
            return false;
        }
   }

   @Override
   public String toString() {
        String obj = new String(flowEntryExtension);
        return obj;
   }
}
