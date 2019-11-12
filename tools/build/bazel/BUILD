py_binary(
    name = "osgi_feature_bundler",
    srcs = ["osgi_feature_bundler.py"],
    main = "osgi_feature_bundler.py",
    visibility = ["//visibility:public"],
)

py_binary(
    name = "onos_app_tools",
    srcs = ["onos_app_tools.py"],
    main = "onos_app_tools.py",
    visibility = ["//visibility:public"],
)

py_binary(
    name = "onos_app_bundler",
    srcs = ["onos_app_bundler.py"],
    main = "onos_app_bundler.py",
    visibility = ["//visibility:public"],
)

py_binary(
    name = "pom_generator",
    srcs = ["pom_generator.py"],
    main = "pom_generator.py",
    visibility = ["//visibility:public"],
)

py_binary(
    name = "dependencies_pom_generator",
    srcs = ["dependencies_pom_generator.py"],
    main = "dependencies_pom_generator.py",
    visibility = ["//visibility:public"],
)

java_binary(
    name = "onos_yang_compiler",
    main_class = "org.onosproject.yang.compiler.main.YangCompilerMain",
    visibility = ["//visibility:public"],
    runtime_deps = [
        "@onos_yang_compiler_main//jar",
        "@slf4j_nop//jar",  # TODO: Change to jdk14 after fixing verbosity
    ],
)
