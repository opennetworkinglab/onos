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

package org.onosproject.yangutils.translator.tojava;

/**
 * Represents data model tree traversal types.
 */
public enum TraversalType {

    /**
     * Start of traversal at the tree root.
     */
    ROOT,

    /**
     * Child node traversal.
     */
    CHILD,

    /**
     * Sibling node traversal.
     */
    SIBILING,

    /**
     * Parent node traversal.
     */
    PARENT
}
