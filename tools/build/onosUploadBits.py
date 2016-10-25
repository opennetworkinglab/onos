#!/usr/bin/env python
# -----------------------------------------------------------------------------
# Uploads ONOS distributable bits.
# -----------------------------------------------------------------------------

#FIXME need to export s3Creds

import re
from os import listdir
from os.path import isfile, join

from uploadToS3 import uploadFile

nightlyTag = 'NIGHTLY'
bitsPath = '/tmp'

prefix = 'onos-(?:test-)?(\d+\.\d+\.\d+)'
buildNum = '\.?([\w-]*)'
ext = '\.(?:tar\.gz|zip|deb|noarch\.rpm)'

def findBits( path, target_version=None ):
    for file in listdir( path ):
        filePath = join( path, file )
        if not isfile( filePath ):
            continue

        regex = prefix + buildNum + ext
        match = re.match( regex, file )
        if match:
            version = match.group(1)
            if target_version is not None and version != target_version:
                print 'Skipping %s...' % filePath
                continue
            build = match.group(2)
            if build:
                if 'NIGHTLY' in build or 'rc' in build:
                    uploadFile(filePath, dest='nightly/')
            else:
                #no build; this is a release
                uploadFile(filePath, dest='release/')

if __name__ == '__main__':
    import sys

    version = sys.argv[1] if len(sys.argv) >= 2 else None
    findBits( '/tmp', version )
