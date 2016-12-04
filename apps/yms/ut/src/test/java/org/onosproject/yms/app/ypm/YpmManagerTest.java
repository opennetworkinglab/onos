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

package org.onosproject.yms.app.ypm;

import org.junit.Test;
import org.onosproject.yangutils.datamodel.YangSchemaNodeIdentifier;
import org.onosproject.yms.ypm.DefaultYpmNode;
import org.onosproject.yms.ypm.YpmContext;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

/**
 * Unit tests for YpmManager class.
 */
public class YpmManagerTest {
    private final String logicalName = "logicalYpmNode";
    private final String moduleName1 = "portPairModule1";
    private final String moduleName2 = "portPairModule2";
    private final String xNodeName = "x";
    private final String yNodeName = "y";
    private final String zNodeName = "z";
    private final String x1NodeName = "x1";
    private final String x2NodeName = "x2";
    private final String y1NodeName = "y1";
    private final String y2NodeName = "y2";
    private final String y11NodeName = "y11";
    private final String z1NodeName = "z1";
    private final String z2NodeName = "z2";
    private final String z3NodeName = "z3";

    /**
     * Creates module1 ydt tree.
     */
    public YdtNodeAdapter createModule1Tree() throws CloneNotSupportedException {
        YangSchemaNodeIdentifier tmpNodeIdentifier;
        YdtNodeAdapter rootNode = new YdtNodeAdapter();
        tmpNodeIdentifier = new YangSchemaNodeIdentifier();
        tmpNodeIdentifier.setName(logicalName);
        rootNode.setNodeIdentifier(tmpNodeIdentifier);

        // Create module node with moduleName1
        YdtNodeAdapter moduleNode = new YdtNodeAdapter();
        tmpNodeIdentifier = new YangSchemaNodeIdentifier();
        tmpNodeIdentifier.setName(moduleName1);
        moduleNode.setNodeIdentifier(tmpNodeIdentifier);
        moduleNode.setParent(rootNode);
        rootNode.addChild(moduleNode); // child to logical node
        YdtNodeAdapter xNode = new YdtNodeAdapter();
        tmpNodeIdentifier = new YangSchemaNodeIdentifier();
        tmpNodeIdentifier.setName(xNodeName);
        xNode.setNodeIdentifier(tmpNodeIdentifier);
        YdtNodeAdapter yNode = new YdtNodeAdapter();
        tmpNodeIdentifier = new YangSchemaNodeIdentifier();
        tmpNodeIdentifier.setName(yNodeName);
        yNode.setNodeIdentifier(tmpNodeIdentifier);
        YdtNodeAdapter zNode = new YdtNodeAdapter();
        tmpNodeIdentifier = new YangSchemaNodeIdentifier();
        tmpNodeIdentifier.setName(zNodeName);
        zNode.setNodeIdentifier(tmpNodeIdentifier);
        moduleNode.addChild(xNode); // child to module node
        xNode.addSibling(yNode);
        yNode.addSibling(zNode);
        YdtNodeAdapter x1Node = new YdtNodeAdapter();
        tmpNodeIdentifier = new YangSchemaNodeIdentifier();
        tmpNodeIdentifier.setName(x1NodeName);
        x1Node.setNodeIdentifier(tmpNodeIdentifier);
        YdtNodeAdapter x2Node = new YdtNodeAdapter();
        tmpNodeIdentifier = new YangSchemaNodeIdentifier();
        tmpNodeIdentifier.setName(x2NodeName);
        x2Node.setNodeIdentifier(tmpNodeIdentifier);
        YdtNodeAdapter y1Node = new YdtNodeAdapter();
        tmpNodeIdentifier = new YangSchemaNodeIdentifier();
        tmpNodeIdentifier.setName(y1NodeName);
        y1Node.setNodeIdentifier(tmpNodeIdentifier);
        YdtNodeAdapter y2Node = new YdtNodeAdapter();
        tmpNodeIdentifier = new YangSchemaNodeIdentifier();
        tmpNodeIdentifier.setName(y2NodeName);
        y2Node.setNodeIdentifier(tmpNodeIdentifier);
        YdtNodeAdapter z1Node = new YdtNodeAdapter();
        tmpNodeIdentifier = new YangSchemaNodeIdentifier();
        tmpNodeIdentifier.setName(z1NodeName);
        z1Node.setNodeIdentifier(tmpNodeIdentifier);
        YdtNodeAdapter z2Node = new YdtNodeAdapter();
        tmpNodeIdentifier = new YangSchemaNodeIdentifier();
        tmpNodeIdentifier.setName(z2NodeName);
        z2Node.setNodeIdentifier(tmpNodeIdentifier);
        xNode.addChild(x1Node);
        x1Node.addSibling(x2Node);
        yNode.addChild(y1Node);
        y1Node.addSibling(y2Node);
        zNode.addChild(z1Node);
        z1Node.addSibling(z2Node);

        return rootNode;
    }

