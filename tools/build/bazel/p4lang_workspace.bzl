GITHUB_BASE_URL = "https://github.com"

PI_COMMIT_SHORT = "59c9409"
PI_COMMIT = "59c940916b4f5b182f33b4788d8c410972eaecce"
PI_REPO = "p4lang/PI"

GOOGLE_RPC_COMMIT_SHORT = "916e66d"
GOOGLE_RPC_COMMIT = "916e66d03a4f4716937b3bfa6539b9de8a598b7c"
GOOGLE_RPC_REPO = "googleapis/googleapis"

def _format_github_url(repo, commit):
    return GITHUB_BASE_URL + "/{0}/tarball/{1}".format(repo, commit)

def generate_p4lang():
    native.http_archive(
        name = "com_google_protobuf",
        sha256 = "cef7f1b5a7c5fba672bec2a319246e8feba471f04dcebfe362d55930ee7c1c30",
        strip_prefix = "protobuf-3.5.0",
        urls = ["https://github.com/google/protobuf/archive/v3.5.0.zip"],
    )

    native.new_http_archive(
        name = "p4lang_pi",
        urls = [_format_github_url(PI_REPO, PI_COMMIT)],
        build_file = "//tools/build/bazel:p4lang_BUILD",
        strip_prefix = "p4lang-PI-" + PI_COMMIT_SHORT + "/proto",
        type = "tar.gz",
    )

    native.new_http_archive(
        name = "google_rpc",
        urls = [_format_github_url(GOOGLE_RPC_REPO, GOOGLE_RPC_COMMIT)],
        build_file = "//tools/build/bazel:google_RPC_BUILD",
        strip_prefix = "googleapis-googleapis-" + GOOGLE_RPC_COMMIT_SHORT,
        type = "tar.gz",
    )
