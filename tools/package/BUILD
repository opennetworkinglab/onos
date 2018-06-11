load("//tools/build/bazel:generate_workspace.bzl", "ONOS_VERSION")

KARAF = "@apache_karaf//file"
PATCHES = "@apache_karaf_patches//file"
BRANDING = "//tools/package/branding:onos-tools-package-branding"

# FIXME: This is still work in progress
genrule(
    name = "onos-karaf",
    srcs = [
        KARAF,
        PATCHES,
        BRANDING,
      ] + glob(["bin/*", "etc/*", "init/*", "runtime/bin/*"]),
    outs = ["karaf.zip"],
    cmd = "echo $(location onos-prep-karaf) $(location karaf.zip) $(location %s) %s $(location %s) $(location %s) > $(location karaf.zip)" \
              % (KARAF, ONOS_VERSION, BRANDING, PATCHES),
    tools = ["onos-prep-karaf"],
    visibility = ["//visibility:public"],
)
#    cmd = "$(location onos-prep-karaf) $(location karaf.zip) $(location %s) %s $(location %s) $(location %s)" \
#              % (KARAF, ONOS_VERSION), BRANDING, PATCHES),

PACKAGING_REQUIREMENTS = [
    "//features:onos-features",
    ":onos-karaf",
]

# FIXME: This is still work in progress
genrule(
    name = "onos-package",
    srcs = PACKAGING_REQUIREMENTS + glob(["bin/*", "etc/*", "init/*", "config/*", "runtime/bin/*"]),
    outs = ["onos.tar.gz"],
    cmd = "echo $(location onos_stage.py) $(location onos.tar.gz) $(location :onos-karaf) $(location //features:onos-features) $(SRCS) >$(location onos.tar.gz)",
    visibility = ["//visibility:public"],
    tools = ["onos_stage.py"],
)
#    cmd = "$(location onos_stage.py) $(location onos.tar.gz) $(location :onos-karaf) $(SRCS)",
#    cmd = "$(exe //buck-tools:onos-stage) $OUT " + ONOS_VERSION + " $(location :onos-karaf) " + " ".join(sources),


#staged_repos = ['$(location %s-repo)' % f for f in FEATURES]
#staged_apps = ['$(location %s)' % a for a in APPS]
#
## feature_coords = 'foo:bar:1.3'
#sources = [ '$(location :onos-features)', ]
#sources += staged_repos + staged_apps
#
#tar_file(
#  name = 'onos-package-runtime',
#  srcs = glob(['runtime/bin/*']),
#  root = 'tools/package',
#  out = 'package-runtime.tar.gz',
#  visibility = [ 'PUBLIC' ],
#)
#
#genrule(
#  name = 'onos-package',
#  srcs = glob(['bin/*', 'etc/*', 'init/*', 'config/*', 'runtime/bin/*']),
#  out = 'onos.tar.gz',
#  bash = '$(exe //buck-tools:onos-stage) $OUT ' + ONOS_VERSION + ' $(location :onos-karaf) ' + ' '.join(sources),
#  visibility = [ 'PUBLIC' ],
#)
#
#genrule(
#  name = 'onos-run',
#  out = 'onos-run',
#  srcs = [ 'onos-run-karaf' ],
#  bash = 'sed "s#ONOS_TAR=#ONOS_TAR=$(location :onos-package)#" $SRCS > $OUT; chmod +x $OUT',
#  executable = True,
#  visibility = [ 'PUBLIC' ],
#)
