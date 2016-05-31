#! /bin/bash
#
# Copyright 2014-2016 Open Networking Laboratory
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

# exit on errors
set -e

basedir="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
ns="org.onosproject.bmv2.thriftapi"

# add java namespace at beginning of file
for f in ${basedir}/*.thrift
do
    echo "namespace java ${ns}" | cat - ${f} > temp && mv temp ${f}
done