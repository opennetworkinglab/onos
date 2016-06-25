#!/usr/bin/env python
#FIXME Add license

import re
import os
from zipfile import ZipFile
from tarfile import TarFile, TarInfo
import tarfile
import time
from cStringIO import StringIO

VERSION = '1.7.0' #FIXME version, and maybe git commit hash
BASE = 'onos-%s/' % VERSION


written_files = set()
now = time.time()

def addFile(tar, dest, file, file_size):
    if dest not in written_files:
        info = TarInfo(dest)
        info.size = file_size
        info.mtime = now
        info.mode = 0777
        tar.addfile(info, fileobj=file)
        written_files.add(dest)

def addString(tar, dest, string):
    if dest not in written_files:
        print dest, string
        info = TarInfo(dest)
        info.size = len(string)
        info.mtime = now
        info.mode = 0777
        file = StringIO(string)
        tar.addfile(info, fileobj=file)
        file.close()
        written_files.add(dest)

def stageOnos(output, files=[]):
    # Note this is not a compressed zip
    with tarfile.open(output, 'w:gz') as output:
        for file in files:
            if '.zip' in file:
                with ZipFile(file, 'r') as zip_part:
                    for f in zip_part.infolist():
                        dest = f.filename
                        if BASE not in dest:
                            dest = BASE + 'apache-karaf-3.0.5/system/' + f.filename
                        addFile(output, dest, zip_part.open(f), f.file_size)
            elif '.oar' in file:
                with ZipFile(file, 'r') as oar:
                    app_xml = oar.open('app.xml').read()
                    app_name = re.search('name="([^"]+)"', app_xml).group(1)
                    dest = BASE + 'apps/%(name)s/%(name)s.oar' % { 'name': app_name}
                    addFile(output, dest, open(file), os.stat(file).st_size)
                    dest = BASE + 'apps/%s/app.xml' % app_name
                    addString(output, dest, app_xml)
                    for f in oar.infolist():
                        filename = f.filename
                        if 'm2' in filename:
                            dest = BASE + 'apache-karaf-3.0.5/system/' + filename[3:]
                            if dest not in written_files:
                                addFile(output, dest, oar.open(f), f.file_size)
                                written_files.add(dest)
            elif 'features.xml' in file:
                dest = BASE + 'apache-karaf-3.0.5/system/org/onosproject/onos-features/1.7.0-SNAPSHOT/'
                dest += 'onos-features-1.7.0-SNAPSHOT-features.xml'
                with open(file) as f:
                    addFile(output, dest, f, os.stat(file).st_size)
        addString(output, BASE + 'apps/org.onosproject.drivers/active', '')
        addString(output, BASE + 'VERSION', VERSION)

if __name__ == '__main__':
    import sys

    if len(sys.argv) < 2:
        print 'USAGE'
        sys.exit(1)

    output = sys.argv[1]
    args = sys.argv[2:]

    stageOnos(output, args)
