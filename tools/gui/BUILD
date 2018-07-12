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
    outs = ["tools-gui-gulp.tar"],
    cmd = "cd tools/gui && tar cf ../../$@ .",
    visibility = ["//visibility:public"],
)
