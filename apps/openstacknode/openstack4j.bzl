INCLUDE_PACKAGES = "com.google.common.net,com.google.common.io,com.fasterxml.jackson.annotation"
EXCLUDE_PACKAGES = "!org.openstack4j,!org.openstack4j.*,!org.openstack4j.api,!org.openstack4j.api.*,!org.openstack4j.core.*,!org.openstack4j.model.network,!org.openstack4j.model.common,!org.openstack4j.openstack"
ALL_PACKAGES = "*"

def get_openstack4j_deps_path():
    WEB_INF_PATH = "WEB-INF/classes/deps/"
    OPENSTACK4J_DEPS = [
        "openstack4j-core",
        "openstack4j-http-connector",
        "openstack4j-httpclient",
    ]
    OPENSTACK4J_VER = "3.2.0"

    openstack_deps_path = ""

    for dep in OPENSTACK4J_DEPS:
        name = dep + "-" + OPENSTACK4J_VER + ".jar"
        path = WEB_INF_PATH + name
        openstack_deps_path = openstack_deps_path + path + ","

    return openstack_deps_path

def get_jackson_deps_path():
    WEB_INF_PATH = "WEB-INF/classes/deps/"
    JACKSON_DEPS_WITH_VER = [
        "json-patch-1.9.jar",
        "jackson-coreutils-1.6.jar",
        "msg-simple-1.1.jar",
        "btf-1.2.jar",
        "snakeyaml-1.18.jar",
    ]

    jackson_deps_path = ""

    for dep in JACKSON_DEPS_WITH_VER:
        path = WEB_INF_PATH + dep
        jackson_deps_path = jackson_deps_path + path + ","

    return jackson_deps_path
