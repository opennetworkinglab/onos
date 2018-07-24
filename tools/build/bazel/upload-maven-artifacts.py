#!/usr/bin/env python
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

from subprocess import call
import tempfile
import hashlib
import requests, os
import xml.etree.ElementTree, shutil

SONATYPE_USER=os.environ.get("SONATYPE_USER")
SONATYPE_PASSWORD=os.environ.get("SONATYPE_PASSWORD")
SONATYPE_PROFILE=os.environ.get("SONATYPE_PROFILE")

CREATE_REPO_REQUEST_TEMPLATE = '''\
<promoteRequest>
    <data>
      <description>%(description)</description>
    </data>
</promoteRequest>
'''

CLOSE_REPO_REQUEST_TEMPLATE = '''\
<promoteRequest>
    <data>
      <description>%(description)</description>
      <stagedRepositoryId>%(repo_id)</stagedRepositoryId>
    </data>
</promoteRequest>
'''

def hashlib_compute(hash, input_file, output_file):
    with open(input_file, 'rb') as f:
        for block in iter(lambda: f.read(100000), b''):
            hash.update(block)
        md5_string = hash.hexdigest()
        output = open(output_file, "w")
        output.write(md5_string + "\n")
        f.close()
        output.close()


def generate_metadata_files(input_file):
    # create a temporary directory to hold the metadata files
    global tempdir
    base_metadata_filename = tempdir + "/" + os.path.basename(input_file)

    files = []

    # generate the signature file
    signature_filename = base_metadata_filename + ".asc"
    call(["gpg", "--armor", "--detach-sig", "--output", signature_filename,  input_file])
    files.append(signature_filename)

    # generate the md5 checksum file
    md5_filename = base_metadata_filename + ".md5"
    md5 = hashlib.md5()
    hashlib_compute(md5, input_file, md5_filename)
    files.append(md5_filename)

    # generate the SHA checksum file
    sha1_filename = base_metadata_filename + ".sha1"
    sha1 = hashlib.sha1()
    hashlib_compute(sha1, input_file, sha1_filename)
    files.append(sha1_filename)

    return files


def create_staging_repo(description):
    if destination_repo_url is None:
        return None
    create_request = CREATE_REPO_REQUEST_TEMPLATE.replace("%(description)", description)
    url = "https://" + destination_repo_url + "/service/local/staging/profiles" + "/" + SONATYPE_PROFILE + "/start"
    headers = {'Content-Type': 'application/xml'}
    r = requests.post(url, create_request, headers=headers, auth=(SONATYPE_USER, SONATYPE_PASSWORD))
    root = xml.etree.ElementTree.fromstring(r.text)
    repo_id = root.find("data").find("stagedRepositoryId").text
    return repo_id


def close_staging_repo(description, repo_id):
    if repo_id is None:
        return
    close_request = CLOSE_REPO_REQUEST_TEMPLATE.replace("%(description)", description).replace("%(repo_id)", repo_id)
    url = "https://" + destination_repo_url + "/service/local/staging/profiles" + "/" + SONATYPE_PROFILE + "/finish"
    headers = {'Content-Type': 'application/xml'}
    r = requests.post(url, close_request, headers=headers, auth=(SONATYPE_USER, SONATYPE_PASSWORD))


def stage_file(file, repo_id, dest):
    if destination_repo_url is not None:
        # deploy to Nexus repo
        upload_base = "https://" + destination_repo_url + "/service/local/staging/deployByRepositoryId"
        url = upload_base + "/" + repo_id + "/" + os.path.dirname(dest) + "/" + os.path.basename(file)
        headers = {'Content-Type': 'application/xml'}
        with open(file, 'rb') as f:
            r = requests.post(url, files={file: f}, headers=headers, auth=(SONATYPE_USER, SONATYPE_PASSWORD))
    else:
        # deploy to local repo
        dest_local_repo = os.path.expanduser(local_maven_repo + "/" + dest)
        dest_local_repo_dir = os.path.dirname(dest_local_repo)
        if not os.path.isdir(dest_local_repo_dir):
            os.makedirs(dest_local_repo_dir)
        shutil.copyfile(src, dest_local_repo)


def stage_files(files, dest):
    for file in files:
        stage_file(file=file, repo_id=repo_id, dest=dest)


def upload_file(src, dest):
    files = generate_metadata_files(src)
    files.append(src)
    stage_files(files, dest)


if __name__ == '__main__':
    import sys

    if len(sys.argv) < 2:
        print 'USAGE: upload-maven-artifacts catalog-file-name [nexus root url]'
        sys.exit(1)

    input_list_file = sys.argv[1]

    local_maven_repo = None
    destination_repo_url = None

    if len(sys.argv) == 3:
        destination_repo_url = sys.argv[2]
    else:
        local_maven_repo = os.environ.get("MAVEN_REPO")
        if local_maven_repo is None:
            local_maven_repo = "~/.m2/repository"

    if destination_repo_url is not None:
        if SONATYPE_USER is None:
            print "Environment variable SONATYPE_USER must be set"
            sys.exit(1)

        if SONATYPE_PASSWORD is None:
            print "Environment variable SONATYPE_PASSWORD must be set"
            sys.exit(1)

        if SONATYPE_PROFILE is None:
            print "Environment variable SONATYPE_PROFILE must be set"
            sys.exit(1)

        print ("Uploading to remote repo: " + destination_repo_url)
    else:
        print ("Installing in local repo: " + local_maven_repo)

    list_file = open(input_list_file, "r")
    lines = list_file.readlines()
    list_file.close()

    tempdir = tempfile.mkdtemp(prefix="upload-maven-artifacts-")
    description = "test repo"
    repo_id = create_staging_repo(description)
    for line in lines:
        s = line.split()
        src = s[0]
        dest = s[1]
        upload_file(src, dest)
    close_staging_repo(repo_id=repo_id, description=description)
    shutil.rmtree(tempdir)