    /**
     * Checks the protocol data added in YpmManger when only single module exists.
     */
    @Test
    public void retrieveAndCheckProtocolDataWhenSingleModule() throws CloneNotSupportedException {
        YdtNodeAdapter rootNode = createModule1Tree();

        YpmManager ypmManager = new YpmManager();
        Object metaData = 10;
        ypmManager.setProtocolData(rootNode, metaData);
        YpmContext ypmContext = ypmManager.getProtocolData(rootNode);
        DefaultYpmNode rootYpmNode = (DefaultYpmNode) ypmContext;
        assertThat(rootYpmNode.getName(), is(logicalName));
        DefaultYpmNode currYpmNode = (DefaultYpmNode) rootYpmNode.getFirstChild();
        assertThat(currYpmNode.getName(), is(moduleName1));
        currYpmNode = (DefaultYpmNode) currYpmNode.getFirstChild();
        assertThat(currYpmNode.getName(), is(xNodeName));  // x node
        assertThat(currYpmNode.getMetaData(), is(metaData));
        assertThat(currYpmNode.getNextSibling(), notNullValue()); // y or z node
        currYpmNode = currYpmNode.getNextSibling();
        assertThat(currYpmNode.getMetaData(), is(metaData));
        assertThat(currYpmNode.getNextSibling(), notNullValue()); // y or z node
        currYpmNode = currYpmNode.getNextSibling();
        assertThat(currYpmNode.getMetaData(), is(metaData));
        assertThat(currYpmNode.getNextSibling(), nullValue());  // no sibling

        // Check x node leaf's x1 and x2
        DefaultYpmNode moduleYpmNode = currYpmNode.getParent();
        assertThat(moduleYpmNode.getName(), is(moduleName1));
        DefaultYpmNode xYpmNode = (DefaultYpmNode) moduleYpmNode.getFirstChild();
        assertThat(xYpmNode.getName(), is(xNodeName));
        assertThat(xYpmNode.getMetaData(), is(metaData));
        // Check x1 node
        currYpmNode = (DefaultYpmNode) xYpmNode.getFirstChild();
        assertThat(currYpmNode.getName(), is(x1NodeName));
        assertThat(currYpmNode.getMetaData(), is(metaData));
        // Check x2 node
        currYpmNode = currYpmNode.getNextSibling();
        assertThat(currYpmNode.getName(), is(x2NodeName));
        assertThat(currYpmNode.getMetaData(), is(metaData));

        // Check y node leaf's y1 and y2
        DefaultYpmNode yYpmNode = xYpmNode.getNextSibling();
        assertThat(yYpmNode.getName(), is(yNodeName));
        assertThat(yYpmNode.getMetaData(), is(metaData));
        // Check y1 node
        currYpmNode = (DefaultYpmNode) yYpmNode.getFirstChild();
        assertThat(currYpmNode.getName(), is(y1NodeName));
        assertThat(currYpmNode.getMetaData(), is(metaData));
        // Check y2 node
        currYpmNode = currYpmNode.getNextSibling();
        assertThat(currYpmNode.getName(), is(y2NodeName));
        assertThat(currYpmNode.getMetaData(), is(metaData));

        // Check z node leaf's z1 and z2
        DefaultYpmNode zYpmNode = yYpmNode.getNextSibling();
        assertThat(zYpmNode.getName(), is(zNodeName));
        assertThat(zYpmNode.getMetaData(), is(metaData));
        // Check z1 node
        currYpmNode = (DefaultYpmNode) zYpmNode.getFirstChild();
        assertThat(currYpmNode.getName(), is(z1NodeName));
        assertThat(currYpmNode.getMetaData(), is(metaData));
        // Check z2 node
        currYpmNode = currYpmNode.getNextSibling();
        assertThat(currYpmNode.getName(), is(z2NodeName));
        assertThat(currYpmNode.getMetaData(), is(metaData));
    }

