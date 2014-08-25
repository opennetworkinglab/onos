package org.projectfloodlight.openflow.types;

import java.util.Arrays;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.projectfloodlight.openflow.exceptions.OFParseError;
import org.projectfloodlight.openflow.protocol.OFErrorMsg;
import org.projectfloodlight.openflow.protocol.OFFactories;
import org.projectfloodlight.openflow.protocol.OFFactory;
import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFVersion;
import org.projectfloodlight.openflow.protocol.Writeable;
import org.projectfloodlight.openflow.util.ChannelUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.hash.PrimitiveSink;

/** A special-purpose wrapper for the 'data' field in an {@link OFErrorMsg} message
 *  that contains a byte serialization of the offending message.
 *
 *  This attempts to parse the offending message on demand, and if successful
 *  will present the parsed message.
 *
 * @author Andreas Wundsam <andreas.wundsam@bigswitch.com>
 */
public class OFErrorCauseData implements Writeable, PrimitiveSinkable {
    private static final Logger logger =
            LoggerFactory.getLogger(OFErrorCauseData.class);

    /** A default 'empty' cause. Note: the OFVersion OF_13 passed in here is irrelevant,
     *  because parsing of the 0-byte array will always return null, irrespective of the
     *  version.
     */
    public static final OFErrorCauseData NONE = new OFErrorCauseData(new byte[0], OFVersion.OF_13);

    private final byte[] data;
    private final OFVersion version;

    private OFErrorCauseData(byte[] data, OFVersion version) {
        this.data = data;
        this.version = version;
    }

    public static OFErrorCauseData of(byte[] data, OFVersion version) {
         return new OFErrorCauseData(Arrays.copyOf(data, data.length), version);
    }

    public byte[] getData() {
        return Arrays.copyOf(data, data.length);
    }

    public Optional<OFMessage> getParsedMessage() {
        OFFactory factory = OFFactories.getFactory(version);
        try {
            OFMessage msg = factory.getReader().readFrom(ChannelBuffers.wrappedBuffer(data));
            if(msg != null)
                return Optional.of(msg);
            else
                return Optional.absent();
        } catch (OFParseError e) {
            logger.debug("Error parsing error cause data as OFMessage: {}", e.getMessage(), e);
            return Optional.absent();
        }
    }

    public static OFErrorCauseData read(ChannelBuffer bb, int length, OFVersion version) {
        byte[] bytes = ChannelUtils.readBytes(bb, length);
        return of(bytes, version);
   }

    @Override
    public void putTo(PrimitiveSink sink) {
        sink.putBytes(data);
    }

    @Override
    public void writeTo(ChannelBuffer bb) {
        bb.writeBytes(data);
    }

   @Override
   public String toString() {
      Optional<OFMessage> parsedMessage = getParsedMessage();
      if(parsedMessage.isPresent()) {
          return String.valueOf(parsedMessage.get());
      } else {
          StringBuilder b = new StringBuilder();
          b.append("[unparsed: ");
          for(int i=0; i<data.length; i++) {
              if(i>0)
                  b.append(" ");
              b.append(String.format("%02x", data[i]));
          }
          b.append("]");
          return b.toString();
      }
   }

   @Override
   public int hashCode() {
       final int prime = 31;
       int result = 1;
       result = prime * result + Arrays.hashCode(data);
       result = prime * result + ((version == null) ? 0 : version.hashCode());
       return result;
   }

   @Override
   public boolean equals(Object obj) {
       if (this == obj)
           return true;
       if (obj == null)
           return false;
       if (getClass() != obj.getClass())
           return false;
       OFErrorCauseData other = (OFErrorCauseData) obj;
       if (!Arrays.equals(data, other.data))
           return false;
       if (version != other.version)
           return false;
       return true;
   }

}