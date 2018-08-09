COMPILE_DEPS = COMPILE + NETTY + JACKSON + METRICS + KRYO + [
    "//utils/misc:onlab-misc",
    "//utils/osgi:onlab-osgi",
    "//utils/rest:onlab-rest",
    "@joda_time//jar",
    "@io_netty_netty//jar",
]

osgi_jar_with_tests(
    name = "onos-api",
    exclude_tests = [
        "org.onosproject.net.config.basics.AbstractConfigTest",
        "org.onosproject.net.driver.AbstractDriverLoaderTest",
        "org.onosproject.net.intent.AbstractIntentTest",
        "org.onosproject.net.intent.ConnectivityIntentTest",
        "org.onosproject.net.intent.IntentTest",
        "org.onosproject.net.pi.runtime.PiConstantsTest",
        "org.onosproject.security.SecurityUtilTest",
        "org.onosproject.ui.AbstractUiTest",
        "org.onosproject.ui.model.AbstractUiModelTest",
        "org.onosproject.net.behaviour.QosIdTest",
    ],
    visibility = ["//visibility:public"],
    deps = COMPILE_DEPS,
    #javadoc_files = glob(["src/main/javadoc/**/*"]),
    #javadoc_files_root = "src/main/javadoc",
)
