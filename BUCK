java_library(
    name = 'core',
    visibility = ['PUBLIC'],
    deps = CORE,
)

java_library(
    name = 'apps',
    visibility = ['PUBLIC'],
    deps = APPS + APP_JARS,
)

java_library(
    name = 'onos',
    visibility = ['PUBLIC'],
    deps = [ ':core', ':apps' ]
)

INSTALL = [
    '//utils/misc:onlab-misc-install',
    '//utils/osgi:onlab-osgi-install',
    '//utils/rest:onlab-rest-install',

    '//core/api:onos-api-install',
    '//incubator/api:onos-incubator-api-install',

    '//core/net:onos-core-net-install',
    '//core/common:onos-core-common-install',
    '//core/store/dist:onos-core-dist-install',
    '//core/store/primitives:onos-core-primitives-install',
    '//core/store/persistence:onos-core-persistence-install',
    '//core/store/serializers:onos-core-serializers-install',

    '//incubator/net:onos-incubator-net-install',
    '//incubator/core:onos-incubator-core-install',
    '//incubator/store:onos-incubator-store-install',
    '//incubator/rpc:onos-incubator-rpc-install',

    '//core/security:onos-security-install',

    '//web/api:onos-rest-install',
    '//web/gui:onos-gui-install',
    '//cli:onos-cli-install',
]
java_library(
    name = 'install',
    visibility = ['PUBLIC'],
    deps = INSTALL
)

tar_file(
    name = 'onos-test',
    root = 'onos-test-%s' % ONOS_VERSION,
    srcs = glob(['tools/test/**/*']) + [
               'tools/dev/bash_profile',
               'tools/dev/bin/onos-app',
               'tools/build/envDefaults'
           ],
)