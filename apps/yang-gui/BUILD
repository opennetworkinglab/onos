COMPILE_DEPS = CORE_DEPS + JACKSON + ONOS_YANG + [
    "@onos_yang_runtime//jar",
    "//apps/yang:onos-apps-yang",
]

osgi_jar_with_tests(
    resources = glob(["src/main/resources/**"]),
    resources_root = "src/main/resources",
    deps = COMPILE_DEPS,
)

onos_app(
    category = "GUI",
    description = "Adds GUI extension that allows the operator to view the list of currently " +
                  "registered YANG models, and their YANG sources. It also provides means for compiling " +
                  "source YANG files (*.yang, *.zip, *.jar) on the fly and directly from the GUI using " +
                  "drag-n-drop.<p/>User can simply compress a set of YANG source files and drop it on the " +
                  "YANG Models GUI view to automatically compile and then register the compiled YANG models.",
    required_apps = ["org.onosproject.yang"],
    title = "YANG Runtime GUI",
    url = "http://onosproject.org",
)
