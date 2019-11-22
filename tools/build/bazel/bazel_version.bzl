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

def check_bazel_version():
    if "bazel_version" not in dir(native):
        fail("\nBazel version is too old; please use 1.* official release!\n\n")
    elif not native.bazel_version:
        print("\nBazel is not a release version; please use 1.* official release!\n\n")
        return

    versions = native.bazel_version.split(".")
    if not int(versions[0]) >= 1:
        fail("\nBazel version %s is not supported; please use 1.* official release!\n\n" %
             native.bazel_version)
