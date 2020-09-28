load("//tools/build/bazel:project_javadoc.bzl", "project_javadoc")
load("//tools/build/bazel:generate_workspace.bzl", "CORE_DEPS")
load("//tools/build/bazel:variables.bzl", "ONOS_VERSION")

SOURCES = glob(["src/main/javadoc/doc-files/*"]) + [
    "src/main/javadoc/overview.html",
    "//utils/osgi:onlab-osgi-sources",
    "//utils/junit:onlab-junit-sources",
    "//utils/misc:onlab-misc-sources",
    "//utils/rest:onlab-rest-sources",
    "//core/api:onos-api-sources",
    "//core/net:onos-core-net-sources",
    "//core/common:onos-core-common-sources",
    "//core/store/primitives:onos-core-primitives-sources",
    "//core/store/serializers:onos-core-serializers-sources",
    "//core/store/dist:onos-core-dist-sources",
    # "//core/security:onos-security-sources",
    "//core/store/persistence:onos-core-persistence-sources",
    "//cli:onos-cli-sources",
    "//web/api:onos-rest",
    "//apps/cpman/api:onos-apps-cpman-api-sources",
    "//apps/routing-api:onos-apps-routing-api-sources",
    "//apps/dhcp/api:onos-apps-dhcp-api-sources",
]

JAVADOC_DEPS = CORE_DEPS + KRYO + ATOMIX + REST + CLI + JACKSON + TEST + NETTY + METRICS + ONOS_YANG + [
    "@io_netty_netty//jar",
    "@io_netty_netty_codec//jar",
    "@io_netty_netty_transport//jar",
    "@io_netty_netty_handler//jar",
    "@io_netty_netty_transport_native_epoll//jar",
    "@commons_math3//jar",
    "@joda_time//jar",
    "@mapdb//jar",
    "@httpclient_osgi//jar",
    "@httpcore_osgi//jar",
    "@bndlib//jar",
    "@jersey_client//jar",
    "@jersey_server//jar",
    "@jersey_security//jar",
    "@org_apache_felix_scr_bnd//jar",
    "@openflowj//jar",
    "@org_apache_servicemix_bundles_snmp4j//jar",
    "@snmp_core//jar",
    "@mibs_net_snmp//jar",
    "@mibs_rfc//jar",
    "@tinder_xmpp//jar",
    "@aalto_xml//jar",
    "@stax2_api//jar",
    "@bcprov_jdk15on//jar",
    "@bcpkix_jdk15on//jar",
    "@sshd_core//jar",
    "@ganymed_ssh2//jar",
    "@concurrent_trees//jar",
]

EXTERNAL_GROUPS = {
    "Network Model &amp; Services": ["org.onosproject.*"],
    "Incubator for Network Model &amp; Services": ["org.onosproject.incubator.*"],
    "Utilities": ["org.onlab.*"],
    "App &amp; Extensions": [
        "org.onosproject.dhcp",
        "org.onosproject.cpman",
        "org.onosproject.routing",
    ],
}

project_javadoc(
    name = "external",
    srcs = SOURCES,
    groups = EXTERNAL_GROUPS,
    title = "ONOS Java API (%s)" % ONOS_VERSION,
    deps = JAVADOC_DEPS,
)
