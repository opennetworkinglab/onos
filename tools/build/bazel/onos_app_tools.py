#!/usr/bin/env python3
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

##### Templates for features.xml
FEATURES_HEADER = '''\
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<features xmlns="http://karaf.apache.org/xmlns/features/v1.2.0"
          name="%(feature_repo_name)s">
'''
FEATURE_HEADER= '''\
    <feature name="%(feature_name)s" version="%(version)s"
             description="%(title)s">
'''
EXISTING_FEATURE = '        <feature>%s</feature>\n'
BUNDLE = '        <bundle>%s</bundle>\n'
FEATURE_FOOTER = '    </feature>\n'
FEATURES_FOOTER = '</features>\n'

##### Templates for app.xml
APP_HEADER = '''\
<?xml version="1.0" encoding="UTF-8"?>
<app name="%(app_name)s" origin="%(origin)s" version="%(version)s"
        title="%(title)s" category="%(category)s" url="%(url)s"
        featuresRepo="%(feature_repo_name)s"
        features="%(feature_name)s" apps="%(apps)s">
    <description>%(description)s</description>
'''
ARTIFACT = '    <artifact>%s</artifact>\n'
SECURITY = '''\
    <security>
%s
    </security>\n'''
APP_FOOTER = '</app>\n'

NON_OSGI_TAG = 'NON-OSGI'

def mvnUrl(bundle):
    #mvn-uri := 'mvn:' [ repository-url '!' ] group-id '/' artifact-id [ '/' [version] [ '/' [type] [ '/' classifier ] ] ] ]
    parts = bundle.split(':')[1:]
    prefix = 'mvn:'
    suffix = ''
    if len(parts) > 3:
        parts.insert(2, parts.pop()) # move version to the 3rd position
    if len(parts) >= 5:
        # check classifier for special non-OSGi tag
        i = parts[4].find(NON_OSGI_TAG)
        if i == 0:
            prefix = 'wrap:' + prefix
            suffix = '$Bundle-SymbolicName=%s.%s&amp;Bundle-Version=%s' % tuple(parts[0:3])
            if len(parts[4]) == len(NON_OSGI_TAG):
                parts.pop() # pop off empty classifier
                if parts[3].lower() == 'jar':
                    parts.pop() # pop off default extension: jar
            else:
                parts[4] = parts[4][len(NON_OSGI_TAG):]
    return prefix + '/'.join(parts) + suffix

def generateFeatureFile(feature_repo_name,
                        features = [],
                        **kwargs):
    values = {
        'feature_repo_name' : '-'.join(feature_repo_name.split(':')[1:3]),
    }

    output = FEATURES_HEADER % values

    for feature in features:
        output += feature

    output += FEATURES_FOOTER
    return output

def generateFeature(feature_name,
                    version,
                    title,
                    features = [],
                    bundles = [],
                    **kwargs):
    values = {
        'feature_name' : feature_name,
        'version' : version,
        'title' : title,
    }

    output = FEATURE_HEADER % values

    if features:
        for feature in features:
            output += EXISTING_FEATURE % feature

    if bundles:
        for bundle in bundles:
            output += BUNDLE % mvnUrl(bundle)

    output += FEATURE_FOOTER
    return output


def generateAppFile(app_name,
                    origin,
                    version,
                    title,
                    category,
                    url,
                    feature_repo_name,
                    feature_name,
                    description = None,
                    apps = [],
                    artifacts = [],
                    security= None,
                    **kwargs):
    values = {
        'app_name' : app_name,
        'origin' : origin,
        'version' : version,
        'title' : title,
        'category' : category,
        'url' : url,
        'feature_repo_name' : mvnUrl(feature_repo_name) + '/xml/features',
        'feature_name' : feature_name,
    }

    values['description'] = description if description else title
    values['apps'] = ','.join(apps) if apps else ''

    output = APP_HEADER % values

    for artifact in artifacts:
        output += ARTIFACT % mvnUrl(artifact)

    if security is not None:
        output += SECURITY % security

    output += APP_FOOTER
    return output

def write(name, msg):
    if name is not None:
        with open(name, "w") as file:
            file.write(msg)
    else:
        print(msg)

if __name__ == '__main__':
    import sys, optparse

    parser = optparse.OptionParser()
    parser.add_option("-O", "--output",   dest="output",       help="Output file")
    parser.add_option("-n", "--name",     dest="feature_coords", help="Feature MVN Coords")
    parser.add_option("-a", "--app",      dest="app_name",     help="App Name")
    parser.add_option("-o", "--origin",   dest="origin",       help="Origin")
    parser.add_option("-c", "--category", dest="category",     help="Category")
    parser.add_option("-u", "--url",      dest="url",          help="URL")
    parser.add_option("-v", "--version",  dest="version",      help="Version")
    parser.add_option("-t", "--title",    dest="title",        help="Title")
    parser.add_option("-r", "--repo",     dest="repo_name",    help="Repo Name")
    parser.add_option('-D', '--desc',     dest='desc',         help='Application description')
    parser.add_option('-s', '--security', dest='security',     help='Application security')

    parser.add_option('-b', '--bundle',
                      action="append", dest='included_bundles',
                      metavar="BUNDLE", help='Included Bundle (multiple allowed)')
    parser.add_option('-e', '--excluded-bundle',
                      action="append", dest='excluded_bundles',
                      metavar="BUNDLE", help='Excluded Bundle (multiple allowed)')
    parser.add_option('-f', '--feature',
                      action="append", dest='features',
                      metavar="FEATURE", help='Existing Feature (multiple allowed)')
    parser.add_option('-d', '--apps',
                      action="append", dest='apps',
                      metavar="FEATURE", help='Required App (multiple allowed)')

    parser.add_option("-A", "--write-app", dest="write_app", action="store_true")
    parser.add_option("-F", "--write-features", dest="write_features", action="store_true")
    parser.add_option("-E", "--write-feature", dest="write_feature", action="store_true")

    (options, args) = parser.parse_args()

    values = {}
    if options.feature_coords and options.version and options.title:
        parts = options.feature_coords.split(':')
        values['feature_name'] = parts[2] if len(parts) > 2 else parts[0]
        values['version'] = options.version
        values['title'] = options.title
    else:
        sys.stderr.write('ERROR: Feature Name, Version, and Title are required\n')
        sys.stderr.flush()
        sys.exit(1)

    if options.app_name and options.origin and options.category and options.url:
        values['app_name'] = options.app_name
        values['origin'] = options.origin
        values['category'] = options.category
        values['url'] = options.url
    elif options.write_app:
        sys.stderr.write('ERROR: App Name, Origin, Category and URL are required\n')
        sys.stderr.flush()
        sys.exit(1)

    values['feature_repo_name'] = options.repo_name if options.repo_name \
                                    else options.feature_coords

    bundles = []
    if options.included_bundles:
        bundles += options.included_bundles
    if options.excluded_bundles:
        bundles += options.excluded_bundles
    if options.desc:
        values['description'] = options.desc

    feature = generateFeature(bundles=bundles,
                              features=options.features,
                              **values)

    if options.write_feature:
        write(options.output, feature)

    if options.write_features:
        write(options.output,
              generateFeatureFile(features=[ feature ], **values))

    if options.write_app:
        write(options.output,
              generateAppFile(artifacts=options.included_bundles,
                              apps=options.apps,
                              security=options.security,
                              **values))
