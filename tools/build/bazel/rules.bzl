load("//tools/build/bazel:generate_workspace.bzl",
        "generated_java_libraries", "COMPILE", "CORE_DEPS", "JACKSON",
        "TEST_ADAPTERS", "TEST", "TEST_REST", "METRICS", "KRYO", "NETTY")

def onos_bazel_rules() :
    return

def g() :
    generated_java_libraries()