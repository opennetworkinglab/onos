load("//tools/build/bazel:variables.bzl", "ONOS_VERSION")
load(
    "//tools/build/bazel:modules.bzl",
    "CORE",
    "FEATURES",
    "apps",
    "extensions",
    "profiles",
)

#
# ONOS Package Profiles
# Usage: bazel build onos-package --define profile=<profile name>
# Example: bazel build onos-package --define profile=minimal
#
# To view or update which apps and features are included in each
# profile, open: tools/build/bazel/modules.bzl
#
profiles([
    "minimal",
    "seba",
    "stratum",
    "sdfabric",
    "sona",
])

filegroup(
    name = "onos",
    srcs = CORE + [
        "//tools/build/conf:onos-build-conf",
        ":onos-package-admin",
        ":onos-package-test",
        ":onos-package",
    ] + select({
        ":minimal_profile": extensions("minimal") + apps("minimal"),
        ":seba_profile": extensions("seba") + apps("seba"),
        ":stratum_profile": extensions("stratum") + apps("stratum"),
        ":sdfabric_profile": extensions("sdfabric") + apps("sdfabric"),
        ":sona_profile": extensions("sona") + apps("sona"),
        "//conditions:default": extensions() + apps(),
    }),
    visibility = ["//visibility:public"],
)

KARAF = "@apache_karaf//:apache_karaf"

BRANDING = "//tools/package/branding:onos-tools-package-branding"

LOG4J_EXTRA = "//tools/package/log4j2-extra:onos-log4j2-extra"

# Generates auxiliary karaf.zip file; branded and augmented with ONOS runtime tools
genrule(
    name = "onos-karaf",
    srcs = [
        KARAF,
        BRANDING,
        LOG4J_EXTRA,
    ] + glob([
        "tools/package/bin/*",
        "tools/package/etc/*",
        "tools/package/init/*",
        "tools/package/runtime/bin/*",
    ]),
    outs = ["karaf.zip"],
    cmd = "$(location tools/package/onos-prep-karaf) $(location karaf.zip) $(location %s) %s $(location %s) '' $(location %s) tools/package" %
          (KARAF, ONOS_VERSION, BRANDING, LOG4J_EXTRA),
    tools = ["tools/package/onos-prep-karaf"],
)

# Generates the principal onos.tar.gz bundle
genrule(
    name = "onos-package",
    srcs = [
        "//tools/package/features:onos-features",
        ":onos-karaf",
    ] + FEATURES + select({
        ":minimal_profile": apps("minimal"),
        ":seba_profile": apps("seba"),
        ":stratum_profile": apps("stratum"),
        ":sdfabric_profile": apps("sdfabric"),
        ":sona_profile": apps("sona"),
        "//conditions:default": apps(),
    }),
    outs = ["onos.tar.gz"],
    cmd = "$(location tools/package/onos_stage.py) $(location onos.tar.gz) %s $(location :onos-karaf) $(SRCS)" % ONOS_VERSION,
    output_to_bindir = True,
    tags = ["local"],
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
    cmd = "mkdir onos-admin-%s; cp $(SRCS) onos-admin-%s; tar hzcf $(location onos-admin.tar.gz) onos-admin-%s" %
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
    cmd = "mkdir onos-test-%s; cp -r tools onos-test-%s; tar hzcf $(location onos-test.tar.gz) onos-test-%s" %
          (ONOS_VERSION, ONOS_VERSION, ONOS_VERSION),
    output_to_bindir = True,
)

# Runs ONOS as a single instance from the /tmp directory
alias(
    name = "onos-local",
    actual = select({
        ":run_with_absolute_javabase": ":onos-local_absolute-javabase",
        "//conditions:default": ":onos-local_current-jdk",
    }),
)

config_setting(
    name = "run_with_absolute_javabase",
    define_values = {
        "RUN_WITH_ABSOLUTE_JAVABASE": "true",
    },
)

# Run onos-local with JAVA_HOME set to ABSOLUTE_JAVABASE (see .bazelrc)
genrule(
    name = "onos-local_absolute-javabase",
    srcs = [":onos-local-base"],
    outs = ["onos-runner_absolute-javabase"],
    cmd = "sed \"s#ABSOLUTE_JAVABASE=#ABSOLUTE_JAVABASE=$(ABSOLUTE_JAVABASE)#\" " +
          "$(location onos-local-base) > $(location onos-runner_absolute-javabase)",
    executable = True,
    output_to_bindir = True,
    visibility = ["//visibility:private"],
)

# Run onos-local with the same JDK used for the build, packaged in a tarball.
genrule(
    name = "onos-local_current-jdk",
    srcs = [
        ":onos-local-base",
        "//tools/build/jdk:current_jdk_tar",
    ],
    outs = ["onos-runner_current-jdk"],
    cmd = "sed \"s#JDK_TAR=#JDK_TAR=$(location //tools/build/jdk:current_jdk_tar)#\" " +
          "$(location :onos-local-base) > $(location onos-runner_current-jdk); ",
    executable = True,
    output_to_bindir = True,
    visibility = ["//visibility:private"],
)

# Create an onos-runner script based on onos-run-karaf
genrule(
    name = "onos-local-base",
    srcs = [
        ":onos-package",
        "tools/package/onos-run-karaf",
    ] + glob(["tools/package/config/**"]),
    outs = ["onos-runner"],
    cmd = "sed \"s#ONOS_TAR=#ONOS_TAR=$(location :onos-package)#\" " +
          "$(location tools/package/onos-run-karaf) > $(location onos-runner); " +
          "chmod +x $(location onos-runner)",
    visibility = ["//visibility:private"],
)

load("@com_github_bazelbuild_buildtools//buildifier:def.bzl", "buildifier")

buildifier(
    name = "buildifier_check",
    exclude_patterns = [
        "./tools/build/bazel/generate_workspace.bzl",
        "./web/gui2/node_modules/@angular/bazel/src/esm5.bzl",
        "./web/gui2/node_modules/@bazel/typescript/internal/common/tsconfig.bzl",
        "./web/gui2/node_modules/@bazel/typescript/internal/common/compilation.bzl",
        "./web/gui2/node_modules/@bazel/rollup/rollup_bundle.bzl",
        "./web/gui2/node_modules/@bazel/typescript/internal/ts_project.bzl",
        "./web/gui2/node_modules/@bazel/typescript/internal/build_defs.bzl",
        "./web/gui2/node_modules/@bazel/protractor/protractor_web_test.bzl",
        "./web/gui2/node_modules/@bazel/typescript/third_party/github.com/bazelbuild/bazel/src/main/protobuf/BUILD.bazel",
    ],
    mode = "check",
)

buildifier(
    name = "buildifier_fix",
    exclude_patterns = [
        "./tools/build/bazel/generate_workspace.bzl",
        "./web/gui2/node_modules/@angular/bazel/src/esm5.bzl",
        "./web/gui2/node_modules/@bazel/typescript/internal/common/tsconfig.bzl",
        "./web/gui2/node_modules/@bazel/typescript/internal/common/compilation.bzl",
        "./web/gui2/node_modules/@bazel/typescript/internal/ts_project.bzl",
        "./web/gui2/node_modules/@bazel/typescript/internal/build_defs.bzl",
        "./web/gui2/node_modules/@bazel/protractor/protractor_web_test.bzl",
        "./web/gui2/node_modules/@bazel/typescript/third_party/github.com/bazelbuild/bazel/src/main/protobuf/BUILD.bazel",
    ],
    mode = "fix",
)
