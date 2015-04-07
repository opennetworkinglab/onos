package org.onlab.packet;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import static org.onlab.packet.PacketUtils.checkInput;

public class MPLS extends BasePacket {
    public static final int HEADER_LENGTH = 4;

    public static final byte PROTOCOL_IPV4 = 0x1;
    public static final byte PROTOCOL_MPLS = 0x6;
    static Map<Byte, Deserializer<? extends IPacket>> protocolDeserializerMap
            = new HashMap<>();

    static {
        protocolDeserializerMap.put(PROTOCOL_IPV4, IPv4.deserializer());
        protocolDeserializerMap.put(PROTOCOL_MPLS, MPLS.deserializer());
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

        Deserializer<? extends IPacket> deserializer;
        if (protocolDeserializerMap.containsKey(this.protocol)) {
            deserializer = protocolDeserializerMap.get(this.protocol);
        } else {
            deserializer = Data.deserializer();
        }
        try {
            this.payload = deserializer.deserialize(data, bb.position(), bb.limit() - bb.position());
            this.payload.setParent(this);
        } catch (DeserializationException e) {
            return this;
        }

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

    /**
     * Deserializer function for MPLS packets.
     *
     * @return deserializer function
     */
    public static Deserializer<MPLS> deserializer() {
        return (data, offset, length) -> {
            checkInput(data, offset, length, HEADER_LENGTH);

            MPLS mpls = new MPLS();
            ByteBuffer bb = ByteBuffer.wrap(data, offset, length);

            int mplsheader = bb.getInt();
            mpls.label = ((mplsheader & 0xfffff000) >>> 12);
            mpls.bos = (byte) ((mplsheader & 0x00000100) >> 8);
            mpls.ttl = (byte) (mplsheader & 0x000000ff);
            mpls.protocol = (mpls.bos == 1) ? PROTOCOL_IPV4 : PROTOCOL_MPLS;

            Deserializer<? extends IPacket> deserializer;
            if (protocolDeserializerMap.containsKey(mpls.protocol)) {
                deserializer = protocolDeserializerMap.get(mpls.protocol);
            } else {
                deserializer = Data.deserializer();
            }
            mpls.payload = deserializer.deserialize(data, bb.position(), bb.limit() - bb.position());
            mpls.payload.setParent(mpls);

            return mpls;
        };
    }
}
