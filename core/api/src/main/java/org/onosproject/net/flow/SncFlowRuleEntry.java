package org.onosproject.net.flow;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.projectfloodlight.openflow.exceptions.OFParseError;
import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFMessageReader;

public class SncFlowRuleEntry {
   private int deviceId;
   private int length;
   private byte[] sncflow;
   public SncFlowRuleEntry(Integer fpid, byte[] data) {
	   this.setDeviceId(fpid);
	   this.setLength(length);
	   this.setSncflow(data);
   }
   public int getDeviceId() {
	   return deviceId;
   }
   public void setDeviceId(int deviceId) {
	   this.deviceId = deviceId;
   }
   public int getLength() {
	  return length;
   }
   public void setLength(int length) {
	  this.length = length;
   }
   public byte[] getSncflow() {
	  return sncflow;
   }
   public void setSncflow(byte[] sncflow) {
	  this.sncflow = sncflow;
   }

   public byte[] subBytes(byte[] src, int begin, int len) {
	   byte[] subbytes = new byte[len];
	   for (int i = begin; i<begin + len; i++) {
		   subbytes[i - begin] = src[i];
	   }
	   return subbytes;
   }

   public OFMessage readOFMessage(byte[] buff) throws OFParseError {
	   ChannelBuffer buffer = ChannelBuffers.wrappedBuffer(buff);
	   int start = buffer.writerIndex();
	   buffer.writerIndex(0);
	   buffer.writeByte(0X4);
	   buffer.writerIndex(start);
	   OFMessageReader<OFMessage> reader = org.projectfloodlight.openflow.protocol.
			   ver13.OFFactoryVer13.INSTANCE.getReader();
	   OFMessage message = reader.readFrom(buffer);
	   return message;
   }
   
   public int getInt(byte[] buffer) {
	   return (int) ((((buffer[3] & 0xff) << 24) | ((buffer[2] & 0xff) << 16)
			   | ((buffer[1] & 0xff) << 8) | ((buffer[0] & 0xff) << 0)));
   }
}
