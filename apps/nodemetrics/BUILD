BUNDLES = [
    "//apps/nodemetrics/api:onos-apps-nodemetrics-api",
    "//apps/nodemetrics/mgr:onos-apps-nodemetrics-mgr",
]

onos_app(
    app_name = "org.onosproject.nodemetrics",
    category = "Monitoring",
    description = "1.Nodemetrics Application uses, sigar library to fetch Controller information." +
                  "2. The Sigar library uses Native libraries and currently It supports Windows, Linux and MacOs platform." +
                  "3. The Native libraries like .so, .dll and .dylib are packed as jar along with sigar libs." +
                  "4. If the Native libraries are corrupted because of any reason," +
                  "so, the controller is vulnerable to crash of the entire JVM",
    included_bundles = BUNDLES,
    title = "Controller Monitor Application",
    url = "http://samsung.com",
)
