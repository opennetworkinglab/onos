#!/usr/bin/env python
#FIXME Add license

import re
from zipfile import ZipFile

def stageOnos(output, files=[]):
    # Note this is not a compressed zip
    with ZipFile(output, 'a') as output:
        written_files = set(output.namelist())
        for file in files:
            if '.zip' in file:
                with ZipFile(file, 'r') as zip_part:
                    for f in zip_part.namelist():
                        dest = 'apache-karaf-3.0.5/system/' + f
                        if dest not in written_files:
                            output.writestr(dest, zip_part.open(f).read())
                            written_files.add(dest)
            elif '.oar' in file:
                with ZipFile(file, 'r') as oar:
                    app_xml = oar.open('app.xml').read()
                    app_name = re.search('name="([^"]+)"', app_xml).group(1)
                    dest = 'apps/%(name)s/%(name)s.oar' % { 'name': app_name}
                    output.write(file, dest)
                    dest = 'apps/%s/app.xml' % app_name
                    output.writestr(dest, app_xml)
                    for f in oar.namelist():
                        if 'm2' in f:
                            dest = 'apache-karaf-3.0.5/system/' + f[3:]
                            if dest not in written_files:
                                output.writestr(dest, oar.open(f).read())
                                written_files.add(dest)
            elif 'features.xml' in file:
                dest = 'apache-karaf-3.0.5/system/org/onosproject/onos-features/1.6.0-SNAPSHOT/'
                dest += 'onos-features-1.6.0-SNAPSHOT-features.xml'
                with open(file) as f:
                    output.writestr(dest, f.read())
            # filename = file.split('/')[-1]
            # if mvnCoords == 'APP':
            #     dest = filename
            # else:
            #     groupId, artifactId, version = mvnCoords.split(':')
            #     groupId = groupId.replace('.', '/')
            #     extension = filename.split('.')[-1]
            #     if extension == 'jar':
            #         filename = '%s-%s.jar' % ( artifactId, version )
            #     elif 'features.xml' in filename:
            #         filename = '%s-%s-features.xml' % ( artifactId, version )
            #     dest = 'system/%s/%s/%s/%s' % ( groupId, artifactId, version, filename )
            # zip.write(file, dest)
        output.writestr('apps/org.onosproject.drivers/active', '')
        output.writestr('apps/org.onosproject.openflow-base/active', '')
        output.writestr('apps/org.onosproject.lldp/active', '')
        output.writestr('apps/org.onosproject.host/active', '')

if __name__ == '__main__':
    import sys

    if len(sys.argv) < 2:
        print 'USAGE'
        sys.exit(1)

    output = sys.argv[1]
    args = sys.argv[2:]

    # if len(args) % 2 != 0:
    #     print 'There must be an even number of args: file mvn_coords'
    #     sys.exit(2)

    #files = zip(*[iter(args)]*2)
    stageOnos(output, args)
