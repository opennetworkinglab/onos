#!/usr/bin/env python
#FIXME Add license

from zipfile import ZipFile, ZipInfo

def generateOar(output, files=[]):
    # Note this is not a compressed zip
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
        print 'USAGE'
        sys.exit(1)

    output = sys.argv[1]
    args = sys.argv[2:]

    if len(args) % 2 != 0:
        print 'There must be an even number of args: file mvn_coords'
        sys.exit(2)

    files = zip(*[iter(args)]*2)
    generateOar(output, files)
