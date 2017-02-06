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
 * Constants used in model package.
 */
final class ModelConstants {
    private ModelConstants() {

    }
    static final String INCOMPLETE_SCHEMA_INFO = "Schema info is not complete";
    static final String LEAF_IS_TERMINAL = "Leaf must be the terminal node";
    static final String NON_KEY_LEAF = "Leaf list is not a key of list";
    static final String NO_KEY_SET = "Resource Identifier is empty";
}
