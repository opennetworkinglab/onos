COMPILE_DEPS = CORE_DEPS + JACKSON + [
    "@javax_ws_rs_api//jar",
    "@joda_time//jar",
    "//utils/rest:onlab-rest",
    "//protocols/rest/api:onos-protocols-rest-api",
]

BUNDLES = [
    ":onos-drivers-server",
]

REQUIRED_APPS = [
    "org.onosproject.restsb",
]

osgi_jar_with_tests(
    resources = glob(["src/main/resources/**"]),
    resources_root = "src/main/resources",
    test_deps = TEST_ADAPTERS,
    deps = COMPILE_DEPS,
)

onos_app(
    app_name = "org.onosproject.drivers.server",
    category = "Drivers",
    description = "ONOS southbound drivers for commodity servers",
    included_bundles = BUNDLES,
    origin = "KTH Royal Institute of Technology &amp; RISE SICS",
    required_apps = REQUIRED_APPS,
    title = "Server Device Drivers",
    url = "https://github.com/gkatsikas/onos/tree/metron-driver",
)
