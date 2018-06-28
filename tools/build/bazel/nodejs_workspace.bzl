load("@build_bazel_rules_nodejs//:defs.bzl", "yarn_install", "npm_install")

def packages_example_setup_workspace():

  npm_install(
      name = "packages_install",
      package_json = "//tools/gui:package.json",
      package_lock_json = "@packages_example//:package-lock.json",
      data = ["@packages_example//:postinstall.js"],
  )

