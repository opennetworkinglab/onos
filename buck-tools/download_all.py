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

from optparse import OptionParser
import re
from subprocess import check_call, CalledProcessError, Popen, PIPE

MAIN = ['//buck-tools/eclipse:classpath']
PAT = re.compile(r'"(//.*?)" -> "//buck-tools:download_file"')
# TODO(davido): Remove this hack when Buck bugs are fixed:
# https://github.com/facebook/buck/issues/656
# https://github.com/facebook/buck/issues/658
JGIT = re.compile(r'//org.eclipse.jgit.*')
CELL = '//lib/jgit'

opts = OptionParser()
opts.add_option('--src', action='store_true')
args, _ = opts.parse_args()

targets = set()

p = Popen(['buck', 'audit', 'classpath', '--dot'] + MAIN, stdout = PIPE)
for line in p.stdout:
  m = PAT.search(line)
  if m:
    n = m.group(1)
    if JGIT.match(n):
      n = CELL + n[1:]
    if args.src and n.endswith('__download_bin'):
      n = n[:-13] + 'src'
    targets.add(n)
r = p.wait()
if r != 0:
  exit(r)

try:
  check_call(['buck', 'build'] + sorted(targets))
except CalledProcessError as err:
  exit(1)
