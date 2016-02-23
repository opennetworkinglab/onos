/*Copyright 2016.year Open Networking Laboratory

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.*/
package org.onosproject.yangutils.datamodel;

import java.util.List;

/**
 * Abstraction of atomic configurable/status entity. It is used to abstract the
 * data holders of leaf or leaf list. Used in leaves parsing or attribute code
 * generation.
 */
public interface YangLeavesHolder {

    /**
     * Get the list of leaves from data holder like container / list.
     *
     * @return the list of leaves.
     */
    public List<YangLeaf> getListOfLeaf();

    /**
     * Add a leaf in data holder like container / list.
     *
     * @param leaf the leaf to be added.
     */
    void addLeaf(YangLeaf leaf);

    /**
     * Get the list of leaf-list from data holder like container / list.
     *
     * @return the list of leaf-list.
     */
    List<YangLeafList> getListOfLeafList();

    /**
     * Add a leaf-list in data holder like container / list.
     *
     * @param leafList the leaf-list to be added.
     */
    void addLeafList(YangLeafList leafList);
}
