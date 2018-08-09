load("@bazel_tools//tools/build_defs/repo:http.bzl", "http_archive")

COMMIT = "37cc0e5acae50ee91f00827a7010c3b07dfa5311"
SHA = "17d023f48ea290f25edaf25a967973b5a42ce6d71b1570862f302d95aa8b9f77"

def generate_googleapis():
    http_archive(
        name = "com_github_googleapis",
        urls = ["https://github.com/googleapis/googleapis/archive/%s.zip" % COMMIT],
        sha256 = SHA,
        strip_prefix = "googleapis-" + COMMIT,
        build_file = "//tools/build/bazel:googleapis_BUILD",
    )
