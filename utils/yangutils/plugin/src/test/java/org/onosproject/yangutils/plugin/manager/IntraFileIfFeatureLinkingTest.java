/*
 * Copyright 2016-present Open Networking Laboratory
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onosproject.yangutils.plugin.manager;

import org.junit.Test;
import org.onosproject.yangutils.datamodel.YangContainer;
import org.onosproject.yangutils.datamodel.YangFeature;
import org.onosproject.yangutils.datamodel.YangIfFeature;
import org.onosproject.yangutils.datamodel.YangLeaf;
import org.onosproject.yangutils.datamodel.YangModule;
import org.onosproject.yangutils.datamodel.YangNode;
import org.onosproject.yangutils.datamodel.YangNodeType;
import org.onosproject.yangutils.datamodel.YangSubModule;
import org.onosproject.yangutils.datamodel.utils.ResolvableStatus;
import org.onosproject.yangutils.parser.exceptions.ParserException;
import org.onosproject.yangutils.parser.impl.YangUtilsParserManager;

import java.io.IOException;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

/**
 * Test cases for testing if-feature intra file linking.
 */
public class IntraFileIfFeatureLinkingTest {

    private final YangUtilsParserManager manager = new YangUtilsParserManager();

    /**
     * Checks self resolution when feature defined in same file.
     */
    @Test
    public void processSelfFileLinkingWithFeature()
            throws IOException, ParserException {

        YangNode node = manager
                .getDataModel("src/test/resources/SelfFileLinkingWithFeature.yang");

        // Check whether the data model tree returned is of type module.
        assertThat((node instanceof YangModule), is(true));

        // Check whether the node type is set properly to module.
        assertThat(node.getNodeType(), is(YangNodeType.MODULE_NODE));

        // Check whether the module name is set correctly.
        YangModule yangNode = (YangModule) node;
        assertThat(yangNode.getName(), is("syslog"));

        List<YangFeature> featureList = yangNode.getFeatureList();
        YangFeature feature = featureList.iterator().next();
        assertThat(feature.getName(), is("local-storage"));

        YangContainer container = (YangContainer) yangNode.getChild();
        assertThat(container.getName(), is("speed"));

        List<YangLeaf> listOfLeaf = container.getListOfLeaf();
        YangLeaf leaf = listOfLeaf.iterator().next();
        assertThat(leaf.getName(), is("local-storage-limit"));

        List<YangIfFeature> ifFeatureList = leaf.getIfFeatureList();
        YangIfFeature ifFeature = ifFeatureList.iterator().next();
        assertThat(ifFeature.getName().getName(), is("local-storage"));
        assertThat(ifFeature.getResolvableStatus(), is(ResolvableStatus.RESOLVED));
    }

    /**
     * Checks self resolution when feature is undefined.
     */
    @Test
    public void processSelfFileLinkingWithFeatureUndefined()
            throws IOException, ParserException {

        YangNode node = manager
                .getDataModel("src/test/resources/SelfFileLinkingWithFeatureUndefined.yang");

        // Check whether the data model tree returned is of type module.
        assertThat((node instanceof YangModule), is(true));

        // Check whether the node type is set properly to module.
        assertThat(node.getNodeType(), is(YangNodeType.MODULE_NODE));

        // Check whether the module name is set correctly.
        YangModule yangNode = (YangModule) node;
        assertThat(yangNode.getName(), is("syslog"));

        YangContainer container = (YangContainer) yangNode.getChild();
        assertThat(container.getName(), is("speed"));

        List<YangLeaf> listOfLeaf = container.getListOfLeaf();
        YangLeaf leaf = listOfLeaf.iterator().next();
        assertThat(leaf.getName(), is("local-storage-limit"));

        List<YangIfFeature> ifFeatureList = leaf.getIfFeatureList();
        YangIfFeature ifFeature = ifFeatureList.iterator().next();
        assertThat(ifFeature.getName().getName(), is("local-storage"));
        assertThat(ifFeature.getResolvableStatus(), is(ResolvableStatus.INTRA_FILE_RESOLVED));
    }

