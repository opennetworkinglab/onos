#!/usr/bin/env python3
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

POM_HEADER = '''\
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" 
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>org.onosproject</groupId>
        <artifactId>onos-base</artifactId>
        <version>1</version>
    </parent>'''
POM_FOOTER = '</project>\n'

ID_BLOCK = '''
    <groupId>%s</groupId>
    <artifactId>%s</artifactId>
    <version>%s</version>
    <name>%s</name>
'''

DEPENDENCIES_HEADER = '    <dependencies>\n'
DEPENDENCIES_FOOTER = '    </dependencies>\n'

DEPENDENCY_BLOCK = '''        <dependency>
            <groupId>%s</groupId>
            <artifactId>%s</artifactId>
            <version>%s</version>
        </dependency>
'''

def write(name, msg):
    if name is not None:
        with open(name, "w") as file:
            file.write(msg)
    else:
        print(msg)


def write_pom(output, coords, deps):
    mvn = coords.split(':')
    lines = POM_HEADER
    lines += ID_BLOCK % (mvn[1], mvn[2], mvn[3], mvn[2])
    lines += DEPENDENCIES_HEADER

    for dep in deps:
        mvn = dep.split(':')
        lines += DEPENDENCY_BLOCK % (mvn[1], mvn[2], mvn[len(mvn)-1])

    lines += DEPENDENCIES_FOOTER
    lines += POM_FOOTER
    write(output, lines)

if __name__ == '__main__':
    import sys

    if len(sys.argv) < 3:
        print('usage: pom_generator pom.xml maven_coords dep_coords1 dep_coords2 ...')
        sys.exit(1)

    output = sys.argv[1]
    coords = sys.argv[2]
    deps = sys.argv[3:]

    write_pom(output, coords, deps)
