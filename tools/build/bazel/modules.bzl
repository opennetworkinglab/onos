UTILS = [
    "//utils/osgiwrap:osgi-jar",
    "//utils/osgi:onlab-osgi",
    "//utils/junit:onlab-junit",
    "//utils/misc:onlab-misc",
    "//utils/rest:onlab-rest",
]

API = [
    "//core/api:onos-api",
]

CORE = UTILS + API + [
    "//core/net:onos-core-net",
    "//core/common:onos-core-common",
    "//core/protobuf/models:onos-core-protobuf-models",
    "//core/protobuf/models/proto:onos-core-protobuf-models-proto",
    "//core/store/primitives:onos-core-primitives",
    "//core/store/serializers:onos-core-serializers",
    "//core/store/dist:onos-core-dist",
    "//core/store/persistence:onos-core-persistence",
    "//cli:onos-cli",
    "//drivers/utilities:onos-drivers-utilities",
    "//providers/general/device:onos-providers-general-device",
    "//web/api:onos-rest",
]

FEATURES = [
    "//tools/package/features:onos-thirdparty-base",
    "//tools/package/features:onos-thirdparty-web",
    "//tools/package/features:onos-api",
    "//tools/package/features:onos-core",
    "//tools/package/features:onos-cli",
    "//tools/package/features:onos-rest",
]

#
# ONOS Profile Maps
#
# To include a JAR or app in a specific profile, add the profile name
# to the list in the maps below. If multiple profiles are listed,
# then it will be included in each profile. Every item included in the
# map will be included in the default profile (build with no profile
# specified).
#

#
# ONOS Protocols and Providers
#

PROTOCOL_MAP = {
    "//protocols/bgp/bgpio:onos-protocols-bgp-bgpio": [],
    "//protocols/bgp/api:onos-protocols-bgp-api": [],
    "//protocols/bgp/ctl:onos-protocols-bgp-ctl": [],
    "//protocols/lisp/api:onos-protocols-lisp-api": [],
    "//protocols/lisp/ctl:onos-protocols-lisp-ctl": [],
    "//protocols/lisp/msg:onos-protocols-lisp-msg": [],
    "//protocols/netconf/api:onos-protocols-netconf-api": [],
    "//protocols/netconf/ctl:onos-protocols-netconf-ctl": [],
    "//protocols/openflow/api:onos-protocols-openflow-api": ["seba", "sona"],
    "//protocols/openflow/ctl:onos-protocols-openflow-ctl": ["seba", "sona"],
    "//protocols/ovsdb/rfc:onos-protocols-ovsdb-rfc": ["sona"],
    "//protocols/ovsdb/api:onos-protocols-ovsdb-api": ["sona"],
    "//protocols/ovsdb/ctl:onos-protocols-ovsdb-ctl": ["sona"],
    "//protocols/p4runtime/api:onos-protocols-p4runtime-api": ["stratum"],
    "//protocols/p4runtime/model:onos-protocols-p4runtime-model": ["stratum"],
    "//protocols/rest/api:onos-protocols-rest-api": [],
    "//protocols/rest/ctl:onos-protocols-rest-ctl": [],
    "//protocols/restconf/client/api:onos-protocols-restconf-client-api": [],
    "//protocols/restconf/client/ctl:onos-protocols-restconf-client-ctl": [],
    "//protocols/snmp/api:onos-protocols-snmp-api": [],
    "//protocols/snmp/ctl:onos-protocols-snmp-ctl": [],
    "//protocols/tl1/api:onos-protocols-tl1-api": [],
    "//protocols/tl1/ctl:onos-protocols-tl1-ctl": [],
    "//protocols/xmpp/core/api:onos-protocols-xmpp-core-api": [],
    "//protocols/xmpp/core/ctl:onos-protocols-xmpp-core-ctl": [],
}

PROTOCOL_APP_MAP = {
    "//protocols/grpc:onos-protocols-grpc-oar": ["stratum", "sdfabric", "sona"],
    "//protocols/gnmi:onos-protocols-gnmi-oar": ["stratum", "sdfabric", "sona"],
    "//protocols/gnoi:onos-protocols-gnoi-oar": ["stratum", "sdfabric"],
    "//protocols/p4runtime:onos-protocols-p4runtime-oar": ["stratum", "sdfabric"],
    "//protocols/restconf/server:onos-protocols-restconf-server-oar": [],
    "//protocols/xmpp/core:onos-protocols-xmpp-core-oar": [],
    "//protocols/xmpp/pubsub:onos-protocols-xmpp-pubsub-oar": [],
}

