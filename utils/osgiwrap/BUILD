OSGIWRAP_EXECUTABLE = "osgi-jar"

COMPILE_DEPS = [
    "@guava//jar",
    "@bndlib//jar",
    "@org_apache_felix_scr_bnd//jar",
]

java_binary(
    name = OSGIWRAP_EXECUTABLE,
    srcs = glob(["src/main/java/**/*.java"]),
    main_class = "org.onlab.osgiwrap.OSGiWrapper",
    visibility = ["//visibility:public"],
    deps = COMPILE_DEPS,
)
