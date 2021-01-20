def _impl(ctx):
    output = ctx.outputs.deps_files

    dep_list = []
    for dep in ctx.files.deps:
        dep_list += [dep.path]

    cmd = [
        "echo %s >>  %s" % (",".join(dep_list), output.path),
    ]

    ctx.actions.run_shell(
        inputs = ctx.files.deps,
        outputs = [output],
        progress_message = "Generating deps file paths for %s" % ctx.attr.name,
        command = ";\n".join(cmd),
    )

deps_files = rule(
    attrs = {
        "deps": attr.label_list(allow_files = True),
    },
    implementation = _impl,
    outputs = {"deps_files": "%{name}-deps-files.txt"},
)
