#!/bin/bash

# Generate code from XML schema
# it would be better to specify path to latest xjc (e.g., one with JDK9)
XJC=${XJC:-xjc}
$XJC -p org.onosproject.netconf.rpc -d src/main/java -no-header https://www.iana.org/assignments/xml-registry/schema/netconf.xsd

# Adding Copyright header + checkstyle ignore comment
# patching javadoc syntax issue
for s in src/main/java/org/onosproject/netconf/rpc/*.java ; do
  ed -s $s << EOF
H
0a
/*
 * Copyright 2018-present Open Networking Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy
 * of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
// CHECKSTYLE:OFF
.
w
,g/&lt;/s|>|\&gt;|
.
w
EOF
  chmod 444 $s
done
