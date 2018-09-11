CFGDEF_EXECUTABLE = "cfgdef_generator"

COMPILE_DEPS = JACKSON + [
    "@com_google_guava_guava//jar",
    "@qdox//jar",
]

java_binary(
    name = CFGDEF_EXECUTABLE,
    srcs = glob(["src/main/java/org/onosproject/cfgdef/CfgDefGenerator.java"]),
    main_class = "org.onosproject.cfgdef.CfgDefGenerator",
    visibility = ["//visibility:public"],
    deps = COMPILE_DEPS,
)
