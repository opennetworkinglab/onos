COMPILE_DEPS = CORE_DEPS + KRYO + JACKSON + REST + CLI + METRICS + [
    "@jersey_server//jar",
    "//core/store/serializers:onos-core-serializers",
]

osgi_jar(
    api_description = "REST API for Packet Stats",
    api_package = "org.onosproject.packetstats.rest",
    api_title = "Packet Stats App",
    api_version = "1.0",
    web_context = "/onos/v1/packet-stats",
    deps = COMPILE_DEPS,
)
