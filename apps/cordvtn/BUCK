COMPILE_DEPS = [
    '//lib:CORE_DEPS',
    '//lib:org.apache.karaf.shell.console',
    '//lib:javax.ws.rs-api',
    '//lib:jsch',
    '//lib:openstack4j-core',
    '//lib:openstack4j-http-connector',
    '//lib:openstack4j-httpclient',
    '//utils/rest:onlab-rest',
    '//cli:onos-cli',
    '//core/store/serializers:onos-core-serializers',
    '//apps/dhcp/api:onos-apps-dhcp-api',
    '//protocols/ovsdb/api:onos-ovsdb-api',
    '//protocols/ovsdb/rfc:onos-ovsdb-rfc',
]

osgi_jar_with_tests (
    deps = COMPILE_DEPS,
    web_context = '/onos/cordvtn',
)

#FIXME need onos_app