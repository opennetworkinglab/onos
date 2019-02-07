BUNDLES = [
    "//providers/bgp/cfg:onos-providers-bgp-cfg",
    "//providers/bgp/topology:onos-providers-bgp-topology",
    "//providers/bgp/route:onos-providers-bgp-route",
    "//providers/bgp/cli:onos-providers-bgp-cli",
    "//protocols/bgp/api:onos-protocols-bgp-api",
    "//protocols/bgp/ctl:onos-protocols-bgp-ctl",
    "//protocols/bgp/bgpio:onos-protocols-bgp-bgpio",
]

onos_app(
    category = "Provider",
    description = "BGP protocol southbound providers.",
    included_bundles = BUNDLES,
    required_apps = [
        "org.onosproject.evpn-route-service",
        "org.onosproject.tunnel",
        "org.onosproject.pcep-api",
    ],
    title = "BGP Provider",
    url = "http://onosproject.org",
)
