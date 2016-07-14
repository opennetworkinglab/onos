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
package org.onosproject.yangutils.datamodel;

/**
 * Abstraction of an entity which represent operation parameter info for augmentable node.
 */
public interface YangAugmentedOpParamInfo extends YangAugmentedInfo {

    /**
     * Returns class object of base class.
     *
     * @return class object of base class
     */
    Class<?> getBaseClass();

    /**
     * Returns if augmented info's contents matches.
     *
     * @param augmentedInfo augmented info
     * @return true or false
     */
    boolean isFilterContentMatch(Object augmentedInfo);
}
