SRC = 'src/main/java/org/onosproject/**/'
TEST = 'src/test/java/org/onosproject/**/'

CURRENT_NAME = 'onos-app-cord-mcast'
CURRENT_TARGET = ':' + CURRENT_NAME

COMPILE_DEPS = [
    '//lib:CORE_DEPS',
    '//lib:javax.ws.rs-api',
    '//lib:jersey-client',
    '//lib:jersey-common',
    '//utils/rest:onlab-rest',
    '//apps/olt:onos-app-olt-api',
]

TEST_DEPS = [
    '//lib:TEST',
]

java_library(
    name = CURRENT_NAME,
    srcs = glob([SRC + '/*.java']),
    deps = COMPILE_DEPS,
    visibility = ['PUBLIC'],
)

java_test(
    name = 'tests',
    srcs = glob([TEST + '/*.java']),
    deps = COMPILE_DEPS +
           TEST_DEPS +
           [CURRENT_TARGET],
    source_under_test = [CURRENT_TARGET],
)
