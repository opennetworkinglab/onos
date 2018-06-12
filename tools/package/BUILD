load("//tools/build/bazel:generate_workspace.bzl", "ONOS_VERSION")

KARAF = "@apache_karaf//file"
PATCHES = "@apache_karaf_patches//file"
BRANDING = "//tools/package/branding:onos-tools-package-branding"

BRANDED_KARAF_REQUIREMENTS = [
    KARAF,
    PATCHES,
    BRANDING,
]

# Generates auxiliary karaf.zip file; branded and augmented with ONOS runtime tools
genrule(
    name = "onos-karaf",
    srcs = BRANDED_KARAF_REQUIREMENTS + glob(["bin/*", "etc/*", "init/*", "runtime/bin/*"]),
    outs = ["karaf.zip"],
    cmd = "$(location onos-prep-karaf) $(location karaf.zip) $(location %s) %s $(location %s) $(location %s) tools/package" \
              % (KARAF, ONOS_VERSION, BRANDING, PATCHES),
    tools = ["onos-prep-karaf"],
)

# FIXME: Move /features to under tools/package/features
PACKAGING_REQUIREMENTS = [
    "//features:onos-features",
    ":onos-karaf",
]

# FIXME: Need to add OAR files as dependencies
# Generates the principal distributable onos.tar.gz file
genrule(
    name = "onos-package",
    srcs = PACKAGING_REQUIREMENTS + glob(["bin/*", "etc/*", "init/*", "config/*", "runtime/bin/*"]),
    outs = ["onos.tar.gz"],
    cmd = "$(location onos_stage.py) $(location onos.tar.gz) %s $(location :onos-karaf) $(SRCS)" % ONOS_VERSION,
    visibility = ["//visibility:public"],
    tools = ["onos_stage.py"],
)

filegroup(
    name = "onos-runtime-tools",
    srcs = glob(["runtime/bin/*"]),
)

# Generates the onos-admin.tar.gz file with remote admin tools
genrule(
    name = "onos-package-admin",
    srcs = ["//tools/dev:onos-admin", "//tools/test:onos-admin", ":onos-runtime-tools"],
    outs = ["onos-admin.tar.gz"],
    cmd = "mkdir onos-admin-%s; cp $(SRCS) onos-admin-%s; tar zcf $(location onos-admin.tar.gz) onos-admin-%s"\
          % (ONOS_VERSION, ONOS_VERSION, ONOS_VERSION),
    visibility = ["//visibility:public"],
)

# Generates the onos-test.tar.gz file with test tools
genrule(
    name = "onos-package-test",
    srcs = ["//tools/dev:onos-test", "//tools/test:onos-test", ":onos-runtime-tools", "//:onos-env-defaults"],
    outs = ["onos-test.tar.gz"],
    cmd = "mkdir onos-test-%s; cp -r tools onos-test-%s; tar zcf $(location onos-test.tar.gz) onos-test-%s"\
          % (ONOS_VERSION, ONOS_VERSION, ONOS_VERSION),
    visibility = ["//visibility:public"],
)

# Runs ONOS as a single instance from the /tmp directory
# FIXME: Still work in progress
genrule(
    name = "onos-run",
    outs = ["onos-runner"],
    srcs = ["onos-run-karaf", "onos-package"],
    cmd = "sed \"s#ONOS_TAR=#ONOS_TAR=$(location :onos-package)#\" $(location onos-run-karaf) > $(location onos-runner); chmod +x $(location onos-runner)",
    executable = True,
    local = True,
    visibility = ["//visibility:public"],
)

#staged_repos = ['$(location %s-repo)' % f for f in FEATURES]
#staged_apps = ['$(location %s)' % a for a in APPS]
#
## feature_coords = 'foo:bar:1.3'
#sources = [ '$(location :onos-features)', ]
#sources += staged_repos + staged_apps


