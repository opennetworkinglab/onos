package org.projectfloodlight.openflow.types;

import org.jboss.netty.buffer.ChannelBuffer;

import com.google.common.hash.PrimitiveSink;
import com.google.common.primitives.Shorts;

/**
 * IP-Protocol field representation
 *
 * @author Yotam Harchol (yotam.harchol@bigswitch.com)
 */
public class IpProtocol implements OFValueType<IpProtocol> {

    static final short MAX_PROTO = 0xFF;
    static final int LENGTH = 1;

    private final short proto;

    static final short NUM_HOPOPT  = 0x00;
    static final short NUM_ICMP    = 0x01;
    static final short NUM_IGMP    = 0x02;
    static final short NUM_GGP = 0x03;
    static final short NUM_IPv4    = 0x04;
    static final short NUM_ST  = 0x05;
    static final short NUM_TCP = 0x06;
    static final short NUM_CBT = 0x07;
    static final short NUM_EGP = 0x08;
    static final short NUM_IGP = 0x09;
    static final short NUM_BBN_RCC_MON = 0x0A;
    static final short NUM_NVP_II  = 0x0B;
    static final short NUM_PUP = 0x0C;
    static final short NUM_ARGUS   = 0x0D;
    static final short NUM_EMCON   = 0x0E;
    static final short NUM_XNET    = 0x0F;
    static final short NUM_CHAOS   = 0x10;
    static final short NUM_UDP = 0x11;
    static final short NUM_MUX = 0x12;
    static final short NUM_DCN_MEAS    = 0x13;
    static final short NUM_HMP = 0x14;
    static final short NUM_PRM = 0x15;
    static final short NUM_XNS_IDP = 0x16;
    static final short NUM_TRUNK_1 = 0x17;
    static final short NUM_TRUNK_2 = 0x18;
    static final short NUM_LEAF_1  = 0x19;
    static final short NUM_LEAF_2  = 0x1A;
    static final short NUM_RDP = 0x1B;
    static final short NUM_IRTP    = 0x1C;
    static final short NUM_ISO_TP4 = 0x1D;
    static final short NUM_NETBLT  = 0x1E;
    static final short NUM_MFE_NSP = 0x1F;
    static final short NUM_MERIT_INP   = 0x20;
    static final short NUM_DCCP    = 0x21;
    static final short NUM_3PC = 0x22;
    static final short NUM_IDPR    = 0x23;
    static final short NUM_XTP = 0x24;
    static final short NUM_DDP = 0x25;
    static final short NUM_IDPR_CMTP   = 0x26;
    static final short NUM_TP_PP   = 0x27;
    static final short NUM_IL  = 0x28;
    static final short NUM_IPv6    = 0x29;
    static final short NUM_SDRP    = 0x2A;
    static final short NUM_IPv6_ROUTE  = 0x2B;
    static final short NUM_IPv6_FRAG   = 0x2C;
    static final short NUM_IDRP    = 0x2D;
    static final short NUM_RSVP    = 0x2E;
    static final short NUM_GRE = 0x2F;
    static final short NUM_MHRP    = 0x30;
    static final short NUM_BNA = 0x31;
    static final short NUM_ESP = 0x32;
    static final short NUM_AH  = 0x33;
    static final short NUM_I_NLSP  = 0x34;
    static final short NUM_SWIPE   = 0x35;
    static final short NUM_NARP    = 0x36;
    static final short NUM_MOBILE  = 0x37;
    static final short NUM_TLSP    = 0x38;
    static final short NUM_SKIP    = 0x39;
    static final short NUM_IPv6_ICMP   = 0x3A;
    static final short NUM_IPv6_NO_NXT = 0x3B;
    static final short NUM_IPv6_OPTS   = 0x3C;
    static final short NUM_HOST_INTERNAL   = 0x3D;
    static final short NUM_CFTP    = 0x3E;
    static final short NUM_LOCAL_NET   = 0x3F;
    static final short NUM_SAT_EXPAK   = 0x40;
    static final short NUM_KRYPTOLAN   = 0x41;
    static final short NUM_RVD = 0x42;
    static final short NUM_IPPC    = 0x43;
    static final short NUM_DIST_FS = 0x44;
    static final short NUM_SAT_MON = 0x45;
    static final short NUM_VISA    = 0x46;
    static final short NUM_IPCV    = 0x47;
    static final short NUM_CPNX    = 0x48;
    static final short NUM_CPHB    = 0x49;
    static final short NUM_WSN = 0x4A;
    static final short NUM_PVP = 0x4B;
    static final short NUM_BR_SAT_MON  = 0x4C;
    static final short NUM_SUN_ND  = 0x4D;
    static final short NUM_WB_MON  = 0x4E;
    static final short NUM_WB_EXPAK    = 0x4F;
    static final short NUM_ISO_IP  = 0x50;
    static final short NUM_VMTP    = 0x51;
    static final short NUM_SECURE_VMTP = 0x52;
    static final short NUM_VINES   = 0x53;
    static final short NUM_TTP_IPTM = 0x54;
    static final short NUM_NSFNET_IGP  = 0x55;
    static final short NUM_DGP = 0x56;
    static final short NUM_TCF = 0x57;
    static final short NUM_EIGRP   = 0x58;
    static final short NUM_OSPF    = 0x59;
    static final short NUM_Sprite_RPC  = 0x5A;
    static final short NUM_LARP    = 0x5B;
    static final short NUM_MTP = 0x5C;
    static final short NUM_AX_25   = 0x5D;
    static final short NUM_IPIP    = 0x5E;
    static final short NUM_MICP    = 0x5F;
    static final short NUM_SCC_SP  = 0x60;
    static final short NUM_ETHERIP = 0x61;
    static final short NUM_ENCAP   = 0x62;
    static final short NUM_PRIVATE_ENCRYPT = 0x63;
    static final short NUM_GMTP    = 0x64;
    static final short NUM_IFMP    = 0x65;
    static final short NUM_PNNI    = 0x66;
    static final short NUM_PIM = 0x67;
    static final short NUM_ARIS    = 0x68;
    static final short NUM_SCPS    = 0x69;
    static final short NUM_QNX = 0x6A;
    static final short NUM_A_N = 0x6B;
    static final short NUM_IP_COMP = 0x6C;
    static final short NUM_SNP = 0x6D;
    static final short NUM_COMPAQ_PEER = 0x6E;
    static final short NUM_IPX_IN_IP   = 0x6F;
    static final short NUM_VRRP    = 0x70;
    static final short NUM_PGM = 0x71;
    static final short NUM_ZERO_HOP    = 0x72;
    static final short NUM_L2TP    = 0x73;
    static final short NUM_DDX = 0x74;
    static final short NUM_IATP    = 0x75;
    static final short NUM_STP = 0x76;
    static final short NUM_SRP = 0x77;
    static final short NUM_UTI = 0x78;
    static final short NUM_SMP = 0x79;
    static final short NUM_SM  = 0x7A;
    static final short NUM_PTP = 0x7B;
    static final short NUM_IS_IS_OVER_IPv4 = 0x7C;
    static final short NUM_FIRE    = 0x7D;
    static final short NUM_CRTP    = 0x7E;
    static final short NUM_CRUDP   = 0x7F;
    static final short NUM_SSCOPMCE    = 0x80;
    static final short NUM_IPLT    = 0x81;
    static final short NUM_SPS = 0x82;
    static final short NUM_PIPE    = 0x83;
    static final short NUM_SCTP    = 0x84;
    static final short NUM_FC  = 0x85;
    static final short NUM_RSVP_E2E_IGNORE = 0x86;
    static final short NUM_MOBILITY_HEADER = 0x87;
    static final short NUM_UDP_LITE    = 0x88;
    static final short NUM_MPLS_IN_IP  = 0x89;
    static final short NUM_MANET   = 0x8A;
    static final short NUM_HIP = 0x8B;
    static final short NUM_SHIM6   = 0x8C;

