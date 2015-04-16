package org.onlab.packet;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

public class MPLS extends BasePacket {
    public static final int ADDRESS_LENGTH = 4;
    public static final byte PROTOCOL_IPV4 = 0x1;
    public static final byte PROTOCOL_MPLS = 0x6;
    public static final Map<Byte, Class<? extends IPacket>> PROTOCOL_CLASS_MAP;

    static {
        PROTOCOL_CLASS_MAP = new HashMap<Byte, Class<? extends IPacket>>();
        PROTOCOL_CLASS_MAP.put(PROTOCOL_IPV4, IPv4.class);
        PROTOCOL_CLASS_MAP.put(PROTOCOL_MPLS, MPLS.class);
    }

    protected int label; //20bits
    protected byte bos; //1bit
    protected byte ttl; //8bits
    protected byte protocol;

    /**
     * Default constructor that sets the version to 4.
     */
    public MPLS() {
        super();
        this.bos = 1;
        this.protocol = PROTOCOL_IPV4;
    }

    @Override
    public byte[] serialize() {
        byte[] payloadData = null;
        if (payload != null) {
            payload.setParent(this);
            payloadData = payload.serialize();
        }

        byte[] data = new byte[(4 + ((payloadData != null) ? payloadData.length : 0)) ];
        ByteBuffer bb = ByteBuffer.wrap(data);

        bb.putInt(((this.label & 0x000fffff) << 12) | ((this.bos & 0x1) << 8 | (this.ttl & 0xff)));
        if (payloadData != null) {
            bb.put(payloadData);
        }

        return data;
    }

    @Override
    public IPacket deserialize(byte[] data, int offset, int length) {
        ByteBuffer bb = ByteBuffer.wrap(data, offset, length);

        int mplsheader = bb.getInt();
        this.label = ((mplsheader & 0xfffff000) >> 12);
        this.bos = (byte) ((mplsheader & 0x00000100) >> 8);
        this.bos = (byte) (mplsheader & 0x000000ff);
        this.protocol = (this.bos == 1) ? PROTOCOL_IPV4 : PROTOCOL_MPLS;

        IPacket payload;
        if (IPv4.PROTOCOL_CLASS_MAP.containsKey(this.protocol)) {
            Class<? extends IPacket> clazz = IPv4.PROTOCOL_CLASS_MAP.get(this.protocol);
            try {
                payload = clazz.newInstance();
            } catch (Exception e) {
                throw new RuntimeException("Error parsing payload for MPLS packet", e);
            }
        } else {
            payload = new Data();
        }
        this.payload = payload.deserialize(data, bb.position(), bb.limit() - bb.position());
        this.payload.setParent(this);

        return this;
    }

    /**
     * Returns the MPLS label.
     *
     * @return MPLS label
     */
    public int getLabel() {
        return label;
    }

    /**
     * Sets the MPLS label.
     *
     * @param label MPLS label
     */
    public void setLabel(int label) {
        this.label = label;
    }

    /**
     * Returns the MPLS TTL of the packet.
     *
     * @return MPLS TTL of the packet
     */
    public byte getTtl() {
        return ttl;
    }

    /**
     * Sets the MPLS TTL of the packet.
     *
     * @param ttl MPLS TTL
     */
    public void setTtl(byte ttl) {
        this.ttl = ttl;
    }

}
