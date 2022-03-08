#!/usr/bin/env python3
"""
 Copyright 2021-present Open Networking Foundation

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

from subprocess import call
import requests, os
import argparse
import sys


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description='Upload artifacts with Maven.')
    parser.add_argument('catalog_file_name', type=str, help="Catalog File Name")
    parser.add_argument('destination_repo_url', type=str, nargs="?", default="",
                        help="Nexus complete URL")
    parser.add_argument('--local_settings', type=str, required=False, default=None,
                        help="Use a local settings.xml instead of the default one")
    parser.add_argument('--repo_id', type=str, required=False, default="ossrh",
                        help="ID to map on the <id> under <server> section of settings.xml")
    parser.add_argument('--group_id', type=str, required=False, default="org.onosproject",
                        help="GroupId of the artifacts to be deployed")
    parser.add_argument('--dry_run', action="store_true", required=False, default=False)
    args = parser.parse_args()

    input_list_file = args.catalog_file_name
    destination_repo_url = args.destination_repo_url
    local_settings_file = args.local_settings
    repo_id = args.repo_id
    group_id = args.group_id
    dry_run = args.dry_run

    with open(input_list_file, "r") as list_file:
        lines = list_file.readlines()

    artifacts_to_upload = {}
    for line in lines:
        s = line.split()
        src = s[0]
        dest = s[1]
        artifactId = dest.split("/")[2]
        info = artifacts_to_upload.get(artifactId, {})
        version = dest.split("/")[3]
        if info.get("version", None) is not None:
            if info["version"] != version:
                print("ERROR: Version mismatch: " + artifactId + ", Version: " +
                      info["version"], ", New Version: " + version)
                sys.exit(1)
        else:
            info['version'] = version
        artifacts = info.get("artifacts", {})
        src_and_extension = [src, os.path.splitext(src)[1].replace(".", '')]
        if "-sources" in src:
            artifacts["sources"] = src_and_extension
        elif "-javadoc" in src:
            artifacts["javadoc"] = src_and_extension
        elif "-oar" in src:
            artifacts["oar"] = src_and_extension
        elif "-tests" in src:
            artifacts["tests"] = src_and_extension
        elif "-pom" in src:
            artifacts["pom"] = src_and_extension
        elif src.endswith(".jar"):
            artifacts["jar"] = src_and_extension
        # Exclude archives for now
        # elif src.endswith(".tar.gz"):
        #     artifacts["tar"] = [src, "targz"]
        else:
            print("WARNING: artifact " + dest + " not supported, skipping")
        info["artifacts"] = artifacts
        artifacts_to_upload[artifactId] = info

    if dry_run:
        print("------- Artifacts dictionary ---------")
        print(artifacts_to_upload)
        print("--------------------------------------")
        print()

    # Upload the artifacts to the remote Nexus repository
    for artifact_id, info in artifacts_to_upload.items():
        artifacts = info["artifacts"]
        call_funct = ["mvn",
                      "-B",  # Run in batch mode
                      "-q",  # Run in quiet mode, will report only warning or errors
                      "deploy:deploy-file",
                      "-Durl=" + destination_repo_url,
                      "-DrepositoryId=" + repo_id,
                      "-DgroupId=" + group_id,
                      "-DartifactId=" + artifact_id,
                      "-Dversion=" + info["version"]]
        if local_settings_file is not None:
            call_funct.insert(1, "-s")
            call_funct.insert(2, local_settings_file)

        if artifact_id == "onos-dependencies":
            # Special case for onos-dependencies, the JAR is a fake empty jar.
            # For this case, upload only the pom.
            if "pom" in artifacts.keys():
                call_funct.append("-Dfile=" + artifacts["pom"][0])
                call_funct.append("-DgeneratePom=false")
                del artifacts["pom"]
            if "jar" in artifacts.keys():
                del artifacts["jar"]
        else:
            # If we have a pom, use it, otherwise do not let Maven generate one
            if "pom" in artifacts.keys():
                call_funct.append("-DpomFile=" + artifacts["pom"][0])
                del artifacts["pom"]
            else:
                call_funct.append("-DgeneratePom=false")

            # Find the main artifact, it can be a JAR or a OAR
            if "jar" in artifacts.keys():
                call_funct.append("-Dfile=" + artifacts["jar"][0])
                del artifacts["jar"]
            elif "oar" in artifacts.keys():
                call_funct.append("-Dfile=" + artifacts["oar"][0])
                del artifacts["oar"]
            else:
                print("WARNING: Skipping, no main artifact for artifact ID: " + artifact_id)
                continue

        if "javadoc" in artifacts.keys():
            call_funct.append("-Djavadoc=" + artifacts["javadoc"][0])
            del artifacts["javadoc"]
        if "sources" in artifacts.keys():
            call_funct.append("-Dsources=" + artifacts["sources"][0])
            del artifacts["sources"]

        # Build the list of the other files to upload together with the main artifact
        other_files = []
        other_files_types = []
        other_files_classifier = []
        for key, val in artifacts.items():
            other_files.append(val[0])
            other_files_types.append(val[1])
            other_files_classifier.append(key)
        if len(other_files) > 0:
            call_funct.append("-Dfiles=" + ",".join(other_files))
            call_funct.append("-Dtypes=" + ",".join(other_files_types))
            call_funct.append("-Dclassifiers=" + ",".join(other_files_classifier))
        if dry_run:
            print(artifact_id + "\n" + " ".join(call_funct))
            print()
        else:
            call(call_funct)
