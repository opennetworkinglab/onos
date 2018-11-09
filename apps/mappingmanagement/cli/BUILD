COMPILE_DEPS = CORE_DEPS + JACKSON + CLI + REST + [
    "//apps/mappingmanagement/api:onos-apps-mappingmanagement-api",
    "//core/store/serializers:onos-core-serializers",
]

osgi_jar_with_tests(
    karaf_command_packages = ["org.onosproject.mapping.cli"],
    deps = COMPILE_DEPS,
)
