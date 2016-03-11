#!/usr/bin/env python
# Copyright (C) 2013 The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

from __future__ import print_function
from optparse import OptionParser
from os import makedirs, path, symlink
from subprocess import check_call
import sys

opts = OptionParser()
opts.add_option('-o', help='path to write WAR to')
opts.add_option('--lib', action='append', help='target for WEB-INF/lib')
opts.add_option('--pgmlib', action='append', help='target for WEB-INF/pgm-lib')
opts.add_option('--tmp', help='temporary directory')
args, ctx = opts.parse_args()

war = args.tmp
jars = set()
basenames = set()

def prune(l):
  return [j for e in l for j in e.split(':')]

def link_jars(libs, directory):
  makedirs(directory)
  for j in libs:
    if j not in jars:
      # When jgit is consumed from its own cell,
      # potential duplicates should be filtered.
      # e.g. jsch.jar will be reached through:
      # 1. /home/username/projects/gerrit/buck-out/gen/lib/jsch.jar
      # 2. /home/username/projects/jgit/buck-out/gen/lib/jsch.jar
      if (j.find('jgit/buck-out/gen/lib') > 0
          and path.basename(j) in basenames):
          continue
      jars.add(j)
      n = path.basename(j)
      if j.find('buck-out/gen/gerrit-') > 0:
        n = j[j.find('buck-out'):].split('/')[2] + '-' + n
      basenames.add(n)
      symlink(j, path.join(directory, n))

if args.lib:
  link_jars(prune(args.lib), path.join(war, 'WEB-INF', 'lib'))
if args.pgmlib:
  link_jars(prune(args.pgmlib), path.join(war, 'WEB-INF', 'pgm-lib'))
try:
  for s in ctx:
    check_call(['unzip', '-q', '-d', war, s])
  check_call(['zip', '-9qr', args.o, '.'], cwd=war)
except KeyboardInterrupt:
  print('Interrupted by user', file=sys.stderr)
  exit(1)
