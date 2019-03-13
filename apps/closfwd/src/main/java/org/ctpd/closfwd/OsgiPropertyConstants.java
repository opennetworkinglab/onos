package org.ctpd.closfwd;

public final class OsgiPropertyConstants {
    private OsgiPropertyConstants() {
    }

    public static final String FLOW_TIMEOUT = "flowTimeout";
    public static final int DEFAULT_FLOW_TIMEOUT = 0;

    public static final String FLOW_PRIORITY = "flowPriority";
    public static final int DEFAULT_FLOW_PRIORITY = 10;

    public static final String BYPASS_FLOW_PRIORITY = "bypassFlowPriority";
    public static final int DEFAULT_BYPASS_FLOW_PRIORITY = 40001;

    public static final String CTPD_FAKE_INTERNAL_MAC_ADDRESS = "ctpdFakeInternalMacAddress";
    public static final String DEFAULT_CTPD_FAKE_INTERNAL_MAC_ADDRESS = "22:22:22:00:00:00";

    public static final String CTPD_FAKE_EXTERNAL_MAC_ADDRESS = "ctpdFakeExternalMacAddress";
    public static final String DEFAULT_CTPD_FAKE_EXTERNAL_MAC_ADDRESS = "22:22:22:00:00:01";

    public static final String VPDC_CLIENT_PREFIX_LENGTH = "vpdcClientPrefixlength";
    public static final int DEFAULT_VPDC_CLIENT_PREFIX_LENGTH = 64;

    public static final String L1_OPENFLOW_SWITCH_PREFIX = "l1OpenflowSwitchPrefix";
    public static final String DEFAULT_L1_OPENFLOW_SWITCH_PREFIX = "of:0000000000000001";

    public static final String L4_OPENFLOW_SWITCH_PREFIX = "l4OpenflowSwitchPrefix";
    public static final String DEFAULT_L4_OPENFLOW_SWITCH_PREFIX = "of:0000000000000004";

    public static final String SPINE_OPENFLOW_SWITCH_PREFIX = "spineOpenflowSwitchPrefix";
    public static final String DEFAULT_SPINE_OPENFLOW_SWITCH_PREFIX = "of:0000000000000f";

    public static final String OLT_OPENFLOW_SWITCH_PREFIX = "oltOpenflowSwitchPrefix";
    public static final String DEFAULT_OLT_OPENFLOW_SWITCH_PREFIX = "of:0001";

    public static final String NEIGHBOUR_SOLICITATION_INTERVAL = "neighbourSolicitationInterval";
    public static final int DEFAULT_NEIGHBOUR_SOLICITATION_INTERVAL = 30000;

    public static final String FLOW_INSTALLATION_TIMEOUT = "flowInstallationTimeout";
    public static final int DEFAULT_FLOW_INSTALLATION_TIMEOUT = 10;

    public static final String OFDPA_ACTIVATED = "ofdpaActivated";
    public static final boolean DEFAULT_OFDPA_ACTIVATED = true;

    public static final String EMPTY_VLAN_ID ="emptyVlanIdP";
    public static final int DEFAULT_EMPTY_VLAN_ID = 3333;

    public static final String SERVICE_VLAN_ID = "serviceVlanId";
    public static final int DEFAULT_SERVICE_VLAN_ID = 2223;

    public static final String BYPASS_VLAN_ID ="bypassVlanIdP";
    public static final int DEFAULT_BYPASS_VLAN_ID = 2222;

    public static final String EXTERNAL_SERVICE_VLAN_ID = "extServiceVlanId";
    public static final int DEFAULT_EXTERNAL_SERVICE_VLAN_ID = 2224;

    public static final String MONOETIQUETA = "monoetiqueta";
    public static final boolean DEFAULT_MONOETIQUETA = false;

    public static final String DEFAULT_VLAN_ID = "defaultVlan";
    public static final int DEFAULT_DEFAULT_VLAN_ID = 23;

    public static final String INTERNET_SERVICES_FLOW_PRIOTITY = "internetServicesFlowPriority";
    public static final int DEFAULT_INTERNET_SERVICES_FLOW_PRIOTITY = 15;

    public static final String RESPOND_NDP_LOCALLY = "respondNDPLocally";
    public static final boolean DEFAULT_RESPOND_NDP_LOCALLY = false;

    public static final String PACKET_PRIORITY_PROCESSOR = "closPacketProcessorPriority";
    public static final int DEFAULT_PACKET_PRIORITY_PROCESSOR = 0;

    public static final String STREAM_ACTIVATED = "streamActivated";
    public static final boolean DEFAULT_STREAM_ACTIVATED = false;

    public static final String CTPD_IPV6 = "ctpdIp";
    public static final String DEFAULT_CTPD_IPV6 = "2001:1498:14::1";

    public static final String CTPD_IPV4 = "ctpdIpv4";
    public static final String DEFAULT_CTPD_IPV4 = "81.47.232.49";

    public static final String BGP_IPV4 = "bgpCtpdIpv4";
    public static final String DEFAULT_BGP_IPV4 = "172.30.127.66";

    public static final String BGP_IPV6 = "bgpCtpdIpv6";
    public static final String DEFAULT_BGP_IPV6 = "2a02:9009:7:ffff::2";

    public static final String CLIENT_SERVICE_IDLE_TIMEOUT = "client2ServiceIdleTimeout";
    public static final int DEFAULT_CLIENT_SERVICE_IDLE_TIMEOUT = 60;

    public static final String KEEP_DATA = "keepData";
    public static final boolean DEFAULT_KEEP_DATA = true;

    public static final String CHECK_LOCAL_NDP = "checkLocalNDP";
    public static final boolean DEFAULT_CHECK_LOCAL_NDP = true;;

    public static final String VPDC_CLIENT_PREFIX = "vpdcClientPrefix";
    public static final String DEFAULT_VPDC_CLIENT_PREFIX = "2a02:9009:4::/48";

    public static final String VPDC_INTERNAL_PREFIX = "vpdcInternalPrefix";
    public static final String DEFAULT_VPDC_INTERNAL_PREFIX = "2a02:9009:6::/48";

    public static final String CTPD_MAC_PREFIX = "ctpdMacPrefix";
    public static final String DEFAULT_CTPD_MAC_PREFIX = "02:";

    public static final String SERVICE_IPV6_PREFIX = "serviceIp6Prefix";
    public static final String DEFAULT_SERVICE_IPV6_PREFIX = "2a02::/16";

    public static final String SERVICE_IPV4_PREFIX = "serviceIp4Prefix";
    public static final String DEFAULT_SERVICE_IPV4_PREFIX = "10.95.227.0/24";

    public static final String FLOW_ID_START = "flowIdStart";
    public static final int DEFAULT_FLOW_ID_START = 500;

    public static final String USE_ECMP = "useEcmp";
    public static final boolean DEFAULT_USE_ECMP = false;

    public static final String PRODUCTION_ENVIRONMENT = "productionEnviorement";
    public static final boolean DEFAULT_PRODUCTION_ENVIRONMENT = false;

    public static final String STORAGE_NETWORK_ENABLED = "storageNetworkEnabled";
    public static final boolean DEFAULT_STORAGE_NETWORK_ENABLED = false;

}





