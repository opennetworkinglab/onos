COMPILE_DEPS = CORE_DEPS + KRYO + ATOMIX + [
    "//core/common:onos-core-common",
    "//core/store/serializers:onos-core-serializers",
]

TEST_DEPS = TEST + [
    "@commons_math3//jar",
    "@io_netty_netty_handler//jar",
    "@io_netty_netty_buffer//jar",
    "@io_netty_netty_codec//jar",
    "@io_netty_netty_resolver//jar",
    "//core/api:onos-api-tests",
]

_primitive_package_names = [
    "barrier",
    "collection",
    "counter",
    "election",
    "idgenerator",
    "iterator",
    "list",
    "lock",
    "log",
    "map",
    "multimap",
    "multiset",
    "queue",
    "semaphore",
    "set",
    "transaction",
    "tree",
    "value",
    "workqueue",
]

# A list of all core primitive packages for dynamic class loading
PRIMITIVE_PACKAGES = ",".join(["io.atomix.primitive.session.impl"] + ["io.atomix.core.{primitive},io.atomix.core.{primitive}.impl".format(primitive = primitive) for primitive in _primitive_package_names])

# Base package names for core Atomix protocols
_protocol_package_names = [
    "raft",
    "backup",
]

# A list of all cluster protocol packages for dynamic class loading
CLUSTER_PACKAGES = ",".join([
    "io.atomix.cluster.discovery",
    "io.atomix.cluster.protocol",
])

# A list of all core protocol packages for dynamic class loading
PROTOCOL_PACKAGES = ",".join(["io.atomix.primitive.partition.impl"] + ["io.atomix.protocols.{protocol},io.atomix.protocols.{protocol}.partition".format(protocol = protocol) for protocol in _protocol_package_names])

# A list of all additional core packages used in dynamic class loading
CORE_PACKAGES = ",".join([
    "io.atomix.core.profile",
    "io.atomix.core.profile.impl",
])

# A list of all dynamically loadable Atomix packages
INCLUDE_PACKAGES = ",".join([
    CLUSTER_PACKAGES,
    PRIMITIVE_PACKAGES,
    PROTOCOL_PACKAGES,
    CORE_PACKAGES,
])

ALL_PACKAGES = "*"

osgi_jar_with_tests(
    name = "onos-core-primitives",
    import_packages = INCLUDE_PACKAGES + "," + ALL_PACKAGES,
    test_deps = TEST_DEPS,
    visibility = ["//visibility:public"],
    deps = COMPILE_DEPS,
)
