load("//tools/build/bazel:generate_workspace.bzl", "ONOS_VERSION")

COMPILE_DEPS = CORE_DEPS + JACKSON + [
    "@org_apache_karaf_shell_console//jar",
    "//incubator/api:onos-incubator-api",
    "//cli:onos-cli",
]

osgi_jar_with_tests(
    test_deps = TEST_ADAPTERS,
    deps = COMPILE_DEPS,
)

onos_app(
    app_name = "org.onosproject.optical-model",
    category = "Optical",
    description = "ONOS optical information model.",
    # Is there a way to change BUCK target name properly?
    #name = 'onos-optical-model',
    feature_coords = "org.onosproject:onos-optical-model:%s" % (ONOS_VERSION),
    required_apps = [],
    title = "Optical Network Model",
    url = "https://wiki.onosproject.org/x/C4m",
)