    /**
     * Creates module2 ydt tree. Module1 and Module2 trees are point to same logical root.
     */
    public YdtNodeAdapter createModule2Tree() throws CloneNotSupportedException {
        YangSchemaNodeIdentifier tmpNodeIdentifier;
        YdtNodeAdapter rootNode = createModule1Tree();
        YdtNodeAdapter moduleNode = new YdtNodeAdapter();
        tmpNodeIdentifier = new YangSchemaNodeIdentifier();
        tmpNodeIdentifier.setName(moduleName2);
        moduleNode.setNodeIdentifier(tmpNodeIdentifier);
        moduleNode.setParent(rootNode);
        rootNode.addChild(moduleNode); // child to logical node
        YdtNodeAdapter xNode = new YdtNodeAdapter();
        tmpNodeIdentifier = new YangSchemaNodeIdentifier();
        tmpNodeIdentifier.setName(xNodeName);
        xNode.setNodeIdentifier(tmpNodeIdentifier);
        YdtNodeAdapter yNode = new YdtNodeAdapter();
        tmpNodeIdentifier = new YangSchemaNodeIdentifier();
        tmpNodeIdentifier.setName(yNodeName);
        yNode.setNodeIdentifier(tmpNodeIdentifier);
        YdtNodeAdapter zNode = new YdtNodeAdapter();
        tmpNodeIdentifier = new YangSchemaNodeIdentifier();
        tmpNodeIdentifier.setName(zNodeName);
        zNode.setNodeIdentifier(tmpNodeIdentifier);
        moduleNode.addChild(xNode); // child to module node
        xNode.addSibling(yNode);
        yNode.addSibling(zNode);
        YdtNodeAdapter x1Node = new YdtNodeAdapter();
        tmpNodeIdentifier = new YangSchemaNodeIdentifier();
        tmpNodeIdentifier.setName(x1NodeName);
        x1Node.setNodeIdentifier(tmpNodeIdentifier);
        YdtNodeAdapter x2Node = new YdtNodeAdapter();
        tmpNodeIdentifier = new YangSchemaNodeIdentifier();
        tmpNodeIdentifier.setName(x2NodeName);
        x2Node.setNodeIdentifier(tmpNodeIdentifier);
        YdtNodeAdapter y1Node = new YdtNodeAdapter();
        tmpNodeIdentifier = new YangSchemaNodeIdentifier();
        tmpNodeIdentifier.setName(y1NodeName);
        y1Node.setNodeIdentifier(tmpNodeIdentifier);
        YdtNodeAdapter y2Node = new YdtNodeAdapter();
        tmpNodeIdentifier = new YangSchemaNodeIdentifier();
        tmpNodeIdentifier.setName(y2NodeName);
        y2Node.setNodeIdentifier(tmpNodeIdentifier);
        YdtNodeAdapter z1Node = new YdtNodeAdapter();
        tmpNodeIdentifier = new YangSchemaNodeIdentifier();
        tmpNodeIdentifier.setName(z1NodeName);
        z1Node.setNodeIdentifier(tmpNodeIdentifier);
        YdtNodeAdapter z2Node = new YdtNodeAdapter();
        tmpNodeIdentifier = new YangSchemaNodeIdentifier();
        tmpNodeIdentifier.setName(z2NodeName);
        z2Node.setNodeIdentifier(tmpNodeIdentifier);
        xNode.addChild(x1Node);
        x1Node.addSibling(x2Node);
        yNode.addChild(y1Node);
        y1Node.addSibling(y2Node);
        zNode.addChild(z1Node);
        z1Node.addSibling(z2Node);

        return rootNode;
    }

