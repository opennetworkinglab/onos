COMPILE_DEPS = COMPILE + JACKSON + [
    "//utils/osgi:onlab-osgi",
    "//utils/misc:onlab-misc",
    "@javax_ws_rs_api//jar",
    "@jersey_server//jar",
    "@jersey_hk2//jar",
]

osgi_jar(
    name = "onlab-rest",
    visibility = ["//visibility:public"],
    deps = COMPILE_DEPS,
)
