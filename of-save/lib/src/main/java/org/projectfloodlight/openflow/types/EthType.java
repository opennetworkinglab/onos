package org.projectfloodlight.openflow.types;

import org.jboss.netty.buffer.ChannelBuffer;

import com.google.common.hash.PrimitiveSink;
import com.google.common.primitives.UnsignedInts;


/**
 * EtherType field representation.
 *
 * @author Yotam Harchol (yotam.harchol@bigswitch.com)
 */
public class EthType implements OFValueType<EthType> {
    static final int LENGTH = 2;

    private final int rawValue;

    static final int VAL_IPv4              = 0x0800; // Internet Protocol version 4 (IPv4)
    static final int VAL_ARP               = 0x0806; // Address Resolution Protocol (ARP)
    static final int VAL_WAKE_ON_LAN       = 0x0842; // Wake-on-LAN[3]
    static final int VAL_TRILL             = 0x22F3; // IETF TRILL Protocol
    static final int VAL_DECNET_IV         = 0x6003; // DECnet Phase IV
    static final int VAL_REV_ARP           = 0x8035; // Reverse Address Resolution Protocol
    static final int VAL_APPLE_TALK        = 0x809B; // AppleTalk (Ethertalk)
    static final int VAL_APPLE_TALK_ARP    = 0x80F3; // AppleTalk Address Resolution Protocol (AARP)
    static final int VAL_VLAN_FRAME        = 0x8100; // VLAN-tagged frame (IEEE 802.1Q) & Shortest Path Bridging IEEE 802.1aq[4]
    static final int VAL_IPX_8137          = 0x8137; // IPX
    static final int VAL_IPX_8138          = 0x8138; // IPX
    static final int VAL_QNX               = 0x8204; // QNX Qnet
    static final int VAL_IPv6              = 0x86DD; // Internet Protocol Version 6 (IPv6)
    static final int VAL_ETH_FLOW          = 0x8808; // Ethernet flow control
    static final int VAL_SLOW_PROTOCOLS    = 0x8809; // Slow Protocols (IEEE 802.3)
    static final int VAL_COBRANET          = 0x8819; // CobraNet
    static final int VAL_MPLS_UNICAST      = 0x8847; // MPLS unicast
    static final int VAL_MPLS_MULTICAST    = 0x8848; // MPLS multicast
    static final int VAL_PPPoE_DISCOVERY   = 0x8863; // PPPoE Discovery Stage
    static final int VAL_PPPoE_SESSION     = 0x8864; // PPPoE Session Stage
    static final int VAL_JUMBO_FRAMES      = 0x8870; // Jumbo Frames
    static final int VAL_HOMEPLUG_10       = 0x887B; // HomePlug 1.0 MME
    static final int VAL_EAP_OVER_LAN      = 0x888E; // EAP over LAN (IEEE 802.1X)
    static final int VAL_PROFINET          = 0x8892; // PROFINET Protocol
    static final int VAL_HYPERSCSI         = 0x889A; // HyperSCSI (SCSI over Ethernet)
    static final int VAL_ATA_OVER_ETH      = 0x88A2; // ATA over Ethernet
    static final int VAL_ETHERCAT          = 0x88A4; // EtherCAT Protocol
    static final int VAL_BRIDGING          = 0x88A8; // Provider Bridging (IEEE 802.1ad) & Shortest Path Bridging IEEE 802.1aq[5]
    static final int VAL_POWERLINK         = 0x88AB; // Ethernet Powerlink[citation needed]
    static final int VAL_LLDP              = 0x88CC; // Link Layer Discovery Protocol (LLDP)
    static final int VAL_SERCOS            = 0x88CD; // SERCOS III
    static final int VAL_HOMEPLUG_AV       = 0x88E1; // HomePlug AV MME[citation needed]
    static final int VAL_MRP               = 0x88E3; // Media Redundancy Protocol (IEC62439-2)
    static final int VAL_MAC_SEC           = 0x88E5; // MAC security (IEEE 802.1AE)
    static final int VAL_PTP               = 0x88F7; // Precision Time Protocol (IEEE 1588)
    static final int VAL_CFM               = 0x8902; // IEEE 802.1ag Connectivity Fault Management (CFM) Protocol / ITU-T Recommendation Y.1731 (OAM)
    static final int VAL_FCoE              = 0x8906; // Fibre Channel over Ethernet (FCoE)
    static final int VAL_FCoE_INIT         = 0x8914; // FCoE Initialization Protocol
    static final int VAL_RoCE              = 0x8915; // RDMA over Converged Ethernet (RoCE)
    static final int VAL_HSR               = 0x892F; // High-availability Seamless Redundancy (HSR)
    static final int VAL_CONF_TEST         = 0x9000; // Ethernet Configuration Testing Protocol[6]
    static final int VAL_Q_IN_Q            = 0x9100; // Q-in-Q
    static final int VAL_LLT               = 0xCAFE; // Veritas Low Latency Transport (LLT)[7] for Veritas Cluster Server