    /**
     * Checks the protocol data added in YpmManger when multiple modules exists.
     */
    @Test
    public void retrieveAndCheckProtocolDataWhenMultipleModule() throws CloneNotSupportedException {
        YdtNodeAdapter rootNode = createModule2Tree();

        YpmManager ypmManager = new YpmManager();
        Object metaData = 10;
        ypmManager.setProtocolData(rootNode, metaData);
        YpmContext ypmContext = ypmManager.getProtocolData(rootNode);
        DefaultYpmNode rootYpmNode = (DefaultYpmNode) ypmContext;
        assertThat(rootYpmNode.getName(), is(logicalName));
        DefaultYpmNode currYpmNode = (DefaultYpmNode) rootYpmNode.getFirstChild();
        currYpmNode = currYpmNode.getNextSibling(); // jump to next module (module2)
        assertThat(currYpmNode.getName(), is(moduleName2));
        currYpmNode = (DefaultYpmNode) currYpmNode.getFirstChild();
        assertThat(currYpmNode.getName(), is(xNodeName));  // x node
        assertThat(currYpmNode.getMetaData(), is(metaData));
        assertThat(currYpmNode.getNextSibling(), notNullValue()); // y or z node
        currYpmNode = currYpmNode.getNextSibling();
        assertThat(currYpmNode.getMetaData(), is(metaData));
        assertThat(currYpmNode.getNextSibling(), notNullValue()); // y or z node
        currYpmNode = currYpmNode.getNextSibling();
        assertThat(currYpmNode.getMetaData(), is(metaData));
        assertThat(currYpmNode.getNextSibling(), nullValue());  // no sibling

        // Check x node leaf's x1 and x2
        DefaultYpmNode moduleYpmNode = currYpmNode.getParent();
        assertThat(moduleYpmNode.getName(), is(moduleName2));
        DefaultYpmNode xYpmNode = (DefaultYpmNode) moduleYpmNode.getFirstChild();
        assertThat(xYpmNode.getName(), is(xNodeName));
        assertThat(xYpmNode.getMetaData(), is(metaData));
        // Check x1 node
        currYpmNode = (DefaultYpmNode) xYpmNode.getFirstChild();
        assertThat(currYpmNode.getName(), is(x1NodeName));
        assertThat(currYpmNode.getMetaData(), is(metaData));
        // Check x2 node
        currYpmNode = currYpmNode.getNextSibling();
        assertThat(currYpmNode.getName(), is(x2NodeName));
        assertThat(currYpmNode.getMetaData(), is(metaData));

        // Check y node leaf's y1 and y2
        DefaultYpmNode yYpmNode = xYpmNode.getNextSibling();
        assertThat(yYpmNode.getName(), is(yNodeName));
        assertThat(yYpmNode.getMetaData(), is(metaData));
        // Check y1 node
        currYpmNode = (DefaultYpmNode) yYpmNode.getFirstChild();
        assertThat(currYpmNode.getName(), is(y1NodeName));
        assertThat(currYpmNode.getMetaData(), is(metaData));
        // Check y2 node
        currYpmNode = currYpmNode.getNextSibling();
        assertThat(currYpmNode.getName(), is(y2NodeName));
        assertThat(currYpmNode.getMetaData(), is(metaData));

        // Check z node leaf's z1 and z2
        DefaultYpmNode zYpmNode = yYpmNode.getNextSibling();
        assertThat(zYpmNode.getName(), is(zNodeName));
        assertThat(zYpmNode.getMetaData(), is(metaData));
        // Check z1 node
        currYpmNode = (DefaultYpmNode) zYpmNode.getFirstChild();
        assertThat(currYpmNode.getName(), is(z1NodeName));
        assertThat(currYpmNode.getMetaData(), is(metaData));
        // Check z2 node
        currYpmNode = currYpmNode.getNextSibling();
        assertThat(currYpmNode.getName(), is(z2NodeName));
        assertThat(currYpmNode.getMetaData(), is(metaData));
    }