    /**
     * Checks self resolution of feature with multiple dependency.
     */
    @Test
    public void processSelfFileLinkingWithMultipleDependency() throws IOException, ParserException {
        YangNode node = manager
                .getDataModel("src/test/resources/SelfFileLinkingWithMultipleDependency.yang");

        // Check whether the data model tree returned is of type module.
        assertThat((node instanceof YangModule), is(true));

        // Check whether the node type is set properly to module.
        assertThat(node.getNodeType(), is(YangNodeType.MODULE_NODE));

        // Check whether the module name is set correctly.
        YangModule yangNode = (YangModule) node;
        assertThat(yangNode.getName(), is("syslog"));

        List<YangFeature> featureList = yangNode.getFeatureList();
        YangFeature feature = featureList.iterator().next();
        assertThat(feature.getName(), is("p2mp-te"));

        YangContainer container = (YangContainer) yangNode.getChild();
        assertThat(container.getName(), is("speed"));

        List<YangLeaf> listOfLeaf = container.getListOfLeaf();
        YangLeaf leaf = listOfLeaf.iterator().next();
        assertThat(leaf.getName(), is("local-storage-limit"));

        List<YangIfFeature> ifFeatureList = leaf.getIfFeatureList();
        YangIfFeature ifFeature = ifFeatureList.iterator().next();
        assertThat(ifFeature.getName().getName(), is("frr-te"));
        assertThat(ifFeature.getResolvableStatus(), is(ResolvableStatus.RESOLVED));
    }

    /**
     * Checks self resolution of feature with multiple dependency undefined.
     */
    @Test
    public void processSelfFileLinkingWithMultipleDependencyUnresolved() throws IOException, ParserException {
        YangNode node = manager
                .getDataModel("src/test/resources/SelfFileLinkingWithMultipleDependencyUnresolved.yang");

        // Check whether the data model tree returned is of type module.
        assertThat((node instanceof YangModule), is(true));

        // Check whether the node type is set properly to module.
        assertThat(node.getNodeType(), is(YangNodeType.MODULE_NODE));

        // Check whether the module name is set correctly.
        YangModule yangNode = (YangModule) node;
        assertThat(yangNode.getName(), is("syslog"));

        List<YangFeature> featureList = yangNode.getFeatureList();
        YangFeature feature = featureList.iterator().next();
        assertThat(feature.getName(), is("frr-te"));

        YangContainer container = (YangContainer) yangNode.getChild();
        assertThat(container.getName(), is("speed"));

        List<YangLeaf> listOfLeaf = container.getListOfLeaf();
        YangLeaf leaf = listOfLeaf.iterator().next();
        assertThat(leaf.getName(), is("local-storage-limit"));

        List<YangIfFeature> ifFeatureList = leaf.getIfFeatureList();
        YangIfFeature ifFeature = ifFeatureList.iterator().next();
        assertThat(ifFeature.getName().getName(), is("frr-te"));
        assertThat(ifFeature.getResolvableStatus(), is(ResolvableStatus.INTRA_FILE_RESOLVED));
    }

    /**
     * Checks self resolution when feature is defined in same file in submodule.
     */
    @Test
    public void processSelfFileLinkingWithFeatureInSubModule()
            throws IOException, ParserException {

        YangNode node = manager
                .getDataModel("src/test/resources/SelfFileLinkingWithFeatureInSubModule.yang");

        // Check whether the data model tree returned is of type module.
        assertThat((node instanceof YangSubModule), is(true));

        // Check whether the node type is set properly to module.
        assertThat(node.getNodeType(), is(YangNodeType.SUB_MODULE_NODE));

        // Check whether the module name is set correctly.
        YangSubModule yangNode = (YangSubModule) node;
        assertThat(yangNode.getName(), is("syslog"));

        List<YangFeature> featureList = yangNode.getFeatureList();
        YangFeature feature = featureList.iterator().next();
        assertThat(feature.getName(), is("local-storage"));

        YangContainer container = (YangContainer) yangNode.getChild();
        assertThat(container.getName(), is("speed"));

        List<YangLeaf> listOfLeaf = container.getListOfLeaf();
        YangLeaf leaf = listOfLeaf.iterator().next();
        assertThat(leaf.getName(), is("local-storage-limit"));

        List<YangIfFeature> ifFeatureList = leaf.getIfFeatureList();
        YangIfFeature ifFeature = ifFeatureList.iterator().next();
        assertThat(ifFeature.getName().getName(), is("local-storage"));
        assertThat(ifFeature.getResolvableStatus(), is(ResolvableStatus.RESOLVED));
    }
}