PROVIDER_MAP = {
    "//providers/netconf/device:onos-providers-netconf-device": [],
    "//providers/openflow/device:onos-providers-openflow-device": ["seba", "sona"],
    "//providers/openflow/packet:onos-providers-openflow-packet": ["seba", "sona"],
    "//providers/openflow/flow:onos-providers-openflow-flow": ["seba", "sona"],
    "//providers/openflow/group:onos-providers-openflow-group": ["seba", "sona"],
    "//providers/openflow/meter:onos-providers-openflow-meter": ["seba", "sona"],
    "//providers/ovsdb/device:onos-providers-ovsdb-device": ["sona"],
    "//providers/ovsdb/tunnel:onos-providers-ovsdb-tunnel": ["sona"],
    "//providers/p4runtime/packet:onos-providers-p4runtime-packet": ["stratum"],
    "//providers/rest/device:onos-providers-rest-device": [],
    "//providers/snmp/device:onos-providers-snmp-device": [],
    "//providers/lisp/device:onos-providers-lisp-device": [],
    "//providers/tl1/device:onos-providers-tl1-device": [],
}

PROVIDER_APP_MAP = {
    "//providers/general:onos-providers-general-oar": ["stratum", "sdfabric", "sona"],
    "//providers/host:onos-providers-host-oar": ["seba", "stratum", "sdfabric", "sona"],
    "//providers/hostprobing:onos-providers-hostprobing-oar": ["seba", "stratum", "sdfabric", "sona"],
    "//providers/link:onos-providers-link-oar": ["stratum"],
    "//providers/lldp:onos-providers-lldp-oar": ["seba", "stratum", "sdfabric", "sona"],
    "//providers/netcfghost:onos-providers-netcfghost-oar": ["seba", "stratum", "sdfabric", "sona"],
    "//providers/netcfglinks:onos-providers-netcfglinks-oar": ["stratum"],
    "//providers/netconf:onos-providers-netconf-oar": [],
    "//providers/null:onos-providers-null-oar": [],
    "//providers/openflow/app:onos-providers-openflow-app-oar": ["seba", "sona"],
    "//providers/openflow/base:onos-providers-openflow-base-oar": ["seba", "sona"],
    "//providers/openflow/message:onos-providers-openflow-message-oar": ["seba", "sona"],
    "//providers/ovsdb:onos-providers-ovsdb-oar": ["sona"],
    "//providers/ovsdb/host:onos-providers-ovsdb-host-oar": ["sona"],
    "//providers/ovsdb/base:onos-providers-ovsdb-base-oar": ["sona"],
    "//providers/p4runtime:onos-providers-p4runtime-oar": ["stratum", "sdfabric"],
    "//providers/rest:onos-providers-rest-oar": [],
    "//providers/snmp:onos-providers-snmp-oar": [],
    "//providers/lisp:onos-providers-lisp-oar": [],
    "//providers/tl1:onos-providers-tl1-oar": [],
    "//providers/xmpp/device:onos-providers-xmpp-device-oar": [],
}

#
# ONOS Drivers
#

