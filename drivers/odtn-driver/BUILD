COMPILE_DEPS = CORE_DEPS + JACKSON + [
    "@commons_jxpath//jar",
    "@javax_ws_rs_api//jar",
    "@httpcore_osgi//jar",
    "//core/store/serializers:onos-core-serializers",
    "//drivers/utilities:onos-drivers-utilities",
    "//protocols/netconf/api:onos-protocols-netconf-api",
    "//protocols/rest/api:onos-protocols-rest-api",
    "//apps/odtn/api:onos-apps-odtn-api",
    "//apps/optical-model:onos-apps-optical-model",
    "//drivers/optical:onos-drivers-optical",
    "//apps/faultmanagement/fmcli:onos-apps-faultmanagement-fmcli",  # Enabling Alarm stuff
    "//apps/faultmanagement/fmmgr:onos-apps-faultmanagement-fmmgr-native",
    "//drivers/gnmi:onos-drivers-gnmi",
    "//protocols/gnmi/stub:onos-protocols-gnmi-stub",
    "//protocols/gnmi/api:onos-protocols-gnmi-api",
    "//protocols/grpc/utils:onos-protocols-grpc-utils",
]

TEST_DEPS = TEST_ADAPTERS + [
    "@slf4j_jdk14//jar",
    "//core/api:onos-api-tests",
]

BUNDLES = [
    ":onos-drivers-odtn-driver",
    "//drivers/utilities:onos-drivers-utilities",
]

osgi_jar_with_tests(
    resources = glob(["src/main/resources/**"]),
    resources_root = "src/main/resources",
    test_deps = TEST_DEPS,
    deps = COMPILE_DEPS,
)

onos_app(
    app_name = "org.onosproject.drivers.odtn-driver",
    category = "Drivers",
    description = "Drivers related to ODTN",
    included_bundles = BUNDLES,
    required_apps = [
        "org.onosproject.restsb",
        "org.onosproject.netconf",
        "org.onosproject.config",
        "org.onosproject.odtn-api",
        "org.onosproject.drivers.netconf",
        "org.onosproject.drivers.optical",
        "org.onosproject.optical-model",
        "org.onosproject.drivers.gnmi",
    ],
    title = "ODTN Driver",
    url = "https://wiki.onosproject.org/display/ODTN/ODTN",
)
