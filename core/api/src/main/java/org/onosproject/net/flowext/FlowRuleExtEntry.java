package org.onosproject.net.flowext;

import org.onosproject.net.DeviceId;

/**
 * Describes flow rule extension entry, extension means any kind of flow entry.
 */
public class FlowRuleExtEntry {

   private DeviceId deviceId;

   //Length of flowEntryExtension, used for serialization process.
   private int length;

   //classT is what class the flowEntryExtension can be decode to.
   private Class<?> classT;

   /*
    * Not standard OpenFlow flowEntry, maybe privacy by any device vendor.
    * it maybe contains other information such as this entry is to-add or to-remove
    * except the info of deviceId and flow entry. 
    */
   private byte[] flowEntryExtension;

   public FlowRuleExtEntry(DeviceId deviceId, byte[] data) {
	   this.setDeviceId(deviceId);
	   this.setLength(data.length);
	   this.setFlowEntryExt(data);
   }

   public DeviceId getDeviceId() {
	   return deviceId;
   }

   public void setDeviceId(DeviceId deviceId) {
	   this.deviceId = deviceId;
   }

   public int getLength() {
	  return length;
   }

   public void setLength(int length) {
	  this.length = length;
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

   public void setClass(Class<?> classT) {
       this.classT = classT;
   }

   @Override
   public boolean equals(Object obj) {
        if(obj  instanceof FlowRuleExtEntry) {
        	FlowRuleExtEntry entry = (FlowRuleExtEntry)obj;
	        String flowEntryExtension1 = new String(flowEntryExtension);
	        String flowEntryExtension2 = new String(entry.getFlowEntryExt());
            return flowEntryExtension1.equals(flowEntryExtension2);
        }else{
            return false;
        }
   }

   @Override
   public String toString() {
        String obj = new String(flowEntryExtension);
        return obj;
   }
   
}