DRIVER_MAP = {
    "//drivers/default:onos-drivers-default-oar": ["minimal", "seba", "stratum", "sdfabric", "sona"],
    "//drivers/arista:onos-drivers-arista-oar": [],
    "//drivers/bmv2:onos-drivers-bmv2-oar": ["stratum", "sdfabric"],
    "//drivers/barefoot:onos-drivers-barefoot-oar": ["stratum", "sdfabric"],
    "//drivers/ciena/waveserver:onos-drivers-ciena-waveserver-oar": [],
    "//drivers/ciena/c5162:onos-drivers-ciena-c5162-oar": [],
    "//drivers/ciena/c5170:onos-drivers-ciena-c5170-oar": [],
    "//drivers/ciena/waveserverai:onos-drivers-ciena-waveserverai-oar": [],
    "//drivers/cisco/netconf:onos-drivers-cisco-netconf-oar": [],
    "//drivers/cisco/rest:onos-drivers-cisco-rest-oar": [],
    "//drivers/corsa:onos-drivers-corsa-oar": [],
    "//drivers/czechlight:onos-drivers-czechlight-oar": [],
    "//drivers/flowspec:onos-drivers-flowspec-oar": [],
    "//drivers/fujitsu:onos-drivers-fujitsu-oar": [],
    "//drivers/gnmi:onos-drivers-gnmi-oar": ["stratum", "sdfabric", "sona"],
    "//drivers/gnoi:onos-drivers-gnoi-oar": ["stratum", "sdfabric"],
    "//drivers/hp:onos-drivers-hp-oar": [],
    "//drivers/juniper:onos-drivers-juniper-oar": [],
    "//drivers/lisp:onos-drivers-lisp-oar": [],
    "//drivers/lumentum:onos-drivers-lumentum-oar": [],
    "//drivers/mellanox:onos-drivers-mellanox-oar": ["stratum"],
    "//drivers/netconf:onos-drivers-netconf-oar": [],
    "//drivers/odtn-driver:onos-drivers-odtn-driver-oar": [],
    "//drivers/oplink:onos-drivers-oplink-oar": [],
    "//drivers/optical:onos-drivers-optical-oar": [],
    "//drivers/ovsdb:onos-drivers-ovsdb-oar": ["sona"],
    "//drivers/p4runtime:onos-drivers-p4runtime-oar": ["stratum", "sdfabric"],
    "//drivers/polatis/netconf:onos-drivers-polatis-netconf-oar": [],
    "//drivers/polatis/openflow:onos-drivers-polatis-openflow-oar": [],
    "//drivers/server:onos-drivers-server-oar": [],
    "//drivers/stratum:onos-drivers-stratum-oar": ["stratum", "sdfabric"],
    "//drivers/stratum-odtn:onos-drivers-stratum-odtn-oar": [],
    "//drivers/zte:onos-drivers-zte-oar": [],
}

#
# ONOS Apps and App API JARs
#

APP_JAR_MAP = {
    "//apps/cpman/api:onos-apps-cpman-api": [],
    "//apps/routing-api:onos-apps-routing-api": [],
    "//apps/dhcp/api:onos-apps-dhcp-api": [],
    "//apps/dhcp/app:onos-apps-dhcp-app": [],
    "//apps/imr/api:onos-apps-imr-api": [],
    "//apps/imr/app:onos-apps-imr-app": [],
    "//apps/dhcprelay/app:onos-apps-dhcprelay-app": [],
    "//apps/dhcprelay/web:onos-apps-dhcprelay-web": [],
    "//apps/fwd:onos-apps-fwd": [],
    "//apps/kafka-integration/api:onos-apps-kafka-integration-api": [],
    "//apps/kafka-integration/app:onos-apps-kafka-integration-app": [],
    "//apps/routing/common:onos-apps-routing-common": [],
}

