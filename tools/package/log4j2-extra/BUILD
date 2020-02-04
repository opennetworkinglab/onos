load("//tools/build/bazel:osgi_java_library.bzl", "wrapped_osgi_jar")

# This OSGi bundle is needed to import extra packages on log4j2 library

# To include other packages, specify them as comma separated list.
# As reference for packages that can be imported see:
# https://github.com/ops4j/org.ops4j.pax.logging/blob/master/pax-logging-log4j2-extra/osgi.bnd
IMPORT_PACKAGES = "org.apache.kafka.clients.producer,org.apache.kafka.common.serialization"

# An empty OSGi jar bundle including a Manifest that will be merged with
# the specified fragment_host bundle.
wrapped_osgi_jar(
    name = "onos-log4j2-extra",
    fragment_host = "org.ops4j.pax.logging.pax-logging-log4j2",
    generate_pom = False,
    import_packages = IMPORT_PACKAGES,
    jar = ":empty-log4j2-extra-jar",
    visibility = ["//visibility:public"],
    deps = [],
)

# An empty jar so we can wrap it inside an OSGi bundle.
# The parameter resource_strip_prefix is needed to not export packages in the OSGi bundle.
java_library(
    name = "empty-log4j2-extra-jar",
    resource_strip_prefix = "tools/package/log4j2-extra",
    resources = ["empty.txt"],
)
