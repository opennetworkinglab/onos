# Copyright 2015 The Bazel Authors. All rights reserved.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

def _impl(ctx):
  jar = ctx.outputs.jar

  src_list = ""
  for src in ctx.files.srcs:
    if src.path.endswith(".srcjar"):
      src_list += " " + src.path

  cmd = [
      "for sj in %s; do jar xf $sj; done" % src_list,
      "dir=$(find . -type d -name java)",
      "[ -n \"$dir\" -a -d \"$dir\" ] && jar cf %s -C $dir ." % jar.path,
  ]

  ctx.action(
      inputs = ctx.files.srcs,
      outputs = [jar],
      progress_message = "Generating source jar for %s" %  ctx.attr.name,
      command = ";\n".join(cmd)
  )

java_sources = rule(
    attrs = {
        "srcs": attr.label_list(allow_files = True),
    },
    implementation = _impl,
    outputs = {"jar" : "%{name}.jar"},
)