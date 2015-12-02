#!/usr/bin/env python
'''
  Builds synthetic feature file that includes all core, provider, and application
  features, so that we can pre-stage all bundles required to run ONOS off-line.
'''

import os
import xml.etree.ElementTree as ET

FEATURE_TAG = '{http://karaf.apache.org/xmlns/features/v1.2.0}feature'
STAGED_REPOS = 'target/staged-repos.xml'

if 'ONOS_ROOT' in os.environ:
    ONOS_ROOT = os.environ['ONOS_ROOT']
else:
    # fallback to working directory if ONOS_ROOT is not set
    ONOS_ROOT = os.getcwd()

def findFeatureFiles(path=ONOS_ROOT):
    #only descend into target directories that have pom
    for root, dirs, files in os.walk(path):
        if 'pom.xml' not in files:
            if 'target' in dirs:
                #pruning target dir with no pom.xml
                dirs.remove('target')
        if '/target' in root:
            if '/classes/' in root:
                #filter out features.xml for maven-plugin
                continue
            for f in files:
                if f.endswith('features.xml'):
                    yield os.path.join(root, f)

def featuresFromFile(file):
    features = []
    tree = ET.parse(file)
    root = tree.getroot()
    for feature in root.findall(FEATURE_TAG):
        features.append(feature.attrib['name'])
    return features

if __name__ == '__main__':
    outputTree = ET.Element('features')
    uberFeature = ET.Element('feature', attrib={'name' : 'onos-uber-synthetic'})
    for file in findFeatureFiles():
        features = featuresFromFile(file)
        if len(features) > 0:
            ET.SubElement(outputTree, 'repository').text = 'file:%s' % file
            for feature in features:
                ET.SubElement(uberFeature, 'feature').text = feature
    outputTree.append(uberFeature)

    outputFile = os.path.join(os.path.dirname(os.path.realpath(__file__)), STAGED_REPOS)
    outputDir = os.path.dirname(outputFile)
    if not os.path.exists(outputDir):
        os.mkdir(outputDir)
    ET.ElementTree(outputTree).write(outputFile)

    import sys
    if '-d' in sys.argv:
        # -------- TODO for debug only --------
        def indent(elem, level=0):
            #function borrowed from: http://effbot.org/zone/element-lib.htm#prettyprint
            i = "\n" + level*"  "
            if len(elem):
                if not elem.text or not elem.text.strip():
                    elem.text = i + "  "
                if not elem.tail or not elem.tail.strip():
                    elem.tail = i
                for elem in elem:
                    indent(elem, level+1)
                if not elem.tail or not elem.tail.strip():
                    elem.tail = i
            else:
                if level and (not elem.tail or not elem.tail.strip()):
                    elem.tail = i

        print 'Writing to file:', outputFile
        indent(outputTree)
        ET.dump(outputTree)
