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

package org.onosproject.yangutils.utils;

import java.util.List;

/**
 * Abstraction of an entity which represents augmentation of a YANG node.
 */
public interface AugmentationHolder {

    /**
     * Adds augment info to the augment info list.
     *
     * @param augmentInfo augment info of node
     */
    void addAugmentation(AugmentedInfo augmentInfo);

    /**
     * Removes augment info from the node.
     */
    void removeAugmentation();

    /**
     * Returns list of augment info.
     *
     * @return list of augment info
     */
    List<AugmentedInfo> getAugmentedInfoList();
}
