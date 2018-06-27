COMPILE_DEPS = CORE_DEPS + JACKSON + KRYO + [
    "//protocols/lisp/api:onos-protocols-lisp-api",
    "//protocols/lisp/msg:onos-protocols-lisp-msg",
    "//apps/mappingmanagement/api:onos-apps-mappingmanagement-api",
    "//core/store/serializers:onos-core-serializers",
]

TEST_DEPS = TEST_ADAPTERS + [
    "@jersey_test_framework_core//jar",
    "@jersey_test_framework_jetty//jar",
    "//apps/mappingmanagement/api:onos-apps-mappingmanagement-api-tests",
    "//core/api:onos-api-tests",
    "//utils/osgi:onlab-osgi-tests",
    "//web/api:onos-rest-tests",
]

BUNDLES = [
    ":onos-drivers-lisp",
]

osgi_jar_with_tests(
    resources = glob(["src/main/resources/**"]),
    resources_root = "src/main/resources",
    test_deps = TEST_DEPS,
    deps = COMPILE_DEPS,
)

onos_app(
    app_name = "org.onosproject.drivers.lisp",
    category = "Drivers",
    description = "Adds support for devices using LISP.",
    included_bundles = BUNDLES,
    required_apps = ["org.onosproject.lisp"],
    title = "Generic LISP Drivers",
    url = "https://wiki.onosproject.org/display/ONOS/LISP+as+SBI",
)
