#!/usr/bin/env python3
# Copyright 2019-present Open Networking Foundation
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
import argparse
from datetime import datetime
from xml.dom import minidom


def resolve(mvn_coord):
    mvn_pieces = mvn_coord.split(":")
    if mvn_pieces[0] != "mvn":
        raise ValueError("Invalid Maven coordinate: %s" % mvn_coord)
    return dict(
        groupId=mvn_pieces[1],
        artifactId=mvn_pieces[2],
        version=mvn_pieces[-1],
        name=mvn_coord,
    )


def xml_beautify(data):
    beautified = '\n'.join([
        l for l in
        minidom.parseString(data).toprettyxml(indent=' ' * 4).split('\n')
        if l.strip()])
    return beautified


def generate_pom(out_file, template_file, provided_deps, test_deps, deps, var_dict):
    deps = {d: resolve(d) for d in deps}

    dep_mgmt_template = """
    <dependency>
        <!-- {name} -->
        <groupId>{groupId}</groupId>
        <artifactId>{artifactId}</artifactId>
        <version>{version}</version>
    </dependency>"""

    dep_template = """
    <dependency>
        <!-- {name} -->
        <groupId>{groupId}</groupId>
        <artifactId>{artifactId}</artifactId>
        <scope>{scope}</scope>
    </dependency>"""

    mgmt_deps = sorted(deps.keys())
    provided_deps.sort()
    test_deps.sort()

    with open(template_file, "r") as f:
        lines = f.readlines()

    new_lines = [
        "<!-- Automatically generated on %s -->"
        % datetime.now().strftime("%Y-%m-%d %H:%M:%S")
    ]
    for line in lines:
        if "<!-- DEPS_MGMT -->" in line:
            new_lines.extend([
                dep_mgmt_template.format(**deps[x]) for x in mgmt_deps])
        elif "<!-- DEPS -->" in line:
            new_lines.extend([
                dep_template.format(scope='provided', **deps[x])
                for x in provided_deps])
            new_lines.extend([
                dep_template.format(scope='test', **deps[x])
                for x in test_deps])
        else:
            for old, new in list(var_dict.items()):
                line = line.replace(old, new)
            new_lines.append(line)

    with open(out_file, 'w') as f:
        f.write(xml_beautify("\n".join(new_lines)))


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('-o', dest='out_file', type=str, action="store",
                        required=True, help="Path to output file")
    parser.add_argument('-p', dest='template_file', type=str, action="store",
                        required=True, help="Path to pom template file")
    parser.add_argument('-c', dest='provided_deps', metavar='PROVIDED_DEP',
                        type=str, nargs='+', default=[],
                        help='Maven coordinates to list with scope provided')
    parser.add_argument('-t', dest='test_deps', metavar='TEST_DEP', type=str,
                        nargs='+', default=[],
                        help='Maven coordinates to list with scope test')
    parser.add_argument('-d', dest='deps', metavar='DEP', type=str,
                        nargs='+', default=[],
                        help='Maven coordinates to list under <dependencyManagement>')
    parser.add_argument('-v', dest='vars', metavar='VAR=value', type=str,
                        nargs='+', default=[],
                        help='Replace all instances of <!-- VAR --> with the given value')
    args = parser.parse_args()

    processed_vars = {}
    for var in args.vars:
        pieces = var.split('=')
        if len(pieces) != 2:
            raise ValueError("Invalid var '%s'" % var)
        processed_vars["<!-- %s -->" % pieces[0]] = pieces[1]

    generate_pom(
        out_file=args.out_file,
        template_file=args.template_file,
        provided_deps=args.provided_deps,
        test_deps=args.test_deps,
        deps=args.deps,
        var_dict=processed_vars
    )