    public static final EthType IPv4               = new EthType(VAL_IPv4);
    public static final EthType ARP                = new EthType(VAL_ARP);
    public static final EthType WAKE_ON_LAN        = new EthType(VAL_WAKE_ON_LAN);
    public static final EthType TRILL              = new EthType(VAL_TRILL);
    public static final EthType DECNET_IV          = new EthType(VAL_DECNET_IV);
    public static final EthType REV_ARP            = new EthType(VAL_REV_ARP );
    public static final EthType APPLE_TALK         = new EthType(VAL_APPLE_TALK);
    public static final EthType APPLE_TALK_ARP     = new EthType(VAL_APPLE_TALK_ARP);
    public static final EthType VLAN_FRAME         = new EthType(VAL_VLAN_FRAME );
    public static final EthType IPX_8137           = new EthType(VAL_IPX_8137 );
    public static final EthType IPX_8138           = new EthType(VAL_IPX_8138 );
    public static final EthType QNX                = new EthType(VAL_QNX );
    public static final EthType IPv6               = new EthType(VAL_IPv6 );
    public static final EthType ETH_FLOW           = new EthType(VAL_ETH_FLOW);
    public static final EthType SLOW_PROTOCOLS     = new EthType(VAL_SLOW_PROTOCOLS );
    public static final EthType COBRANET           = new EthType(VAL_COBRANET );
    public static final EthType MPLS_UNICAST       = new EthType(VAL_MPLS_UNICAST );
    public static final EthType MPLS_MULTICAST     = new EthType(VAL_MPLS_MULTICAST );
    public static final EthType PPPoE_DISCOVERY    = new EthType(VAL_PPPoE_DISCOVERY);
    public static final EthType PPPoE_SESSION      = new EthType(VAL_PPPoE_SESSION );
    public static final EthType JUMBO_FRAMES       = new EthType(VAL_JUMBO_FRAMES );
    public static final EthType HOMEPLUG_10        = new EthType(VAL_HOMEPLUG_10 );
    public static final EthType EAP_OVER_LAN       = new EthType(VAL_EAP_OVER_LAN );
    public static final EthType PROFINET           = new EthType(VAL_PROFINET );
    public static final EthType HYPERSCSI          = new EthType(VAL_HYPERSCSI );
    public static final EthType ATA_OVER_ETH       = new EthType(VAL_ATA_OVER_ETH);
    public static final EthType ETHERCAT           = new EthType(VAL_ETHERCAT );
    public static final EthType BRIDGING           = new EthType(VAL_BRIDGING );
    public static final EthType POWERLINK          = new EthType(VAL_POWERLINK );
    public static final EthType LLDP               = new EthType(VAL_LLDP );
    public static final EthType SERCOS             = new EthType(VAL_SERCOS );
    public static final EthType HOMEPLUG_AV        = new EthType(VAL_HOMEPLUG_AV );
    public static final EthType MRP                = new EthType(VAL_MRP );
    public static final EthType MAC_SEC            = new EthType(VAL_MAC_SEC);
    public static final EthType PTP                = new EthType(VAL_PTP );
    public static final EthType CFM                = new EthType(VAL_CFM );
    public static final EthType FCoE               = new EthType(VAL_FCoE );
    public static final EthType FCoE_INIT          = new EthType(VAL_FCoE_INIT );
    public static final EthType RoCE               = new EthType(VAL_RoCE );
    public static final EthType HSR                = new EthType(VAL_HSR );
    public static final EthType CONF_TEST          = new EthType(VAL_CONF_TEST );
    public static final EthType Q_IN_Q             = new EthType(VAL_Q_IN_Q );
    public static final EthType LLT                = new EthType(VAL_LLT );


