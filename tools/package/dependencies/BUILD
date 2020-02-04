load("//tools/build/bazel:pom_file.bzl", "dependencies_pom")
load(
    "//tools/build/bazel:variables.bzl",
    "DEFAULT_JAVA_VERSION",
    "ONOS_ARTIFACT_BASE",
    "ONOS_GROUP_ID",
    "ONOS_VERSION",
)
load(
    "//tools/build/bazel:generate_workspace.bzl",
    "CLI",
    "CORE_DEPS",
    "JACKSON",
    "JAXB",
    "KRYO",
    "REST",
    "TEST",
)

# Listed in the <dependencyManagement> section of the generated pom
DEPS = [
    d
    for d in CORE_DEPS + JAXB + JACKSON + KRYO +
             CLI + REST + TEST
    # Keep only third-party (external) ones.
    if d.startswith("@")
] + [
    "@org_apache_karaf_shell_console//jar",
    "@org_osgi_cmpn//jar",
    "@jersey_container_servlet//jar",
    "@kafka_clients//jar",
]

# Listed in the <dependencies> section with scope 'provided'
DEPS_PROVIDED = [
    "@com_google_guava_guava//jar",
    "@slf4j_api//jar",
    "@osgi_core//jar",
    "@org_osgi_cmpn//jar",
    "@org_osgi_util_promise//jar",
    "@org_osgi_service_component//jar",
    "@org_osgi_service_component_annotations//jar",
    "@org_osgi_service_metatype_annotations//jar",
]

# Listed in the <dependencies> section with scope 'test'.
DEPS_TEST = [d for d in TEST if d.startswith("@")]

VARS = [
    "ONOS_VERSION=" + ONOS_VERSION,
    "ONOS_GROUP_ID=" + ONOS_GROUP_ID,
    "ONOS_ARTIFACT_BASE=" + ONOS_ARTIFACT_BASE,
    "JAVA_VERSION=" + DEFAULT_JAVA_VERSION,
]

dependencies_pom(
    name = "onos-dependencies-pom",
    deps_provided = DEPS_PROVIDED,
    deps_test = DEPS_TEST,
    pom_template = "template.pom",
    vars = VARS,
    deps = DEPS,
)

# An empty jar, so we can re-use onos-publish logic.
java_library(
    name = "onos-dependencies",
    resources = ["empty.txt"],
    visibility = ["//visibility:public"],
)
