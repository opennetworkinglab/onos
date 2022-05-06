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

from zipfile import ZipFile, ZipInfo

# Utility to write out the ZIP bundle containing the artifacts required by an
# OSGI feature.

def writeFeatureBundle(output, files=[]):
    with ZipFile(output, 'w') as zip:
        for file, mvnCoords in files:
            filename = file.split('/')[-1]
            if mvnCoords == 'NONE':
                dest = filename
            else:
                parts = mvnCoords.split(':')
                if len(parts) > 4:
                    parts.insert(3, parts.pop()) # move version to the 3rd position
                groupId, artifactId, version = parts[1:4]
                groupId = groupId.replace('.', '/')
                extension = filename.split('.')[-1]
                if extension == 'jar':
                    filename = '%s-%s.jar' % ( artifactId, version )
                elif 'features.xml' in filename:
                    filename = '%s-%s-features.xml' % ( artifactId, version )
                dest = '%s/%s/%s/%s' % ( groupId, artifactId, version, filename )
            f = open(file, 'rb')
            zip.writestr(ZipInfo(dest, date_time=(1980, 1, 1, 0, 0, 0)), f.read())
            f.close()

if __name__ == '__main__':
    import sys

    if len(sys.argv) < 2:
        print('USAGE')
        sys.exit(1)

    output = sys.argv[1]
    args = sys.argv[2:]

    if len(args) % 2 != 0:
        print('There must be an even number of args: file mvn_coords')
        sys.exit(2)

    files = list(zip(*[iter(args)]*2))
    writeFeatureBundle(output, files)
