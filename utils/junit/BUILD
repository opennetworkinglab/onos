SRC = 'src/main/java/org/onlab/**/'
TEST_SRC = 'src/test/java/org/onlab/**/'

COMPILE_DEPS = [
        "@guava//jar",
        "@slf4j_api//jar",
        "@hamcrest_all//jar",
        "@junit//jar",
]

TEST_DEPS = [
    "@guava_testlib//jar",
]

osgi_jar_with_tests(
    name = 'onlab-junit',
    srcs = glob([SRC + '*.java']),
    deps = COMPILE_DEPS,
    test_srcs = glob([TEST_SRC + '*.java']),
    test_deps = TEST_DEPS,
    package_name_root = 'org.onlab',
    visibility = ["//visibility:public"],
)


