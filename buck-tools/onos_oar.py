#!/usr/bin/env python
#FIXME Add license

from zipfile import ZipFile

def generateOar(output, files=[]):
    # Note this is not a compressed zip
    with ZipFile(output, 'w') as zip:
        for file, mvnCoords in files:
            filename = file.split('/')[-1]
            if mvnCoords == 'NONE':
                dest = filename
            else:
                groupId, artifactId, version = mvnCoords.split(':')
                groupId = groupId.replace('.', '/')
                extension = filename.split('.')[-1]
                if extension == 'jar':
                    filename = '%s-%s.jar' % ( artifactId, version )
                elif 'features.xml' in filename:
                    filename = '%s-%s-features.xml' % ( artifactId, version )
                dest = 'm2/%s/%s/%s/%s' % ( groupId, artifactId, version, filename )
            zip.write(file, dest)

if __name__ == '__main__':
    import sys

    if len(sys.argv) < 2:
        print 'USAGE'
        sys.exit(1)

    output = sys.argv[1]
    args = sys.argv[2:]

    if len(args) % 2 != 0:
        print 'There must be an even number of args: file mvn_coords'
        sys.exit(2)

    files = zip(*[iter(args)]*2)
    generateOar(output, files)
