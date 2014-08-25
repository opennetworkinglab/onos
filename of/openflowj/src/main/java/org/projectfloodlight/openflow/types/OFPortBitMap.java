package org.projectfloodlight.openflow.types;

import java.util.ArrayList;

import javax.annotation.concurrent.Immutable;


/** User-facing object representing a bitmap of ports that can be matched on.
 *  This is implemented by the custom BSN OXM type of_oxm_bsn_in_ports_182.
 *
 *  You can call set() on the builder for all the Ports you want to match on
 *  and unset to exclude the port.
 *
 *  <b>Implementation note:</b> to comply with the matching semantics of OXM (which is a logical "AND" not "OR")
 *  the underlying match uses a data format which is very unintuitive. The value is always
 *  0, and the mask has the bits set for the ports that should <b>NOT</b> be included in the
 *  range.
 *
 *  For the curious: We transformed the bitmap (a logical OR) problem into a logical
 *  AND NOT problem.
 *
 *  We logically mean:   Inport is 1 OR 3
 *  We technically say:  Inport IS NOT 2 AND IS NOT 4 AND IS NOT 5 AND IS NOT ....
 *  The second term cannot be represented in OXM, the second can.
 *
 *  That said, all that craziness is hidden from the user of this object.
 *
 *  <h2>Usage</h2>
 *  OFPortBitmap is meant to be used with MatchField <tt>BSN_IN_PORTS_128</tt> in place
 *  of the raw type Masked&lt;OFBitMask128&gt;.
 *
 *  <h3>Example:</h3>:
 *  <pre>
 *  OFPortBitMap portBitMap;
 *  Match.Builder matchBuilder;
 *  // initialize
 *  matchBuilder.setMasked(MatchField.BSN_IN_PORTS_128, portBitmap);
 *  </pre>
 *
 * @author Andreas Wundsam <andreas.wundsam@bigswitch.com>
 */
@Immutable
public class OFPortBitMap extends Masked<OFBitMask128> {

    private OFPortBitMap(OFBitMask128 mask) {
        super(OFBitMask128.NONE, mask);
    }

    /** @return whether or not the given port is logically included in the
     *  match, i.e., whether a packet from in-port <emph>port</emph> be matched by
     *  this OXM.
     */
    public boolean isOn(OFPort port) {
        // see the implementation note above about the logical inversion of the mask
        return !(this.mask.isOn(port.getPortNumber()));
    }

    public static OFPortBitMap ofPorts(OFPort... ports) {
        Builder builder = new Builder();
        for (OFPort port: ports) {
            builder.set(port);
        }
        return builder.build();
    }

    /** @return an OFPortBitmap based on the 'mask' part of an OFBitMask128, as, e.g., returned
     *  by the switch.
     **/
    public static OFPortBitMap of(OFBitMask128 mask) {
        return new OFPortBitMap(mask);
    }

    /** @return iterating over all ports that are logically included in the
     *  match, i.e., whether a packet from in-port <emph>port</emph> be matched by
     *  this OXM.
     */
    public Iterable<OFPort> getOnPorts() {
        ArrayList<OFPort> ports = new ArrayList<>();
        for(int i=0; i < 127; i++) {
            if(!(this.mask.isOn(i))) {
                ports.add(OFPort.of(i));
            }
        }
        return ports;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof OFPortBitMap))
            return false;
        OFPortBitMap other = (OFPortBitMap)obj;
        return (other.value.equals(this.value) && other.mask.equals(this.mask));
    }

    @Override
    public int hashCode() {
        return 619 * mask.hashCode() + 257 * value.hashCode();
    }

    public static class Builder {
        private long raw1 = -1, raw2 = -1;

        public Builder() {

        }

        /** @return whether or not the given port is logically included in the
         *  match, i.e., whether a packet from in-port <emph>port</emph> be matched by
         *  this OXM.
         */
        public boolean isOn(OFPort port) {
            // see the implementation note above about the logical inversion of the mask
            return !(OFBitMask128.isBitOn(raw1, raw2, port.getPortNumber()));
        }

        /** remove this port from the match, i.e., packets from this in-port
         *  will NOT be matched.
         */
        public Builder unset(OFPort port) {
            // see the implementation note above about the logical inversion of the mask
            int bit = port.getPortNumber();
            if (bit < 0 || bit > 127)
                throw new IndexOutOfBoundsException("Port number is out of bounds");
            else if (bit == 127)
                // the highest order bit in the bitmask is reserved. The switch will
                // set that bit for all ports >= 127. The reason is that we don't want
                // the OFPortMap to match all ports out of its range (i.e., a packet
                // coming in on port 181 would match *any* OFPortMap).
                throw new IndexOutOfBoundsException("The highest order bit in the bitmask is reserved.");
            else if (bit < 64) {
                raw2 |= ((long)1 << bit);
            } else {
                raw1 |= ((long)1 << (bit - 64));
            }
            return this;
        }

        /** add this port from the match, i.e., packets from this in-port
         *  will NOT be matched.
         */
        public Builder set(OFPort port) {
            // see the implementation note above about the logical inversion of the mask
            int bit = port.getPortNumber();
            if (bit < 0 || bit > 127)
                throw new IndexOutOfBoundsException("Port number is out of bounds");
            else if (bit == 127)
                // the highest order bit in the bitmask is reserved. The switch will
                // set that bit for all ports >= 127. The reason is that we don't want
                // the OFPortMap to match all ports out of its range (i.e., a packet
                // coming in on port 181 would match *any* OFPortMap).
                throw new IndexOutOfBoundsException("The highest order bit in the bitmask is reserved.");
            else if (bit < 64) {
                raw2 &= ~((long)1 << bit);
            } else {
                raw1 &= ~((long)1 << (bit - 64));
            }
            return this;
        }

        public OFPortBitMap build() {
            return new OFPortBitMap(OFBitMask128.of(raw1, raw2));
        }
    }

}
