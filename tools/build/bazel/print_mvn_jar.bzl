load("//tools/build/bazel:generate_workspace.bzl", "maven_coordinates")

# Example invocation:
# bazel build $(bazel query 'kind("_bnd rule", //...)') \
#       --aspects tools/build/bazel/mvn_jar.bzl%print_mvn_jar 2>&1 | \
#     egrep "DEBUG: .*mvn_jar.bzl" | cut -d\  -f3-

def _impl(target, ctx):
    [ print (maven_coordinates(target.label), f.path) for f in target.files ]
    return []

print_mvn_jar = aspect(
    implementation = _impl,
)
