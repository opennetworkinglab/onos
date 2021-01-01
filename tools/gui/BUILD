load("//tools/build/bazel:jdk_genrule.bzl", genrule = "jdk_genrule")

filegroup(
    name = "_tools-gui-gulp-files",
    srcs = [
        "package.json",
        "gulpfile.babel.js",
    ] + glob(["gulp-tasks/**"]),
)

genrule(
    name = "tools-gui-gulp",
    srcs = [":_tools-gui-gulp-files"],
    outs = ["tools-gui-gulp.jar"],
    cmd = "cd tools/gui && find . -type f -exec touch -t 201806280000 {} \\; && jar Mcf ../../$@ .",
    visibility = ["//visibility:public"],
)