    public static final IpProtocol HOPOPT = new IpProtocol(NUM_HOPOPT);
    public static final IpProtocol ICMP = new IpProtocol(NUM_ICMP);
    public static final IpProtocol IGMP = new IpProtocol(NUM_IGMP);
    public static final IpProtocol GGP = new IpProtocol(NUM_GGP);
    public static final IpProtocol IPv4 = new IpProtocol(NUM_IPv4);
    public static final IpProtocol ST = new IpProtocol(NUM_ST);
    public static final IpProtocol TCP = new IpProtocol(NUM_TCP);
    public static final IpProtocol CBT = new IpProtocol(NUM_CBT);
    public static final IpProtocol EGP = new IpProtocol(NUM_EGP);
    public static final IpProtocol IGP = new IpProtocol(NUM_IGP);
    public static final IpProtocol BBN_RCC_MON = new IpProtocol(NUM_BBN_RCC_MON);
    public static final IpProtocol NVP_II = new IpProtocol(NUM_NVP_II);
    public static final IpProtocol PUP = new IpProtocol(NUM_PUP);
    public static final IpProtocol ARGUS = new IpProtocol(NUM_ARGUS);
    public static final IpProtocol EMCON = new IpProtocol(NUM_EMCON);
    public static final IpProtocol XNET = new IpProtocol(NUM_XNET);
    public static final IpProtocol CHAOS = new IpProtocol(NUM_CHAOS);
    public static final IpProtocol UDP = new IpProtocol(NUM_UDP);
    public static final IpProtocol MUX = new IpProtocol(NUM_MUX);
    public static final IpProtocol DCN_MEAS = new IpProtocol(NUM_DCN_MEAS);
    public static final IpProtocol HMP = new IpProtocol(NUM_HMP);
    public static final IpProtocol PRM = new IpProtocol(NUM_PRM);
    public static final IpProtocol XNS_IDP = new IpProtocol(NUM_XNS_IDP);
    public static final IpProtocol TRUNK_1 = new IpProtocol(NUM_TRUNK_1);
    public static final IpProtocol TRUNK_2 = new IpProtocol(NUM_TRUNK_2);
    public static final IpProtocol LEAF_1 = new IpProtocol(NUM_LEAF_1);
    public static final IpProtocol LEAF_2 = new IpProtocol(NUM_LEAF_2);
    public static final IpProtocol RDP = new IpProtocol(NUM_RDP);
    public static final IpProtocol IRTP = new IpProtocol(NUM_IRTP);
    public static final IpProtocol ISO_TP4 = new IpProtocol(NUM_ISO_TP4);
    public static final IpProtocol NETBLT = new IpProtocol(NUM_NETBLT);
    public static final IpProtocol MFE_NSP = new IpProtocol(NUM_MFE_NSP);
    public static final IpProtocol MERIT_INP = new IpProtocol(NUM_MERIT_INP);
    public static final IpProtocol DCCP = new IpProtocol(NUM_DCCP);
    public static final IpProtocol _3PC = new IpProtocol(NUM_3PC);
    public static final IpProtocol IDPR = new IpProtocol(NUM_IDPR);
    public static final IpProtocol XTP = new IpProtocol(NUM_XTP);
    public static final IpProtocol DDP = new IpProtocol(NUM_DDP);
    public static final IpProtocol IDPR_CMTP = new IpProtocol(NUM_IDPR_CMTP);
    public static final IpProtocol TP_PP = new IpProtocol(NUM_TP_PP);
    public static final IpProtocol IL = new IpProtocol(NUM_IL);
    public static final IpProtocol IPv6 = new IpProtocol(NUM_IPv6);
    public static final IpProtocol SDRP = new IpProtocol(NUM_SDRP);
    public static final IpProtocol IPv6_ROUTE = new IpProtocol(NUM_IPv6_ROUTE);
    public static final IpProtocol IPv6_FRAG = new IpProtocol(NUM_IPv6_FRAG);
    public static final IpProtocol IDRP = new IpProtocol(NUM_IDRP);
    public static final IpProtocol RSVP = new IpProtocol(NUM_RSVP);
    public static final IpProtocol GRE = new IpProtocol(NUM_GRE);
    public static final IpProtocol MHRP = new IpProtocol(NUM_MHRP);
    public static final IpProtocol BNA = new IpProtocol(NUM_BNA);
    public static final IpProtocol ESP = new IpProtocol(NUM_ESP);
    public static final IpProtocol AH = new IpProtocol(NUM_AH);
    public static final IpProtocol I_NLSP = new IpProtocol(NUM_I_NLSP);
    public static final IpProtocol SWIPE = new IpProtocol(NUM_SWIPE);
    public static final IpProtocol NARP = new IpProtocol(NUM_NARP);
    public static final IpProtocol MOBILE = new IpProtocol(NUM_MOBILE);
    public static final IpProtocol TLSP = new IpProtocol(NUM_TLSP);
    public static final IpProtocol SKIP = new IpProtocol(NUM_SKIP);
    public static final IpProtocol IPv6_ICMP = new IpProtocol(NUM_IPv6_ICMP);
    public static final IpProtocol IPv6_NO_NXT = new IpProtocol(NUM_IPv6_NO_NXT);
    public static final IpProtocol IPv6_OPTS = new IpProtocol(NUM_IPv6_OPTS);
    public static final IpProtocol HOST_INTERNAL = new IpProtocol(NUM_HOST_INTERNAL);
    public static final IpProtocol CFTP = new IpProtocol(NUM_CFTP);
    public static final IpProtocol LOCAL_NET = new IpProtocol(NUM_LOCAL_NET);
    public static final IpProtocol SAT_EXPAK = new IpProtocol(NUM_SAT_EXPAK);
    public static final IpProtocol KRYPTOLAN = new IpProtocol(NUM_KRYPTOLAN);
    public static final IpProtocol RVD = new IpProtocol(NUM_RVD);
    public static final IpProtocol IPPC = new IpProtocol(NUM_IPPC);
    public static final IpProtocol DIST_FS = new IpProtocol(NUM_DIST_FS);
    public static final IpProtocol SAT_MON = new IpProtocol(NUM_SAT_MON);
    public static final IpProtocol VISA = new IpProtocol(NUM_VISA);
    public static final IpProtocol IPCV = new IpProtocol(NUM_IPCV);
    public static final IpProtocol CPNX = new IpProtocol(NUM_CPNX);
    public static final IpProtocol CPHB = new IpProtocol(NUM_CPHB);
    public static final IpProtocol WSN = new IpProtocol(NUM_WSN);
    public static final IpProtocol PVP = new IpProtocol(NUM_PVP);
    public static final IpProtocol BR_SAT_MON = new IpProtocol(NUM_BR_SAT_MON);
    public static final IpProtocol SUN_ND = new IpProtocol(NUM_SUN_ND);
    public static final IpProtocol WB_MON = new IpProtocol(NUM_WB_MON);
    public static final IpProtocol WB_EXPAK = new IpProtocol(NUM_WB_EXPAK);
    public static final IpProtocol ISO_IP = new IpProtocol(NUM_ISO_IP);
    public static final IpProtocol VMTP = new IpProtocol(NUM_VMTP);
    public static final IpProtocol SECURE_VMTP = new IpProtocol(NUM_SECURE_VMTP);
    public static final IpProtocol VINES = new IpProtocol(NUM_VINES);
    public static final IpProtocol TTP_IPTM = new IpProtocol(NUM_TTP_IPTM);
    public static final IpProtocol NSFNET_IGP = new IpProtocol(NUM_NSFNET_IGP);
    public static final IpProtocol DGP = new IpProtocol(NUM_DGP);
    public static final IpProtocol TCF = new IpProtocol(NUM_TCF);
    public static final IpProtocol EIGRP = new IpProtocol(NUM_EIGRP);
    public static final IpProtocol OSPF = new IpProtocol(NUM_OSPF);
    public static final IpProtocol Sprite_RPC = new IpProtocol(NUM_Sprite_RPC);
    public static final IpProtocol LARP = new IpProtocol(NUM_LARP);
    public static final IpProtocol MTP = new IpProtocol(NUM_MTP);
    public static final IpProtocol AX_25 = new IpProtocol(NUM_AX_25);
    public static final IpProtocol IPIP = new IpProtocol(NUM_IPIP);
    public static final IpProtocol MICP = new IpProtocol(NUM_MICP);
    public static final IpProtocol SCC_SP = new IpProtocol(NUM_SCC_SP);
    public static final IpProtocol ETHERIP = new IpProtocol(NUM_ETHERIP);
    public static final IpProtocol ENCAP = new IpProtocol(NUM_ENCAP);
    public static final IpProtocol PRIVATE_ENCRYPT = new IpProtocol(NUM_PRIVATE_ENCRYPT);
    public static final IpProtocol GMTP = new IpProtocol(NUM_GMTP);
    public static final IpProtocol IFMP = new IpProtocol(NUM_IFMP);
    public static final IpProtocol PNNI = new IpProtocol(NUM_PNNI);
    public static final IpProtocol PIM = new IpProtocol(NUM_PIM);
    public static final IpProtocol ARIS = new IpProtocol(NUM_ARIS);
    public static final IpProtocol SCPS = new IpProtocol(NUM_SCPS);
    public static final IpProtocol QNX = new IpProtocol(NUM_QNX);
    public static final IpProtocol A_N = new IpProtocol(NUM_A_N);
    public static final IpProtocol IP_COMP = new IpProtocol(NUM_IP_COMP);
    public static final IpProtocol SNP = new IpProtocol(NUM_SNP);
    public static final IpProtocol COMPAQ_PEER = new IpProtocol(NUM_COMPAQ_PEER);
    public static final IpProtocol IPX_IN_IP = new IpProtocol(NUM_IPX_IN_IP);
    public static final IpProtocol VRRP = new IpProtocol(NUM_VRRP);
    public static final IpProtocol PGM = new IpProtocol(NUM_PGM);
    public static final IpProtocol ZERO_HOP = new IpProtocol(NUM_ZERO_HOP);
    public static final IpProtocol L2TP = new IpProtocol(NUM_L2TP);
    public static final IpProtocol DDX = new IpProtocol(NUM_DDX);
    public static final IpProtocol IATP = new IpProtocol(NUM_IATP);
    public static final IpProtocol STP = new IpProtocol(NUM_STP);
    public static final IpProtocol SRP = new IpProtocol(NUM_SRP);
    public static final IpProtocol UTI = new IpProtocol(NUM_UTI);
    public static final IpProtocol SMP = new IpProtocol(NUM_SMP);
    public static final IpProtocol SM = new IpProtocol(NUM_SM);
    public static final IpProtocol PTP = new IpProtocol(NUM_PTP);
    public static final IpProtocol IS_IS_OVER_IPv4 = new IpProtocol(NUM_IS_IS_OVER_IPv4);
    public static final IpProtocol FIRE = new IpProtocol(NUM_FIRE);
    public static final IpProtocol CRTP = new IpProtocol(NUM_CRTP);
    public static final IpProtocol CRUDP = new IpProtocol(NUM_CRUDP);
    public static final IpProtocol SSCOPMCE = new IpProtocol(NUM_SSCOPMCE);
    public static final IpProtocol IPLT = new IpProtocol(NUM_IPLT);
    public static final IpProtocol SPS = new IpProtocol(NUM_SPS);
    public static final IpProtocol PIPE = new IpProtocol(NUM_PIPE);
    public static final IpProtocol SCTP = new IpProtocol(NUM_SCTP);
    public static final IpProtocol FC = new IpProtocol(NUM_FC);
    public static final IpProtocol RSVP_E2E_IGNORE = new IpProtocol(NUM_RSVP_E2E_IGNORE);
    public static final IpProtocol MOBILITY_HEADER = new IpProtocol(NUM_MOBILITY_HEADER);
    public static final IpProtocol UDP_LITE = new IpProtocol(NUM_UDP_LITE);
    public static final IpProtocol MPLS_IN_IP = new IpProtocol(NUM_MPLS_IN_IP);
    public static final IpProtocol MANET = new IpProtocol(NUM_MANET);
    public static final IpProtocol HIP = new IpProtocol(NUM_HIP);
    public static final IpProtocol SHIM6 = new IpProtocol(NUM_SHIM6);

