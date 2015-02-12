package org.onosproject.net.flowext;

import java.nio.ByteBuffer;
import java.util.Objects;

/**
 * Represents a generic abstraction of the service data. User app can customize whatever it needs to install on devices.
 */
public class DownStreamPacket implements FlowEntryExtension{

  /**
   * temporarily only have byte steam, but it will be extract more abstract information from it later.
   */
   private final ByteBuffer payload;

   public DownStreamPacket( ByteBuffer data) {
           this.payload = data;
   }

   /**
    * Get the payload of flowExtension.
    * 
    * @return  the byte steam value of payload.
    */
   @Override
   public ByteBuffer getPayload() {
       // TODO Auto-generated method stub
       return payload;
   }

   /**
    * Returns a hash code value for the object. 
    * It use payload as parameter to hash.
    *
    * @return  a hash code value for this object.
    */
   @Override
   public int hashCode() {
       return Objects.hash(payload);
   }

   /**
    * Indicates whether some other object is "equal to" this one.
    * 
    * @param   obj   the reference object with which to compare.
    * @return  {@code true} if this object is the same as the obj
    *          argument; {@code false} otherwise. 
    */
   @Override
   public boolean equals(Object obj) {
        if (obj instanceof DownStreamPacket) {
            DownStreamPacket packet = (DownStreamPacket) obj;
            return Objects.equals(this.payload, packet.payload);
        } else {
            return false;
        }
   }

   /**
    * Returns a string representation of the object.
    * 
    * @return  a string representation of the object.
    */
   @Override
   public String toString() {
        String obj = new String(payload.array());
        return obj;
   }
}
