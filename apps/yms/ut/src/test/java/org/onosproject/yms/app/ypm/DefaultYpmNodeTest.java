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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import org.onosproject.yms.ypm.YpmContext;
import org.onosproject.yms.ypm.DefaultYpmNode;

/**
 * Unit tests for DefaultYpmNode class.
 */
public class DefaultYpmNodeTest {
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
    private final String z1NodeName = "z1";
    private final String z2NodeName = "z2";

    /**
     * Constructs ypm tree with single module.
     *
     * @return ypm tree root node
     */
    private YpmContext constructYpmTreeSingleModule() {
        // Create logical node
        DefaultYpmNode rootNode = new DefaultYpmNode(logicalName);
        // Create module node with moduleName1
        rootNode.addChild(moduleName1); // child to logical node
        YpmContext moduleNode = rootNode.getChild(moduleName1);
        moduleNode.addChild(xNodeName); // child to module node
        moduleNode.addChild(yNodeName); // sibling node to child node "x"
        YpmContext xNode = moduleNode.getChild("x");
        xNode.addSibling(zNodeName); // sibling node to child node "x"
        xNode.addChild(x1NodeName); // child to node x
        xNode.addChild(x2NodeName); // child to node x
        YpmContext yNode = moduleNode.getChild(yNodeName);
        yNode.addChild(y1NodeName); // child to node y
        yNode.addChild(y2NodeName); // child to node y
        YpmContext zNode = moduleNode.getChild(zNodeName);
        zNode.addChild(z1NodeName); // child to node z
        zNode.addChild(z2NodeName); // child to node z
        return rootNode;
    }

    /**
     * Constructs ypm tree with multi module.
     *
     * @return ypm tree root node
     */
    private YpmContext constructYpmTreeMultiModule(DefaultYpmNode rootNode) {
        rootNode.addChild(moduleName2); // child to logical node
        YpmContext moduleNode = rootNode.getChild(moduleName2);
        moduleNode.addChild(xNodeName); // child to module node
        moduleNode.addChild(yNodeName); // sibling node to child node "x"
        YpmContext xNode = moduleNode.getChild("x");
        xNode.addSibling(zNodeName); // sibling node to child node "x"
        xNode.addChild(x1NodeName); // child to node x
        xNode.addChild(x2NodeName); // child to node x
        YpmContext yNode = moduleNode.getChild(yNodeName);
        yNode.addChild(y1NodeName); // child to node y
        yNode.addChild(y2NodeName); // child to node y
        YpmContext zNode = moduleNode.getChild(zNodeName);
        zNode.addChild(z1NodeName); // child to node z
        zNode.addChild(z2NodeName); // child to node z
        return rootNode;
    }

    /**
     * Checks ypm tree single module construction.
     */
    @Test
    public void testYpmTreeSingleModuleConstruction() {
        DefaultYpmNode rootNode = (DefaultYpmNode) constructYpmTreeSingleModule();
        // Check one by one node
        String name = rootNode.getName();
        assertThat(name, is(logicalName));
        YpmContext moduleNode = rootNode.getChild(moduleName1);
        assertThat(moduleNode.getName(), is(moduleName1));
        YpmContext ypmNode = moduleNode.getChild(xNodeName);
        assertThat(ypmNode.getName(), is(xNodeName));
        // Check sibling by using getNextSibling();
        ypmNode = ypmNode.getNextSibling();
        assertThat(ypmNode, notNullValue()); // either y or z should be there as sibling
        ypmNode = ypmNode.getNextSibling();
        assertThat(ypmNode, notNullValue()); // either y or z should be there as sibling
        ypmNode = ypmNode.getNextSibling();
        assertThat(ypmNode, nullValue()); // last sibling point to next sibling as null
        // Check sibling by using getPreviousSibling()
        ypmNode = moduleNode.getChild(zNodeName);
        assertThat(ypmNode.getName(), is(zNodeName));
        ypmNode = ypmNode.getPreviousSibling();
        assertThat(ypmNode, notNullValue()); // either x or y should be there as sibling
        ypmNode = ypmNode.getPreviousSibling();
        assertThat(ypmNode, notNullValue()); // either x or y should be there as sibling
        ypmNode = ypmNode.getPreviousSibling();
        assertThat(ypmNode, nullValue()); // last sibling point to next sibling as null
        // Checks the child x1 and x2
        ypmNode = moduleNode.getChild(xNodeName);
        ypmNode = ypmNode.getChild(x1NodeName);
        assertThat(ypmNode.getName(), is(x1NodeName));
        ypmNode = ypmNode.getSibling(x2NodeName);
        assertThat(ypmNode.getName(), is(x2NodeName));
        // Checks the child y1 and y2
        ypmNode = moduleNode.getChild(yNodeName);
        ypmNode = ypmNode.getChild(y1NodeName);
        assertThat(ypmNode.getName(), is(y1NodeName));
        ypmNode = ypmNode.getSibling(y2NodeName);
        assertThat(ypmNode.getName(), is(y2NodeName));
        // Checks the child z1 and z2
        ypmNode = moduleNode.getChild(zNodeName);
        ypmNode = ypmNode.getChild(z1NodeName);
        assertThat(ypmNode.getName(), is(z1NodeName));
        ypmNode = ypmNode.getSibling(z2NodeName);
        assertThat(ypmNode.getName(), is(z2NodeName));
    }