APP_MAP = {
    "//apps/acl:onos-apps-acl-oar": [],
    "//apps/artemis:onos-apps-artemis-oar": [],
    "//apps/bgprouter:onos-apps-bgprouter-oar": [],
    "//apps/castor:onos-apps-castor-oar": [],
    "//apps/cfm:onos-apps-cfm-oar": [],
    "//apps/config:onos-apps-config-oar": [],
    "//apps/cord-support:onos-apps-cord-support-oar": [],
    "//apps/cpman/app:onos-apps-cpman-app-oar": [],
    "//apps/dhcp:onos-apps-dhcp-oar": [],
    "//apps/dhcprelay:onos-apps-dhcprelay-oar": ["sdfabric"],
    "//apps/drivermatrix:onos-apps-drivermatrix-oar": [],
    "//apps/events:onos-apps-events-oar": [],
    "//apps/faultmanagement:onos-apps-faultmanagement-oar": [],
    "//apps/flowanalyzer:onos-apps-flowanalyzer-oar": [],
    "//apps/flowspec-api:onos-apps-flowspec-api-oar": [],
    "//apps/fwd:onos-apps-fwd-oar": [],
    "//apps/gangliametrics:onos-apps-gangliametrics-oar": [],
    "//apps/graphitemetrics:onos-apps-graphitemetrics-oar": [],
    "//apps/imr:onos-apps-imr-oar": [],
    "//apps/inbandtelemetry:onos-apps-inbandtelemetry-oar": ["sdfabric"],
    "//apps/influxdbmetrics:onos-apps-influxdbmetrics-oar": [],
    "//apps/intentsync:onos-apps-intentsync-oar": [],
    "//apps/k8s-networking:onos-apps-k8s-networking-oar": ["sona"],
    "//apps/k8s-node:onos-apps-k8s-node-oar": ["sona"],
    "//apps/kubevirt-networking:onos-apps-kubevirt-networking-oar": ["sona"],
    "//apps/kubevirt-node:onos-apps-kubevirt-node-oar": ["sona"],
    "//apps/kafka-integration:onos-apps-kafka-integration-oar": [],
    "//apps/layout:onos-apps-layout-oar": [],
    "//apps/linkprops:onos-apps-linkprops-oar": [],
    "//apps/mappingmanagement:onos-apps-mappingmanagement-oar": [],
    "//apps/mcast:onos-apps-mcast-oar": ["seba", "sdfabric"],
    "//apps/metrics:onos-apps-metrics-oar": [],
    "//apps/mfwd:onos-apps-mfwd-oar": [],
    "//apps/mlb:onos-apps-mlb-oar": ["sdfabric"],
    "//apps/mobility:onos-apps-mobility-oar": [],
    "//apps/network-troubleshoot:onos-apps-network-troubleshoot-oar": [],
    "//apps/newoptical:onos-apps-newoptical-oar": [],
    "//apps/nodemetrics:onos-apps-nodemetrics-oar": [],
    "//apps/node-diagnosis:onos-apps-node-diagnosis-oar": [],
    "//apps/odtn/api:onos-apps-odtn-api-oar": [],
    "//apps/odtn/service:onos-apps-odtn-service-oar": [],
    "//apps/ofagent:onos-apps-ofagent-oar": [],
    "//apps/onlp-demo:onos-apps-onlp-demo-oar": [],
    "//apps/onos-topo:onos-apps-onos-topo-oar": [],
    "//apps/openroadm:onos-apps-openroadm-oar": [],
    "//apps/openstacknetworking:onos-apps-openstacknetworking-oar": ["sona"],
    "//apps/openstacknetworkingui:onos-apps-openstacknetworkingui-oar": ["sona"],
    "//apps/openstacknode:onos-apps-openstacknode-oar": ["sona"],
    "//apps/openstacktelemetry:onos-apps-openstacktelemetry-oar": ["sona"],
    "//apps/openstacktroubleshoot:onos-apps-openstacktroubleshoot-oar": ["sona"],
    "//apps/openstackvtap:onos-apps-openstackvtap-oar": ["sona"],
    "//apps/optical-model:onos-apps-optical-model-oar": ["seba", "sona"],
    "//apps/optical-rest:onos-apps-optical-rest-oar": [],
    "//apps/p4-tutorial/mytunnel:onos-apps-p4-tutorial-mytunnel-oar": [],
    "//apps/p4-tutorial/pipeconf:onos-apps-p4-tutorial-pipeconf-oar": [],
    "//apps/packet-stats:onos-apps-packet-stats-oar": [],
    "//apps/packet-throttle:onos-apps-packet-throttle-oar": [],
    "//apps/pathpainter:onos-apps-pathpainter-oar": [],
    "//apps/pim:onos-apps-pim-oar": [],
    "//apps/portloadbalancer:onos-apps-portloadbalancer-oar": ["seba", "sdfabric"],
    "//apps/powermanagement:onos-apps-powermanagement-oar": [],
    "//apps/proxyarp:onos-apps-proxyarp-oar": [],
    "//apps/rabbitmq:onos-apps-rabbitmq-oar": [],
    "//apps/reactive-routing:onos-apps-reactive-routing-oar": [],
    "//apps/restconf:onos-apps-restconf-oar": [],
    "//apps/roadm:onos-apps-roadm-oar": [],
    "//apps/route-service:onos-apps-route-service-oar": ["seba", "sdfabric"],
    "//apps/routeradvertisement:onos-apps-routeradvertisement-oar": ["sdfabric"],
    "//apps/routing/cpr:onos-apps-routing-cpr-oar": [],
    "//apps/routing/fibinstaller:onos-apps-routing-fibinstaller-oar": [],
    "//apps/routing/fpm:onos-apps-routing-fpm-oar": ["sdfabric"],
    "//apps/scalablegateway:onos-apps-scalablegateway-oar": [],
    "//apps/sdnip:onos-apps-sdnip-oar": [],
    "//apps/simplefabric:onos-apps-simplefabric-oar": [],
    "//apps/test/cluster-ha:onos-apps-test-cluster-ha-oar": [],
    "//apps/test/demo:onos-apps-test-demo-oar": [],
    "//apps/test/distributed-primitives:onos-apps-test-distributed-primitives-oar": [],
    "//apps/test/election:onos-apps-test-election-oar": [],
    "//apps/test/flow-perf:onos-apps-test-flow-perf-oar": [],
    "//apps/test/intent-perf:onos-apps-test-intent-perf-oar": [],
    "//apps/test/loadtest:onos-apps-test-loadtest-oar": [],
    "//apps/test/messaging-perf:onos-apps-test-messaging-perf-oar": [],
    "//apps/test/netcfg-monitor:onos-apps-test-netcfg-monitor-oar": [],
    "//apps/test/primitive-perf:onos-apps-test-primitive-perf-oar": [],
    "//apps/test/route-scale:onos-apps-test-route-scale-oar": [],
    "//apps/test/transaction-perf:onos-apps-test-transaction-perf-oar": [],
    "//apps/tunnel:onos-apps-tunnel-oar": ["sona"],
    "//apps/virtual:onos-apps-virtual-oar": [],
    "//apps/virtualbng:onos-apps-virtualbng-oar": [],
    "//apps/vpls:onos-apps-vpls-oar": [],
    "//apps/vrouter:onos-apps-vrouter-oar": [],
    "//apps/workflow/ofoverlay:onos-apps-workflow-ofoverlay-oar": [],
    "//apps/workflow:onos-apps-workflow-oar": [],
    "//apps/yang-gui:onos-apps-yang-gui-oar": [],
    "//apps/yang:onos-apps-yang-oar": [],
    "//web/gui:onos-web-gui-oar": ["sona", "sdfabric"],
    "//web/gui2:onos-web-gui2-oar": ["stratum", "sdfabric"],
}

