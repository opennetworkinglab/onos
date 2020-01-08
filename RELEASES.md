# ONOS Release Notes
This documents provides a running log describing high-level features
introduced in each recent ONOS release. Developers are encouraged to describe
their changes to keep the ONOS community up-to-date with the recent developments.

## 2.3 - January 2020
#### Platform
* Upgraded to build via Bazel 1.0 - backported to 2.2 LTS and 1.15 LTS
* Publishing dependencies pom.xml for use by archetypes and 3rd party apps
* Updated 3rd party dependencies
* Fixed and updated ONOS Helm charts

#### GUI 2
* Upgrade to Angular 9 and NodeJS 12
* Native Bazel build of NPM modules; no more genrules and scripts
* Additional views to close the GUI/GUI2 gap
...

#### Stratum Support
* Southbound improvements and bug fixes in P4Runtime, fabric.p4, gNMI, gNOI
* Support Trellis and SEBA/BNG offloading use cases
...


## 2.2 (LTS) - September 2019
#### Platform
* Upgraded to build and run with JDK 11
* Upgraded to build with Bazel 0.27
* Upgraded to run Apache Karaf to 4.2.6
* Packet processing priority queues added by Nikolayi Merinov from Inango Systems

#### GUI 2
* ROADM GUI extended with editable fields for power settings
* GUI2 Framework library reused in µONOS

#### Stratum Support
* `fabric.p4` improvements
  * Initial support for PPPoE-based BNG offloading (bng.p4) (demo at Connect ‘19)
  * Support for ACL clone to CPU (i.e. packet-in) actions via P4Runtime Clone Sessions
  * Enabled Travis CI on fabric-p4test, run PTF-based tests on BMv2
* Added stratum-fpm driver
* Stratum+ONOS interoperability demo (Connect ‘19)
* Transitioned dev environment from VM to Docker


## 2.1 - April 2019
#### Platform
* Upgraded Apache Karaf to 4.2.3
* Upgraded to build with Bazel 0.25

#### GUI 2
* Big focus on Topology View
* Background maps
* Absolute locations
* Grid view
* Multiple node selections
* Display of intents
* Display of mastership
* Dynamic updates to topology
* Moved topology to its own library gui2-topo-lib

#### Stratum Support
* P4Runtime improvements
  * Bumped to P4Runtime v1.0.0
  * New lock-free P4Runtime client implementation (i.e. allow concurrent RPCs) with support for batched write/read requests and detailed error reporting (tested with 100K routes on Trellis)
  * Improved mastership handling (use same backup node preference as in the mastership service)
  * Write P4Runtime Clone Sessions via ONOS Group API
  * Initial support for in-service device pipeline upgrades
* Set port oper status via gNMI
  * Via ONOS CLI command portstate (based on OpenConfig Interfaces model)
* Initial gNOI support (contribution from PLVision)
  * Via ONOS CLI command device-reboot
* Major refactoring of connection handling for all gRPC-based southbounds
  * Use one gRPC channel with multiple clients, allow providers to subscribe to gRPC channel events
* Support building third-party pipeconf-based apps with Maven
* New ONOS+P4 tutorial “Build an SRv6-enabled fabric with ONOS and P4”
  * https://github.com/opennetworkinglab/onos-p4-tutorial
* New drivers
  * stratum-bmv2, stratum-tofino (replaces private Barefoot driver)
  * gnmi-standalone (Allow using gNMI as standalone protocol)

#### SONA - CNI
* Supported PODs communication
* Supported Service IP communication
* Supported NodePort based service exposure (inter-node only for now)
* Implemented IPAM CNI extension by leveraging Atomix store
* Implemented k8s node bootstrapping (kbr-int, kbr-ex bridge and tunnel port provisioning)
* Cached k8s resource to Atomix by implementing k8s watcher (relies on fabric8 dependency)
* Implemented CLIs for querying k8s resources (POD, service, endpoints, ingress, etc.)


## 2.0 - January 2019
#### Platform
* Upgraded Apache Karaf to 4.2.2
* Build with Bazel 0.21.0 and JDK 8 or OpenJDK 8 - JDK 11 build pushed out
* Upgraded to Atomix 3.1

#### GUI 2
* Topology View migration continued
  * Hosts icon implemented
  * Icons enhanced
  * Details view - driven by context and linking
  * Toolbar - keyboard shortcuts and icons
  * Traffic monitoring
  * Localization support

#### Stratum Support
* Upgraded P4Runtime version to 1.0.0-rc3
* Initial support for gNMI and OpenConfig Interfaces model
  * Port discovery, stats polling, subscription to port up/down events
* Added Stratum driver (extends P4Runtime and gNMI drivers)
* Various improvements to fabric.p4
  * Added support for Double-VLAN cross-connect
  * Major refactoring of fabric.p4 to optimize HW pipeline stage utilization (went from 10 to 6 stages)
  * Refactoring of pipeliner implementation
* Various bugfixes for gRPC client handling
* Initial support for gRPC secure channels via SSL/TLS (Brian)

#### ODTN
* Phase 1.0 expand/refactor support for TX discovery and programming
  * Cassini
  * Infinera XT-3300
  * FlowRuleProgrammable OpenConfig for phase 1.5
* Updated TAPI to 2.1.1 version
* Updated OpenConfig Models to Reference Design Spec
* DCS 2.6 release with bugfixes
* Various RESTCONF/Netconf bugfixes


## 1.15 (LTS) - December 2018
#### Platform
* Upgraded build of ONOS code-base to use Bazel rather than Buck
* Removed vestigal pom.xml and BUCK files
* Atomix / ONOS Cluster enhancements
  * Migrated distributed primitives, cluster management, and intra-cluster communication to Atomix
  * Re-architected ONOS cluster to store consistent, persistent state externally and tolerate n-1 failures
* ISSU
  * Support rolling upgrades of Atomix clusters for introducing bug fixes and new features
  * Created a test framework for validating backwards compatibility for upgrades

#### GUI 2
* Rebuilt GUI application on Angular v6 - major upgrade from 1.3.x
* Angular CLI Build, Test and Lint added to BUCK build and test
* Framework code mostly ported to Angular 6
  * Some refactoring to align with Angular 6 philosophy i.e. no d3 element manipulations
  * LION, WebSockets, Menu, Navigation all ported
* Views ported:
  * Devices, Apps, Flows, Ports, Links, Group, Meter, Tunnel, Hosts, Settings, Clusters, Partitions
* Replaced Table Builder and Table Details Builder with base classes made possible in ES6
* Enhancements - Lazy loading - load view only when necessary
* Very little changes to backend Java code

#### P4 / P4Runtime
* In-band Network Telemetry (INT) ONOS service and reference int.p4 implementation
* Added new features to fabric.p4
  * VLAN tagged ports, ARP request broadcast, IPv4 multicast, “clone to controller” behavior in ACL table (for cloning ARP requests), initial INT support
* Created PTF-based fabric.p4 data plane tests
  * https://github.com/opennetworkinglab/fabric-p4test
  * Test cases for forwarding, GTP termination, and INT
* Created STC scenario to deploy fabric.p4
* P4Runtime southbound improvements
  * Changes to improve scalability (tested up to 200 BMv2 instances on 3-node ONOS cluster)
  * Support re-connection to lost devices
  * Support for multicast APIs (via packet replication engine)
* P4Runtime-based driver for Mellanox Spectrum-based switches
* Improved learning material: ONOS+P4 tutorial
  * Includes hands-on exercises and new “MyTunnel” ONOS app (apps/p4-tutorial)