    private static final int NONE_VAL = 0x0;
    public static final EthType NONE = new EthType(NONE_VAL);

    public static final EthType NO_MASK = new EthType(0xFFFFFFFF);
    public static final EthType FULL_MASK = new EthType(0x00000000);

    private EthType(int type) {
        this.rawValue = type;
    }

    @Override
    public int getLength() {
        return LENGTH;
    }

    public static EthType of(int type) {
        switch (type) {
            case NONE_VAL:
                return NONE;
            case VAL_IPv4:
                return IPv4;
            case VAL_ARP:
                return ARP;
            case VAL_WAKE_ON_LAN:
                return WAKE_ON_LAN;
            case VAL_TRILL:
                return TRILL;
            case VAL_DECNET_IV:
                return DECNET_IV;
            case VAL_REV_ARP:
                return REV_ARP;
            case VAL_APPLE_TALK:
                return APPLE_TALK;
            case VAL_APPLE_TALK_ARP:
                return APPLE_TALK_ARP;
            case VAL_VLAN_FRAME:
                return VLAN_FRAME;
            case VAL_IPX_8137:
                return IPX_8137;
            case VAL_IPX_8138:
                return IPX_8138;
            case VAL_QNX:
                return QNX;
            case VAL_IPv6:
                return IPv6;
            case VAL_ETH_FLOW:
                return ETH_FLOW;
            case VAL_SLOW_PROTOCOLS:
                return SLOW_PROTOCOLS;
            case VAL_COBRANET:
                return COBRANET;
            case VAL_MPLS_UNICAST:
                return MPLS_UNICAST;
            case VAL_MPLS_MULTICAST:
                return MPLS_MULTICAST;
            case VAL_PPPoE_DISCOVERY:
                return PPPoE_DISCOVERY;
            case VAL_PPPoE_SESSION:
                return PPPoE_SESSION;
            case VAL_JUMBO_FRAMES:
                return JUMBO_FRAMES;
            case VAL_HOMEPLUG_10:
                return HOMEPLUG_10;
            case VAL_EAP_OVER_LAN:
                return EAP_OVER_LAN;
            case VAL_PROFINET:
                return PROFINET;
            case VAL_HYPERSCSI:
                return HYPERSCSI;
            case VAL_ATA_OVER_ETH:
                return ATA_OVER_ETH;
            case VAL_ETHERCAT:
                return ETHERCAT;
            case VAL_BRIDGING:
                return BRIDGING;
            case VAL_POWERLINK:
                return POWERLINK;
            case VAL_LLDP:
                return LLDP;
            case VAL_SERCOS:
                return SERCOS;
            case VAL_HOMEPLUG_AV:
                return HOMEPLUG_AV;
            case VAL_MRP:
                return MRP;
            case VAL_MAC_SEC:
                return MAC_SEC;
            case VAL_PTP:
                return PTP;
            case VAL_CFM:
                return CFM;
            case VAL_FCoE:
                return FCoE;
            case VAL_FCoE_INIT:
                return FCoE_INIT;
            case VAL_RoCE:
                return RoCE;
            case VAL_HSR:
                return HSR;
            case VAL_CONF_TEST:
                return CONF_TEST;
            case VAL_Q_IN_Q:
                return Q_IN_Q;
            case VAL_LLT:
                return LLT;
            default:
                // TODO: What's here?
                return new EthType(type);
        }
    }

    @Override
    public String toString() {
        return Integer.toHexString(rawValue);
    }

    public void write2Bytes(ChannelBuffer c) {
        c.writeShort(this.rawValue);
    }

    public static EthType read2Bytes(ChannelBuffer c) {
        return EthType.of(c.readUnsignedShort());
    }

    @Override
    public EthType applyMask(EthType mask) {
        return EthType.of(this.rawValue & mask.rawValue);
    }

    public int getValue() {
        return rawValue;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof EthType))
            return false;
        EthType o = (EthType)obj;
        if (o.rawValue != this.rawValue)
            return false;
        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 37;
        int result = 1;
        result = prime * result + rawValue;
        return result;
    }

    @Override
    public int compareTo(EthType o) {
        return UnsignedInts.compare(rawValue, o.rawValue);
    }

    @Override
    public void putTo(PrimitiveSink sink) {
        sink.putInt(rawValue);
    }


}
