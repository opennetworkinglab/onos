load("@bazel_tools//tools/build_defs/repo:http.bzl", "http_archive")
load("@npm_bazel_protractor//:package.bzl", "npm_bazel_protractor_dependencies")
load("@npm_bazel_karma//:package.bzl", "npm_bazel_karma_dependencies")
load("@io_bazel_rules_webtesting//web:repositories.bzl", "web_test_repositories")
load("@io_bazel_rules_webtesting//web/versioned:browsers-0.3.2.bzl", "browser_repositories")
load("@npm_bazel_typescript//:index.bzl", "ts_setup_workspace")
load("@io_bazel_rules_sass//sass:sass_repositories.bzl", "sass_repositories")

def load_angular():
    npm_bazel_protractor_dependencies()

    npm_bazel_karma_dependencies()

    web_test_repositories()

    browser_repositories(
        chromium = True,
        firefox = True,
    )

    ts_setup_workspace()

    sass_repositories()
