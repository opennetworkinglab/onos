COMPILE_DEPS = CORE_DEPS + JACKSON + CLI + REST + [
    "//incubator/api:onos-incubator-api",
    "//apps/mappingmanagement/api:onos-apps-mappingmanagement-api",
    "//core/store/serializers:onos-core-serializers",
]

osgi_jar_with_tests(
    import_packages = "*,org.onosproject.cli.net",
    deps = COMPILE_DEPS,
)
