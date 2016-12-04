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

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Service;
import org.onosproject.yms.ydt.YdtContext;
import org.onosproject.yms.ypm.YpmContext;
import org.onosproject.yms.ypm.YpmService;
import org.onosproject.yms.ypm.DefaultYpmNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents implementation of YANG protocol metadata manager.
 */
@Service
@Component(immediate = true)
public class YpmManager implements YpmService {

    private final Logger log = LoggerFactory.getLogger(getClass());
    private YpmContext rootYpmNode;

    @Activate
    public void activate() {
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        log.info("Stopped");
    }

    @Override
    public YpmContext getProtocolData(YdtContext rootYdtNode) {
        if (rootYdtNode == null) {
            log.debug("Input data is null. So, will not proceed.");
            return null;
        }

        // Iterate through YDT and search the path in YPM tree and create new YPM tree equivalent to given YDT
        // and return it.
        if (rootYpmNode == null) {
            log.debug("YPM tree has no data.");
            return null;
        }

        YdtContext currentYdtNode = rootYdtNode;
        YpmContext currentYpmNode = rootYpmNode;
        YpmContext rootRetYpmNode = new DefaultYpmNode(currentYdtNode.getName());
        YpmContext currRetYpmNode = rootRetYpmNode;
        while (currentYdtNode != null) {
            // Each ypm node can have children. So, create new corresponding ypm child
            // and add it to new returning ypm tree, otherwise return new ypm tree.
            YdtContext nextNode = currentYdtNode.getFirstChild();
            if (nextNode != null) {
                YpmContext ypmChild = currentYpmNode.getChild(nextNode.getName());
                if (ypmChild != null) {
                    currRetYpmNode.addChild(ypmChild.getName());
                } else {
                    return rootRetYpmNode;
                }
                currentYdtNode = nextNode;
                currentYpmNode = currentYpmNode.getChild(nextNode.getName());
                currRetYpmNode = currRetYpmNode.getChild(nextNode.getName());
                currRetYpmNode.setMetaData(currentYpmNode.getMetaData());
                continue;
            }

            // No child nodes, so walk tree
            while (currentYdtNode != null) {
                // Each ypm node can have sibling. So, create new corresponding ypm sibling node
                // and add it to new returning ypm tree, otherwise return new ypm tree.
                nextNode = currentYdtNode.getNextSibling();
                if (nextNode != null) {
                    YpmContext ypmSibling = currentYpmNode.getSibling(nextNode.getName());
                    if (ypmSibling != null) {
                        currRetYpmNode.addSibling(ypmSibling.getName());
                    } else {
                        return rootRetYpmNode;
                    }
                    currentYdtNode = nextNode;
                    currentYpmNode = currentYpmNode.getSibling(nextNode.getName());
                    currRetYpmNode = currRetYpmNode.getSibling(nextNode.getName());
                    currRetYpmNode.setMetaData(currentYpmNode.getMetaData());
                    break;
                }

                // Move up
                if (currentYdtNode == rootYdtNode) {
                    currentYdtNode = null;
                } else {
                    currentYdtNode = currentYdtNode.getParent();
                    currentYpmNode = currentYpmNode.getParent();
                    currRetYpmNode = currRetYpmNode.getParent();
                }
            }
        }

        return rootRetYpmNode;
    }

    @Override
    public void setProtocolData(YdtContext rootYdtNode, Object data) {
        if (rootYdtNode == null || data == null) {
            log.debug("Input data is null. So, will not proceed.");
            return;
        }

        // Iterate through YDT and add each node of this path to ypm tree if it is not exists
        YdtContext currentYdtNode = rootYdtNode;
        YpmContext currentYpmNode = rootYpmNode;
        while (currentYdtNode != null) {
            // Check the ypm root node first
            if (rootYpmNode == null) {
                rootYpmNode = new DefaultYpmNode(currentYdtNode.getName());
                currentYpmNode = rootYpmNode;
                // In logical node, should not set protocol metadata
            }

            // Move down to first child
            YdtContext nextNode = currentYdtNode.getFirstChild();
            if (nextNode != null) {
                // Each ypm node can have sibling. So, get corresponding sibling node, otherwise create it.
                if (currentYpmNode.getChild(nextNode.getName()) == null) {
                    currentYpmNode.addChild(nextNode.getName());
                }
                currentYpmNode = currentYpmNode.getChild(nextNode.getName());
                // Set/update protocol metadata
                currentYpmNode.setMetaData(data);
                currentYdtNode = nextNode;
                continue;
            }

            // No child nodes, so walk tree
            while (currentYdtNode != null) {
                // Move to sibling if possible.
                nextNode = currentYdtNode.getNextSibling();
                if (nextNode != null) {
                    // Each ypm node can have children (sibling). So, get corresponding ypm child, otherwise create it.
                    if (currentYpmNode.getSibling(nextNode.getName()) == null) {
                        currentYpmNode.addSibling(nextNode.getName());
                    }
                    currentYpmNode = currentYpmNode.getSibling(nextNode.getName());
                    // Set/update protocol metadata
                    currentYpmNode.setMetaData(data);
                    currentYdtNode = nextNode;
                    break;
                }

                // Move up
                if (currentYdtNode == rootYdtNode) {
                    currentYdtNode = null;
                } else {
                    currentYdtNode = currentYdtNode.getParent();
                    currentYpmNode = currentYpmNode.getParent();
                }
            }
        }
    }
}