    public static final IpProtocol NONE = HOPOPT;

    public static final IpProtocol NO_MASK = HOPOPT;
    public static final IpProtocol FULL_MASK = new IpProtocol((short)0x0000);

    private IpProtocol(short version) {
        this.proto = version;
    }


    @Override
    public int getLength() {
        return LENGTH;
    }

    public static IpProtocol of(short proto) {
        switch (proto) {
            case NUM_HOPOPT:
                return HOPOPT;
            case NUM_ICMP:
                return ICMP;
            case NUM_IGMP:
                return IGMP;
            case NUM_GGP:
                return GGP;
            case NUM_IPv4:
                return IPv4;
            case NUM_ST:
                return ST;
            case NUM_TCP:
                return TCP;
            case NUM_CBT:
                return CBT;
            case NUM_EGP:
                return EGP;
            case NUM_IGP:
                return IGP;
            case NUM_BBN_RCC_MON:
                return BBN_RCC_MON;
            case NUM_NVP_II:
                return NVP_II;
            case NUM_PUP:
                return PUP;
            case NUM_ARGUS:
                return ARGUS;
            case NUM_EMCON:
                return EMCON;
            case NUM_XNET:
                return XNET;
            case NUM_CHAOS:
                return CHAOS;
            case NUM_UDP:
                return UDP;
            case NUM_MUX:
                return MUX;
            case NUM_DCN_MEAS:
                return DCN_MEAS;
            case NUM_HMP:
                return HMP;
            case NUM_PRM:
                return PRM;
            case NUM_XNS_IDP:
                return XNS_IDP;
            case NUM_TRUNK_1:
                return TRUNK_1;
            case NUM_TRUNK_2:
                return TRUNK_2;
            case NUM_LEAF_1:
                return LEAF_1;
            case NUM_LEAF_2:
                return LEAF_2;
            case NUM_RDP:
                return RDP;
            case NUM_IRTP:
                return IRTP;
            case NUM_ISO_TP4:
                return ISO_TP4;
            case NUM_NETBLT:
                return NETBLT;
            case NUM_MFE_NSP:
                return MFE_NSP;
            case NUM_MERIT_INP:
                return MERIT_INP;
            case NUM_DCCP:
                return DCCP;
            case NUM_3PC:
                return _3PC;
            case NUM_IDPR:
                return IDPR;
            case NUM_XTP:
                return XTP;
            case NUM_DDP:
                return DDP;
            case NUM_IDPR_CMTP:
                return IDPR_CMTP;
            case NUM_TP_PP:
                return TP_PP;
            case NUM_IL:
                return IL;
            case NUM_IPv6:
                return IPv6;
            case NUM_SDRP:
                return SDRP;
            case NUM_IPv6_ROUTE:
                return IPv6_ROUTE;
            case NUM_IPv6_FRAG:
                return IPv6_FRAG;
            case NUM_IDRP:
                return IDRP;
            case NUM_RSVP:
                return RSVP;
            case NUM_GRE:
                return GRE;
            case NUM_MHRP:
                return MHRP;
            case NUM_BNA:
                return BNA;
            case NUM_ESP:
                return ESP;
            case NUM_AH:
                return AH;
            case NUM_I_NLSP:
                return I_NLSP;
            case NUM_SWIPE:
                return SWIPE;
            case NUM_NARP:
                return NARP;
            case NUM_MOBILE:
                return MOBILE;
            case NUM_TLSP:
                return TLSP;
            case NUM_SKIP:
                return SKIP;
            case NUM_IPv6_ICMP:
                return IPv6_ICMP;
            case NUM_IPv6_NO_NXT:
                return IPv6_NO_NXT;
            case NUM_IPv6_OPTS:
                return IPv6_OPTS;
            case NUM_HOST_INTERNAL:
                return HOST_INTERNAL;
            case NUM_CFTP:
                return CFTP;
            case NUM_LOCAL_NET:
                return LOCAL_NET;
            case NUM_SAT_EXPAK:
                return SAT_EXPAK;
            case NUM_KRYPTOLAN:
                return KRYPTOLAN;
            case NUM_RVD:
                return RVD;
            case NUM_IPPC:
                return IPPC;
            case NUM_DIST_FS:
                return DIST_FS;
            case NUM_SAT_MON:
                return SAT_MON;
            case NUM_VISA:
                return VISA;
            case NUM_IPCV:
                return IPCV;
            case NUM_CPNX:
                return CPNX;
            case NUM_CPHB:
                return CPHB;
            case NUM_WSN:
                return WSN;
            case NUM_PVP:
                return PVP;
            case NUM_BR_SAT_MON:
                return BR_SAT_MON;
            case NUM_SUN_ND:
                return SUN_ND;
            case NUM_WB_MON:
                return WB_MON;
            case NUM_WB_EXPAK:
                return WB_EXPAK;
            case NUM_ISO_IP:
                return ISO_IP;
            case NUM_VMTP:
                return VMTP;
            case NUM_SECURE_VMTP:
                return SECURE_VMTP;
            case NUM_VINES:
                return VINES;
            case NUM_TTP_IPTM:
                return TTP_IPTM;
            case NUM_NSFNET_IGP:
                return NSFNET_IGP;
            case NUM_DGP:
                return DGP;
            case NUM_TCF:
                return TCF;
            case NUM_EIGRP:
                return EIGRP;
            case NUM_OSPF:
                return OSPF;
            case NUM_Sprite_RPC:
                return Sprite_RPC;
            case NUM_LARP:
                return LARP;
            case NUM_MTP:
                return MTP;
            case NUM_AX_25:
                return AX_25;
            case NUM_IPIP:
                return IPIP;
            case NUM_MICP:
                return MICP;
            case NUM_SCC_SP:
                return SCC_SP;
            case NUM_ETHERIP:
                return ETHERIP;
            case NUM_ENCAP:
                return ENCAP;
            case NUM_PRIVATE_ENCRYPT:
                return PRIVATE_ENCRYPT;
            case NUM_GMTP:
                return GMTP;
            case NUM_IFMP:
                return IFMP;
            case NUM_PNNI:
                return PNNI;
            case NUM_PIM:
                return PIM;
            case NUM_ARIS:
                return ARIS;
            case NUM_SCPS:
                return SCPS;
            case NUM_QNX:
                return QNX;
            case NUM_A_N:
                return A_N;
            case NUM_IP_COMP:
                return IP_COMP;
            case NUM_SNP:
                return SNP;
            case NUM_COMPAQ_PEER:
                return COMPAQ_PEER;
            case NUM_IPX_IN_IP:
                return IPX_IN_IP;
            case NUM_VRRP:
                return VRRP;
            case NUM_PGM:
                return PGM;
            case NUM_ZERO_HOP:
                return ZERO_HOP;
            case NUM_L2TP:
                return L2TP;
            case NUM_DDX:
                return DDX;
            case NUM_IATP:
                return IATP;
            case NUM_STP:
                return STP;
            case NUM_SRP:
                return SRP;
            case NUM_UTI:
                return UTI;
            case NUM_SMP:
                return SMP;
            case NUM_SM:
                return SM;
            case NUM_PTP:
                return PTP;
            case NUM_IS_IS_OVER_IPv4:
                return IS_IS_OVER_IPv4;
            case NUM_FIRE:
                return FIRE;
            case NUM_CRTP:
                return CRTP;
            case NUM_CRUDP:
                return CRUDP;
            case NUM_SSCOPMCE:
                return SSCOPMCE;
            case NUM_IPLT:
                return IPLT;
            case NUM_SPS:
                return SPS;
            case NUM_PIPE:
                return PIPE;
            case NUM_SCTP:
                return SCTP;
            case NUM_FC:
                return FC;
            case NUM_RSVP_E2E_IGNORE:
                return RSVP_E2E_IGNORE;
            case NUM_MOBILITY_HEADER:
                return MOBILITY_HEADER;
            case NUM_UDP_LITE:
                return UDP_LITE;
            case NUM_MPLS_IN_IP:
                return MPLS_IN_IP;
            case NUM_MANET:
                return MANET;
            case NUM_HIP:
                return HIP;
            case NUM_SHIM6:
                return SHIM6;
            default:
                if (proto >= MAX_PROTO) {
                    throw new IllegalArgumentException("Illegal IP protocol number: "
                            + proto);
                } else {
                    return new IpProtocol(proto);
                }
        }
    }

    @Override
    public String toString() {
        return Integer.toHexString(proto);
    }

    public void writeByte(ChannelBuffer c) {
        c.writeByte(this.proto);
    }

    public static IpProtocol readByte(ChannelBuffer c) {
        return IpProtocol.of(c.readUnsignedByte());
    }

    @Override
    public IpProtocol applyMask(IpProtocol mask) {
        return IpProtocol.of((short)(this.proto & mask.proto));
    }

    public short getIpProtocolNumber() {
        return proto;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof IpProtocol))
            return false;
        IpProtocol o = (IpProtocol)obj;
        if (o.proto != this.proto)
            return false;
        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 37;
        int result = 1;
        result = prime * result + proto;
        return result;
    }


    @Override
    public int compareTo(IpProtocol o) {
        return Shorts.compare(proto, o.proto);
    }


    @Override
    public void putTo(PrimitiveSink sink) {
        sink.putShort(proto);
    }

}