    /**
     * Checks the protocol data added in YpmManger, but tests only part of module1 tree.
     */
    @Test
    public void retrieveAndCheckProtocolDataChosenFromPartOfModule1Tree() throws CloneNotSupportedException {
        YangSchemaNodeIdentifier tmpNodeIdentifier;
        YdtNodeAdapter rootNode = createModule2Tree();

        // Sets the tree
        YpmManager ypmManager = new YpmManager();
        Object metaData = 10;
        ypmManager.setProtocolData(rootNode, metaData);

        // Create new ydt tree part of module1 tree
        YdtNodeAdapter rootNewYdtNode = new YdtNodeAdapter();
        // Create module node with moduleName1
        tmpNodeIdentifier = new YangSchemaNodeIdentifier();
        tmpNodeIdentifier.setName(logicalName);
        rootNewYdtNode.setNodeIdentifier(tmpNodeIdentifier);
        YdtNodeAdapter moduleNode = new YdtNodeAdapter();
        tmpNodeIdentifier = new YangSchemaNodeIdentifier();
        tmpNodeIdentifier.setName(moduleName1);
        moduleNode.setNodeIdentifier(tmpNodeIdentifier);
        moduleNode.setParent(rootNewYdtNode);
        rootNewYdtNode.addChild(moduleNode); // child to logical node
        YdtNodeAdapter yNode = new YdtNodeAdapter();
        tmpNodeIdentifier = new YangSchemaNodeIdentifier();
        tmpNodeIdentifier.setName(yNodeName);
        yNode.setNodeIdentifier(tmpNodeIdentifier);
        YdtNodeAdapter zNode = new YdtNodeAdapter();
        tmpNodeIdentifier = new YangSchemaNodeIdentifier();
        tmpNodeIdentifier.setName(zNodeName);
        zNode.setNodeIdentifier(tmpNodeIdentifier);
        moduleNode.addChild(yNode); // child to module node
        yNode.addSibling(zNode);
        YdtNodeAdapter y1Node = new YdtNodeAdapter();
        tmpNodeIdentifier = new YangSchemaNodeIdentifier();
        tmpNodeIdentifier.setName(y1NodeName);
        y1Node.setNodeIdentifier(tmpNodeIdentifier);
        YdtNodeAdapter y2Node = new YdtNodeAdapter();
        tmpNodeIdentifier = new YangSchemaNodeIdentifier();
        tmpNodeIdentifier.setName(y2NodeName);
        y2Node.setNodeIdentifier(tmpNodeIdentifier);
        yNode.addChild(y1Node);
        y1Node.addSibling(y2Node);

        // Again sets the protocol data
        metaData = 20;
        ypmManager.setProtocolData(rootNewYdtNode, metaData);

        // Retrieve protocol data and check the contents
        YpmContext ypmContext = ypmManager.getProtocolData(rootNewYdtNode);
        DefaultYpmNode rootYpmNode = (DefaultYpmNode) ypmContext;
        assertThat(rootYpmNode.getName(), is(logicalName));
        DefaultYpmNode currYpmNode = (DefaultYpmNode) rootYpmNode.getFirstChild();
        assertThat(currYpmNode.getName(), is(moduleName1));
        // Check y and z node
        currYpmNode = (DefaultYpmNode) currYpmNode.getFirstChild();
        DefaultYpmNode yYpmNode = currYpmNode;
        assertThat(currYpmNode.getName(), is(yNodeName));  // x node
        assertThat(currYpmNode.getMetaData(), is(metaData));
        assertThat(currYpmNode.getNextSibling(), notNullValue()); // z node
        currYpmNode = currYpmNode.getNextSibling();
        assertThat(currYpmNode.getName(), is(zNodeName));
        assertThat(currYpmNode.getMetaData(), is(metaData));
        assertThat(currYpmNode.getNextSibling(), nullValue());  // no sibling
        // Check y1 and y2 node
        currYpmNode = (DefaultYpmNode) yYpmNode.getFirstChild();
        assertThat(currYpmNode.getName(), is(y1NodeName));
        assertThat(currYpmNode.getMetaData(), is(metaData));
        assertThat(currYpmNode.getNextSibling(), notNullValue()); // y2 should exists
        currYpmNode = currYpmNode.getNextSibling();
        assertThat(currYpmNode.getName(), is(y2NodeName));
        assertThat(currYpmNode.getMetaData(), is(metaData));
        assertThat(currYpmNode.getNextSibling(), nullValue());
    }

