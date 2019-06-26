"""
 Copyright 2018-present Open Networking Foundation

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
"""

"""
Extension to genrule that has the JDK bin directory in the PATH, thus allowing
to invoke commands like jar directly in the genrule "cmd" attribute.
This allows using JDK-related tools on a host system that does not have the JDK
installed, instead using the current JDK used by Bazel, e.g. the embedded or
remote one.
"""

def jdk_genrule(
        cmd,
        tools = [],
        toolchains = [],
        **kwargs):
    new_tools = tools + ["@bazel_tools//tools/jdk:current_java_runtime"]
    new_toolchains = toolchains + ["@bazel_tools//tools/jdk:current_java_runtime"]

    # Add JAVABASE/bin to the PATH env.
    # Prepend PWD (sandbox path) if JAVABASE is not an absolute path.
    new_cmd = "echo 'if [[ \"$(JAVABASE)\" = /* ]]; then " + \
              "JHOME=$(JAVABASE); " + \
              "else JHOME=$$PWD/$(JAVABASE); " + \
              "fi' > jdk_genrule_setup.sh; " + \
              "echo 'export PATH=$$JHOME/bin:$$PATH:' >> jdk_genrule_setup.sh; " + \
              "source jdk_genrule_setup.sh; " + cmd

    native.genrule(
        cmd = new_cmd,
        tools = new_tools,
        toolchains = new_toolchains,
        **kwargs
    )