    /**
     * Checks ypm tree multiple module construction.
     */
    @Test
    public void testYpmTreeMultiModuleConstruction() {
        DefaultYpmNode rootNode = (DefaultYpmNode) constructYpmTreeSingleModule();
        rootNode = (DefaultYpmNode) constructYpmTreeMultiModule(rootNode);
        // Check one by one node
        String name = rootNode.getName();
        assertThat(name, is(logicalName));
        YpmContext moduleNode = rootNode.getChild(moduleName2);
        assertThat(moduleNode.getName(), is(moduleName2));
        YpmContext ypmNode = moduleNode.getChild(xNodeName);
        assertThat(ypmNode.getName(), is(xNodeName));
        // Check sibling by using getNextSibling();
        ypmNode = ypmNode.getNextSibling();
        assertThat(ypmNode, notNullValue()); // either y or z should be there as sibling
        ypmNode = ypmNode.getNextSibling();
        assertThat(ypmNode, notNullValue()); // either y or z should be there as sibling
        ypmNode = ypmNode.getNextSibling();
        assertThat(ypmNode, nullValue()); // last sibling point to next sibling as null
        // Check sibling by using getPreviousSibling()
        ypmNode = moduleNode.getChild(zNodeName);
        assertThat(ypmNode.getName(), is(zNodeName));
        ypmNode = ypmNode.getPreviousSibling();
        assertThat(ypmNode, notNullValue()); // either x or y should be there as sibling
        ypmNode = ypmNode.getPreviousSibling();
        assertThat(ypmNode, notNullValue()); // either x or y should be there as sibling
        ypmNode = ypmNode.getPreviousSibling();
        assertThat(ypmNode, nullValue()); // last sibling point to next sibling as null
        // Checks the child x1 and x2
        ypmNode = moduleNode.getChild(xNodeName);
        ypmNode = ypmNode.getChild(x1NodeName);
        assertThat(ypmNode.getName(), is(x1NodeName));
        ypmNode = ypmNode.getSibling(x2NodeName);
        assertThat(ypmNode.getName(), is(x2NodeName));
        // Checks the child y1 and y2
        ypmNode = moduleNode.getChild(yNodeName);
        ypmNode = ypmNode.getChild(y1NodeName);
        assertThat(ypmNode.getName(), is(y1NodeName));
        ypmNode = ypmNode.getSibling(y2NodeName);
        assertThat(ypmNode.getName(), is(y2NodeName));
        // Checks the child z1 and z2
        ypmNode = moduleNode.getChild(zNodeName);
        ypmNode = ypmNode.getChild(z1NodeName);
        assertThat(ypmNode.getName(), is(z1NodeName));
        ypmNode = ypmNode.getSibling(z2NodeName);
        assertThat(ypmNode.getName(), is(z2NodeName));
    }
}
