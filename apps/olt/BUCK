SRC = 'src/main/java/org/onosproject/**/'
TEST = 'src/test/java/org/onosproject/**/'

COMPILE_DEPS = [
    '//lib:CORE_DEPS',
    '//lib:javax.ws.rs-api',
    '//lib:jersey-client',
    '//lib:org.apache.karaf.shell.console',
    '//utils/rest:onlab-rest',
    '//cli:onos-cli',
    '//core/store/serializers:onos-core-serializers',
]

TEST_DEPS = [
    '//lib:TEST',
]

java_library(
    name = 'onos-app-olt-api',
    srcs = glob(['api/' + SRC + '*.java']),
    deps = COMPILE_DEPS,
    visibility = ['PUBLIC'],
)

java_test(
    name = 'onos-app-olt-api-tests',
    srcs = glob([TEST + 'api/*.java']),
    deps = COMPILE_DEPS +
           TEST_DEPS +
           [':onos-app-olt-api'],
    source_under_test = [':onos-app-olt-api'],
)

java_library(
    name = 'onos-app-olt-app',
    srcs = glob(['app/' + SRC + '*.java']),
    deps = COMPILE_DEPS + [':onos-app-olt-api'],
    visibility = ['PUBLIC'],
)

java_test(
    name = 'onos-app-olt-app-tests',
    srcs = glob([TEST + 'app/*.java']),
    deps = COMPILE_DEPS +
           TEST_DEPS +
           [':onos-app-olt-api', ':onos-app-olt-app'],
    source_under_test = [':onos-app-olt-app'],
)