#
# Pipelines and Models
#

PIPELINE_MAP = {
    "//pipelines/basic:onos-pipelines-basic-oar": ["stratum", "sdfabric"],
    "//pipelines/fabric:onos-pipelines-fabric-oar": ["stratum", "sdfabric"],
}

MODELS_MAP = {
    "//models/ietf:onos-models-ietf-oar": [],
    "//models/common:onos-models-common-oar": [],
    "//models/openconfig:onos-models-openconfig-oar": [],
    "//models/openconfig-infinera:onos-models-openconfig-infinera-oar": [],
    "//models/openconfig-odtn:onos-models-openconfig-odtn-oar": [],
    "//models/openroadm:onos-models-openroadm-oar": [],
    "//models/tapi:onos-models-tapi-oar": [],
    "//models/polatis:onos-models-polatis-oar": [],
    "//models/ciena/waveserverai:onos-models-ciena-waveserverai-oar": [],
}

#
# Convenience functions for processing profile maps
#

def filter(map, profile):
    all = not bool(profile) or profile == "all"
    return [k for k, v in map.items() if all or profile in v]

def extensions(profile = None):
    return filter(PROTOCOL_MAP, profile) + filter(PROVIDER_MAP, profile)

def apps(profile = None):
    return filter(PROTOCOL_APP_MAP, profile) + \
           filter(PROVIDER_APP_MAP, profile) + \
           filter(DRIVER_MAP, profile) + \
           filter(APP_MAP, profile) + \
           filter(APP_JAR_MAP, profile) + \
           filter(PIPELINE_MAP, profile) + \
           filter(MODELS_MAP, profile)

#
# Instantiate a config_setting for every profile in the list
#
def profiles(profiles):
    for p in profiles:
        native.config_setting(
            name = "%s_profile" % p,
            values = {"define": "profile=%s" % p},
        )
