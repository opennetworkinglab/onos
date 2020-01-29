load("//tools/build/bazel:generate_workspace.bzl", "maven_coordinates")

# Example invocation:
# bazel build $(bazel query 'kind("_bnd rule", //...)') \
#       --aspects tools/build/bazel/publish_catalog.bzl%publish_catalog 2>&1 | \
#     egrep "DEBUG: .*mvn_jar.bzl" | cut -d\  -f3-

def _remote(group_id, artifact_id, version, packaging, classifier):
    p = group_id.replace(".", "/") + "/" + artifact_id + "/" + version + "/" + artifact_id + "-" + version
    if classifier != None:
        p += "-" + classifier
    p += "." + packaging
    return p

def _impl(target, ctx):
    coords = maven_coordinates(target.label)
    mvn = coords.split(":")
    group_id = mvn[1]
    artifact_id = mvn[2]
    version = mvn[len(mvn) - 1]
    packaging = "oar" if target.label.name.endswith("-oar") else "jar"
    classifier = None

    if len(mvn) > 4:
        packaging = mvn[3]

    c = artifact_id.split("-")

    if len(c) > 1 and c[len(c) - 1] in ("javadoc", "sources", "tests", "pom"):
        classifier = c[len(c) - 1]
        artifact_id = "-".join(c[:len(c) - 1])
        if classifier == "pom":
            packaging = classifier
            classifier = None

    for f in target.files.to_list():
        print("%s\t%s" % (f.path, _remote(group_id, artifact_id, version, packaging, classifier)))
    return []

publish_catalog = aspect(
    implementation = _impl,
)