    /**
     * Checks the protocol data added in YpmManger, but tests part of module1 tree with little bit extended tree.
     */
    @Test
    public void retrieveAndCheckProtocolDataChosenFromPartOfModule1TreeWithExtended()
            throws CloneNotSupportedException {
        YangSchemaNodeIdentifier tmpNodeIdentifier;
        YdtNodeAdapter rootNode = createModule2Tree();

        // Sets the tree
        YpmManager ypmManager = new YpmManager();
        Object metaData = 10;
        ypmManager.setProtocolData(rootNode, metaData);

        // Create new ydt tree part of module1 tree
        YdtNodeAdapter rootNewYdtNode = new YdtNodeAdapter();
        // Create module node with moduleName1
        tmpNodeIdentifier = new YangSchemaNodeIdentifier();
        tmpNodeIdentifier.setName(logicalName);
        rootNewYdtNode.setNodeIdentifier(tmpNodeIdentifier);
        YdtNodeAdapter moduleNode = new YdtNodeAdapter();
        tmpNodeIdentifier = new YangSchemaNodeIdentifier();
        tmpNodeIdentifier.setName(moduleName1);
        moduleNode.setNodeIdentifier(tmpNodeIdentifier);
        moduleNode.setParent(rootNewYdtNode);
        rootNewYdtNode.addChild(moduleNode); // child to logical node
        YdtNodeAdapter yNode = new YdtNodeAdapter();
        tmpNodeIdentifier = new YangSchemaNodeIdentifier();
        tmpNodeIdentifier.setName(yNodeName);
        yNode.setNodeIdentifier(tmpNodeIdentifier);
        YdtNodeAdapter zNode = new YdtNodeAdapter();
        tmpNodeIdentifier = new YangSchemaNodeIdentifier();
        tmpNodeIdentifier.setName(zNodeName);
        zNode.setNodeIdentifier(tmpNodeIdentifier);
        moduleNode.addChild(yNode); // child to module node
        yNode.addSibling(zNode);
        YdtNodeAdapter y1Node = new YdtNodeAdapter();
        tmpNodeIdentifier = new YangSchemaNodeIdentifier();
        tmpNodeIdentifier.setName(y1NodeName);
        y1Node.setNodeIdentifier(tmpNodeIdentifier);
        YdtNodeAdapter y2Node = new YdtNodeAdapter();
        tmpNodeIdentifier = new YangSchemaNodeIdentifier();
        tmpNodeIdentifier.setName(y2NodeName);
        y2Node.setNodeIdentifier(tmpNodeIdentifier);
        yNode.addChild(y1Node);
        y1Node.addSibling(y2Node);
        YdtNodeAdapter y11Node = new YdtNodeAdapter();
        // Add new y11 node
        tmpNodeIdentifier = new YangSchemaNodeIdentifier();
        tmpNodeIdentifier.setName(y11NodeName);
        y11Node.setNodeIdentifier(tmpNodeIdentifier);
        y1Node.addChild(y11Node);
        // Add new y3 node
        YdtNodeAdapter z3Node = new YdtNodeAdapter();
        tmpNodeIdentifier = new YangSchemaNodeIdentifier();
        tmpNodeIdentifier.setName(z3NodeName);
        z3Node.setNodeIdentifier(tmpNodeIdentifier);
        zNode.addChild(z3Node);

        // Again sets the protocol data
        metaData = 20;
        ypmManager.setProtocolData(rootNewYdtNode, metaData);

        // Retrieve protocol data and check the contents
        YpmContext ypmContext = ypmManager.getProtocolData(rootNewYdtNode);
        DefaultYpmNode rootYpmNode = (DefaultYpmNode) ypmContext;
        assertThat(rootYpmNode.getName(), is(logicalName));
        DefaultYpmNode currYpmNode = (DefaultYpmNode) rootYpmNode.getFirstChild();
        assertThat(currYpmNode.getName(), is(moduleName1));
        // Check y and z node
        currYpmNode = (DefaultYpmNode) currYpmNode.getFirstChild();
        DefaultYpmNode yYpmNode = currYpmNode;
        assertThat(currYpmNode.getName(), is(yNodeName));  // y node
        assertThat(currYpmNode.getMetaData(), is(metaData));
        assertThat(currYpmNode.getNextSibling(), notNullValue()); // z node
        currYpmNode = currYpmNode.getNextSibling();
        DefaultYpmNode zYpmNode = currYpmNode;
        assertThat(currYpmNode.getName(), is(zNodeName));
        assertThat(currYpmNode.getMetaData(), is(metaData));
        assertThat(currYpmNode.getNextSibling(), nullValue());  // no sibling
        // Check y1 and y2 node
        currYpmNode = (DefaultYpmNode) yYpmNode.getFirstChild();
        DefaultYpmNode y1YpmNode = currYpmNode;
        assertThat(currYpmNode.getName(), is(y1NodeName));
        assertThat(currYpmNode.getMetaData(), is(metaData));
        assertThat(currYpmNode.getNextSibling(), notNullValue()); // y2 should exists
        currYpmNode = currYpmNode.getNextSibling();
        assertThat(currYpmNode.getName(), is(y2NodeName));
        assertThat(currYpmNode.getMetaData(), is(metaData));
        assertThat(currYpmNode.getNextSibling(), nullValue());
        // Check new y11 node
        currYpmNode = (DefaultYpmNode) y1YpmNode.getFirstChild();
        assertThat(currYpmNode.getName(), is(y11NodeName));
        assertThat(currYpmNode.getMetaData(), is(metaData));
        assertThat(currYpmNode.getNextSibling(), nullValue());
        assertThat(currYpmNode.getFirstChild(), nullValue());
        // Check new z3 node
        currYpmNode = (DefaultYpmNode) zYpmNode.getFirstChild();
        assertThat(currYpmNode.getName(), is(z3NodeName));
        assertThat(currYpmNode.getMetaData(), is(metaData));
        assertThat(currYpmNode.getNextSibling(), nullValue());
        assertThat(currYpmNode.getFirstChild(), nullValue());
    }
}
