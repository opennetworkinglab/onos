COMPILE_DEPS = CORE_DEPS + JACKSON + KRYO + CLI + [
    "//apps/tunnel/api:onos-apps-tunnel-api",
    "//core/store/serializers:onos-core-serializers",
    "//apps/gluon:onos-apps-gluon",
    "//apps/vtn/vtnrsc:onos-apps-vtn-vtnrsc",
    "//apps/route-service/api:onos-apps-route-service-api",
    "//apps/evpn-route-service/api:onos-apps-evpn-route-service-api",
]

osgi_jar_with_tests(
    karaf_command_packages = ["org.onosproject.evpnopenflow.rsc.cli"],
    deps = COMPILE_DEPS,
)

onos_app(
    category = "Traffic Engineering",
    description = "Ethernet VPN (EVPN) introduces a new model for Ethernet services delivery." +
                  "It enables integrated Layer 2 service over Ethernet with multihoming.",
    required_apps = [
        "org.onosproject.route-service",
        "org.onosproject.evpn-route-service",
        "org.onosproject.gluon",
        "org.onosproject.tunnel",
        "org.onosproject.vtn",
    ],
    title = "EVPN OpenFlow",
    url = "http://onosproject.org",
)
