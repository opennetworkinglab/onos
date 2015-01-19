package org.onosproject.net.flowext;

import org.onosproject.net.DeviceId;

/**
 * Describes flow rule extension entry, extension means any kind of flow entry.
 */
public class FlowRuleExtEntry {

   private DeviceId deviceId;


   //classT is what class the flowEntryExtension can be decode to, used for GUI or CLI
   private Class<?> classT;

   /*
    * Not standard OpenFlow flowEntry, maybe privacy by any device vendor.
    * it maybe contains other information such as this entry is to-add or to-remove
    * except the info of deviceId and flow entry.
    */
   private byte[] flowEntryExtension;


   public FlowRuleExtEntry(DeviceId deviceId, Class<?> classT, byte[] data) {
	   this.setDeviceId(deviceId);
	   this.setClass(classT);
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
        return super.hashCode();
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
