# Copyright 2018-present Open Networking Foundation
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

def _local_jar_impl(repository_ctx):
    file = repository_ctx.attr.path.split("/")[-1]
    repository_ctx.symlink(repository_ctx.attr.path, "%s" % file)
    repository_ctx.file("BUILD", content = """
# DO NOT EDIT: automatically generated BUILD file for local_jar rule
java_import(
    name = "%s",
    jars = ["%s"],
    visibility = ['//visibility:public']
)
    """ % (repository_ctx.attr.name, file))
    repository_ctx.file("WORKSPACE", content = """
# DO NOT EDIT: automatically generated BUILD file for local_jar rule
workspace(name = "%s")
    """ % repository_ctx.attr.name)
    repository_ctx.file("jar/BUILD", content = """
# DO NOT EDIT: automatically generated BUILD file for local_jar rule

package(default_visibility = ["//visibility:public"])

alias(
    name = "jar",
    actual = "@%s",
)
    """ % repository_ctx.attr.name)

# Workspace rule to allow override of a single locally built 3rd party jar
local_jar = repository_rule(
    implementation = _local_jar_impl,
    local = True,
    attrs = {"path": attr.string(mandatory = True)},
)

# Macro to allow building ONOS against locally-built Atomix artifacts
def local_atomix(path, version):
    local_jar(
        name = "atomix",
        path = "%s/core/target/atomix-%s.jar" % (path, version),
    )
    local_jar(
        name = "atomix_cluster",
        path = "%s/cluster/target/atomix-cluster-%s.jar" % (path, version),
    )
    local_jar(
        name = "atomix_dist",
        path = "%s/dist/target/atomix-dist-%s.jar" % (path, version),
    )
    local_jar(
        name = "atomix_primitive",
        path = "%s/primitive/target/atomix-primitive-%s.jar" % (path, version),
    )
    local_jar(
        name = "atomix_tests",
        path = "%s/tests/target/atomix-tests-%s.jar" % (path, version),
    )
    local_jar(
        name = "atomix_utils",
        path = "%s/utils/target/atomix-utils-%s.jar" % (path, version),
    )
    local_jar(
        name = "atomix_agent",
        path = "%s/agent/target/atomix-agent-%s.jar" % (path, version),
    )
    local_jar(
        name = "atomix_storage",
        path = "%s/storage/target/atomix-storage-%s.jar" % (path, version),
    )
    local_jar(
        name = "atomix_gossip",
        path = "%s/protocols/gossip/target/atomix-gossip-%s.jar" % (path, version),
    )
    local_jar(
        name = "atomix_primary_backup",
        path = "%s/protocols/primary-backup/target/atomix-primary-backup-%s.jar" % (path, version),
    )
    local_jar(
        name = "atomix_raft",
        path = "%s/protocols/raft/target/atomix-raft-%s.jar" % (path, version),
    )
    local_jar(
        name = "atomix_rest",
        path = "%s/rest/target/atomix-rest-%s.jar" % (path, version),
    )

# Macro to allow building ONOS against locally-built YANG tools artifacts
def local_yang_tools(path, version):
    local_jar(
        name = "onos_yang_model",
        path = "%s/model/target/onos-yang-model-%s.jar" % (path, version),
    )
    local_jar(
        name = "onos_yang_compiler_api",
        path = "%s/compiler/api/target/onos-yang-compiler-api-%s.jar" % (path, version),
    )
    local_jar(
        name = "onos_yang_compiler_main",
        path = "%s/compiler/plugin/main/target/onos-yang-compiler-main-%s.jar" % (path, version),
    )
    local_jar(
        name = "onos_yang_runtime",
        path = "%s/runtime/target/onos-yang-runtime-%s.jar" % (path, version),
    )
    local_jar(
        name = "onos_yang_serializers_json",
        path = "%s/serializers/json/target/onos-yang-serializers-json-%s.jar" % (path, version),
    )
    local_jar(
        name = "onos_yang_serializers_xml",
        path = "%s/serializers/xml/target/onos-yang-serializers-xml-%s.jar" % (path, version),
    )
    local_jar(
        name = "onos_yang_serializers_utils",
        path = "%s/serializers/utils/target/onos-yang-serializers-utils-%s.jar" % (path, version),
    )
