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
package org.onosproject.config.model;

/**
 * Hollow definition of ResourceIdentifier for ConfigService APIs.
 */
public interface ResourceIdentifier {
    //will remove this when the corresponding changes in onos-yang-tools become available

    /**
     * Returns the node key used to uniquely identify the branch in the
     * logical tree.
     *
     * @return node key uniquely identifying the branch
     */
    NodeKey nodeKey();

    /**
     * Returns the descendent resource identifier.
     *
     * @return descendent resource identifier
     */
    ResourceIdentifier descendentIdentifier();

    String getBase();
    String asString();
    //DefaultResourceIdentifier asResId(NodeKey nkey);
}
