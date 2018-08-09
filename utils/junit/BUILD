SRC = "src/main/java/org/onlab/**/"

TEST_SRC = "src/test/java/org/onlab/**/"

COMPILE_DEPS = [
    "@com_google_guava_guava//jar",
    "@slf4j_api//jar",
    "@hamcrest_all//jar",
    "@junit//jar",
]

TEST_DEPS = [
    "@com_google_guava_guava_testlib//jar",
]

osgi_jar_with_tests(
    name = "onlab-junit",
    srcs = glob([SRC + "*.java"]),
    test_deps = TEST_DEPS,
    test_srcs = glob([TEST_SRC + "*.java"]),
    visibility = ["//visibility:public"],
    deps = COMPILE_DEPS,
)
