COMPILE_DEPS = CORE_DEPS + [
    "//protocols/lisp/api:onos-protocols-lisp-api",
    "//protocols/lisp/msg:onos-protocols-lisp-msg",
    "//apps/mappingmanagement/api:onos-apps-mappingmanagement-api",
]

TEST_DEPS = TEST_ADAPTERS + [
    "//protocols/lisp/api:onos-protocols-lisp-api-tests",
    "//apps/mappingmanagement/api:onos-apps-mappingmanagement-api-tests",
]

osgi_jar_with_tests(
    test_deps = TEST_DEPS,
    deps = COMPILE_DEPS,
)
