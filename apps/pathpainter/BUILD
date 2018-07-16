COMPILE_DEPS = CORE_DEPS + JACKSON

osgi_jar_with_tests(
    resources = glob(["src/main/resources/**"]),
    resources_root = "src/main/resources",
    deps = COMPILE_DEPS,
)

onos_app(
    category = "GUI",
    description = "Extends the GUI topology view by adding an overlay that allows the operator " +
                  "to visualize topology paths using various forms of path-finding algorithms such as " +
                  "the Dijkstra shortest path using default link metric (ONOS default), using geographic " +
                  "distances, disjoint paths, etc.",
    title = "Path Visualization",
    url = "http://onosproject.org",
)
