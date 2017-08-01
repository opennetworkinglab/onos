COMPILE_DEPS = COMPILE + JACKSON + [
  "//utils/osgi:onlab-osgi",
  "//utils/misc:onlab-misc",
  "@javax_ws_rs_api//jar",
  "@jersey_server//jar",
]

osgi_jar(
    name = 'onlab-rest',
    deps = COMPILE_DEPS,
    package_name_root = 'org.onlab',
    visibility = ["//visibility:public"],
)
