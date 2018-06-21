COMPILE_DEPS = CORE_DEPS + [
    "//protocols/bgp/api:onos-protocols-bgp-api",
    "//protocols/bgp/bgpio:onos-protocols-bgp-bgpio",
    "//incubator/store:onos-incubator-store",
    "//incubator/api:onos-incubator-api",
    "//apps/evpn-route-service/api:onos-apps-evpn-route-service-api",
]

osgi_jar_with_tests(
    test_deps = TEST_ADAPTERS,
    deps = COMPILE_DEPS,
)
