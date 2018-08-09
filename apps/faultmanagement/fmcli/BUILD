COMPILE_DEPS = CORE_DEPS + CLI + [
    "//incubator/api:onos-incubator-api",
    "//apps/faultmanagement/fmmgr:onos-apps-faultmanagement-fmmgr",
]

osgi_jar_with_tests(
    import_packages = "*,org.onosproject.cli.net",
    deps = COMPILE_DEPS,
)
