SWAGGER_EXECUTABLE = "swagger_generator"

COMPILE_DEPS = JACKSON + [
    "@com_google_guava_guava//jar",
    "@qdox//jar",
    "@org_apache_felix_scr_bnd//jar",
]

java_binary(
    name = SWAGGER_EXECUTABLE,
    srcs = glob(["src/main/java/org/onosproject/swagger/SwaggerGenerator.java"]),
    main_class = "org.onosproject.swagger.SwaggerGenerator",
    resources = glob(["src/main/resources/registrator.javat"]),
    visibility = ["//visibility:public"],
    deps = COMPILE_DEPS,
)
