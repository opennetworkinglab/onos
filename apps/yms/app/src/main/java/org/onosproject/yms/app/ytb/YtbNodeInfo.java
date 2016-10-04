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

package org.onosproject.yms.app.ytb;

import org.onosproject.yangutils.datamodel.YangAugment;

import java.util.Iterator;
import java.util.Map;

/**
 * Represents YTB node info for all the nodes that are added to the YDT
 * builder tree.Contains the information which can be attached and retrieved
 * back from YDT while walking.
 */
public class YtbNodeInfo {

    /**
     * Object of the corresponding YANG construct. This object is bound to
     * each and every YDT node. So, whenever walk of parent and sibling
     * happens, object can be retrieved from its YDT node.
     */
    private Object yangObject;

    /**
     * The list iterator since first content of the multi instance node is
     * faced. With this iterator the node can be walked multiple times till
     * it becomes empty.
     */
    private Iterator<Object> listIterator;

    /**
     * The current YTB node's, list of augments are iterated through this
     * iterator. Every time an augment is built completely, this iterator
     * gives the next augment node until it becomes empty.
     */
    private Iterator<YangAugment> augmentNodeItr;

    /**
     * The map with case object as value and choice node name as key is added
     * for the current YTB info. Every time a case schema node comes, it takes
     * this map and checks if it is present.
     */
    private Map<String, Object> choiceCaseMap;

    /**
     * When the case finds its object in map, it assigns it to case object of
     * the YTB info, so when its child wants to take the parent object, they
     * can take from the YTB info's case object.
     */
    private Object caseObject;

    /**
     * When the augment object is present, it assigns it to augment object of
     * the YTB info, so when its child wants to take the parent object, they
     * can take from the YTB info's augment object.
     */
    private Object augmentObject;

    /**
     * Constructs a default YTB node info.
     */
    public YtbNodeInfo() {
    }

    /**
     * Returns the object of the YANG schema node.
     *
     * @return YANG node object
     */
    public Object getYangObject() {
        return yangObject;
    }

    /**
     * Sets the object of the YANG schema node.
     *
     * @param yangObject YANG node object
     */
    public void setYangObject(Object yangObject) {
        this.yangObject = yangObject;
    }

    /**
     * Returns the current list iterator of the YANG schema node.
     *
     * @return current list iterator for the schema node
     */
    public Iterator<Object> getListIterator() {
        return listIterator;
    }

    /**
     * Sets the current list iterator of the YANG schema node.
     *
     * @param listIterator current list iterator for the schema node
     */
    public void setListIterator(Iterator<Object> listIterator) {
        this.listIterator = listIterator;
    }

    /**
     * Returns the map of choice schema name and case object.
     *
     * @return choice name and case object map
     */
    public Map<String, Object> getChoiceCaseMap() {
        return choiceCaseMap;
    }

    /**
     * Sets the map of choice schema name and case object.
     *
     * @param choiceCaseMap choice name and case object map
     */
    public void setChoiceCaseMap(Map<String, Object> choiceCaseMap) {
        this.choiceCaseMap = choiceCaseMap;
    }

    /**
     * Returns the case object.
     *
     * @return case object
     */
    public Object getCaseObject() {
        return caseObject;
    }

    /**
     * Sets the case node object.
     *
     * @param caseObject case node object
     */
    public void setCaseObject(Object caseObject) {
        this.caseObject = caseObject;
    }

    /**
     * Returns the augment node object.
     *
     * @return augment node object
     */
    public Object getAugmentObject() {
        return augmentObject;
    }

    /**
     * Sets the augment node object.
     *
     * @param augmentObject augment node object
     */
    public void setAugmentObject(Object augmentObject) {
        this.augmentObject = augmentObject;
    }

    /**
     * Returns the current list iterator of the YANG augment node.
     *
     * @return augment node iterator
     */
    public Iterator<YangAugment> getAugmentIterator() {
        return augmentNodeItr;
    }

    /**
     * Sets the current list iterator of the YANG augment node.
     *
     * @param augmentNodeItr augment node iterator
     */
    public void setAugmentIterator(Iterator<YangAugment> augmentNodeItr) {
        this.augmentNodeItr = augmentNodeItr;
    }
}
