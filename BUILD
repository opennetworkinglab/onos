load("//tools/build/bazel:generate_workspace.bzl", "ONOS_VERSION")
load(":modules.bzl", "APPS", "CORE")

filegroup(
    name = "onos",
    srcs = CORE + APPS + [
        ":onos-package-admin",
        ":onos-package-test",
        ":onos-package",
    ],
    visibility = ["//visibility:public"],
)

KARAF = "@apache_karaf//file"

PATCHES = "@apache_karaf_patches//file"

BRANDING = "//tools/package/branding:onos-tools-package-branding"

# Generates auxiliary karaf.zip file; branded and augmented with ONOS runtime tools
genrule(
    name = "onos-karaf",
    srcs = [
        KARAF,
        PATCHES,
        BRANDING,
    ] + glob([
        "tools/package/bin/*",
        "tools/package/etc/*",
        "tools/package/init/*",
        "tools/package/runtime/bin/*",
    ]),
    outs = ["karaf.zip"],
    cmd = "$(location tools/package/onos-prep-karaf) $(location karaf.zip) $(location %s) %s $(location %s) $(location %s) tools/package" %
          (KARAF, ONOS_VERSION, BRANDING, PATCHES),
    tools = ["tools/package/onos-prep-karaf"],
)

# Generates the principal onos.tar.gz bundle
genrule(
    name = "onos-package",
    srcs = [
        "//tools/package/features:onos-features",
        ":onos-karaf",
    ] + APPS,
    outs = ["onos.tar.gz"],
    cmd = "$(location tools/package/onos_stage.py) $(location onos.tar.gz) %s $(location :onos-karaf) $(SRCS)" % ONOS_VERSION,
    output_to_bindir = True,
    tools = ["tools/package/onos_stage.py"],
)

# Generates the onos-admin.tar.gz file with remote admin tools
genrule(
    name = "onos-package-admin",
    srcs = glob([
        "tools/package/runtime/bin/*",
        "tools/dev/bin/onos-create-app",
        "tools/test/bin/onos",
    ]),
    outs = ["onos-admin.tar.gz"],
    cmd = "mkdir onos-admin-%s; cp $(SRCS) onos-admin-%s; tar zcf $(location onos-admin.tar.gz) onos-admin-%s" %
          (ONOS_VERSION, ONOS_VERSION, ONOS_VERSION),
    output_to_bindir = True,
)

# Generates the onos-test.tar.gz file with test tools
genrule(
    name = "onos-package-test",
    srcs = glob([
        "tools/build/envDefaults",
        "tools/dev/bash_profile",
        "tools/dev/bin/onos-create-app",
        "tools/test/**/*",
        "tools/package/runtime/bin/*",
    ]),
    outs = ["onos-test.tar.gz"],
    cmd = "mkdir onos-test-%s; cp -r tools onos-test-%s; tar zcf $(location onos-test.tar.gz) onos-test-%s" %
          (ONOS_VERSION, ONOS_VERSION, ONOS_VERSION),
    output_to_bindir = True,
)

# Runs ONOS as a single instance from the /tmp directory
# FIXME: Still work in progress
genrule(
    name = "onos-run",
    outs = ["onos-runner"],
    srcs = [":onos-package", "tools/package/onos-run-karaf"],
    cmd = "sed \"s#ONOS_TAR=#ONOS_TAR=$(location :onos-package)#\" $(location tools/package/onos-run-karaf) > $(location onos-runner); chmod +x $(location onos-runner)",
    executable = True,
    output_to_bindir = True,
    visibility = ["//visibility:public"],
)
