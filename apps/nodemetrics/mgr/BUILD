COMPILE_DEPS = CORE_DEPS + CLI + [
    "@sigar//jar",
    "//apps/nodemetrics/api:onos-apps-nodemetrics-api",
    "//core/store/serializers:onos-core-serializers",
]

osgi_jar_with_tests(
    karaf_command_packages = ["org.onosproject.nodemetrics.cli"],
    deps = COMPILE_DEPS,
)
