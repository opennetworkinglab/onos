/*
 * Copyright 2016 Open Networking Laboratory
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

package org.onosproject.yangutils.parser.impl.listeners;

import org.onosproject.yangutils.parser.antlrgencode.GeneratedYangParser;
import org.onosproject.yangutils.parser.impl.TreeWalkListener;

/*
 * Reference: RFC6020 and YANG ANTLR Grammar
 *
 * ANTLR grammar rule
 * yangfile : module_stmt
 *          | submodule_stmt;
 */

/**
 * Implements call back function corresponding to the "base rule" defined in
 * ANTLR grammar file.
 */
public final class BaseFileListener {

    /**
     * Creates a new base listener.
     */
    private BaseFileListener() {
    }

    /**
     * It is called when parser receives an input matching the grammar
     * rule (yangfile), perform validations and update the data model
     * tree.
     *
     * @param listener Listener's object.
     * @param ctx context object of the grammar rule.
     */
    public static void processYangFileEntry(TreeWalkListener listener, GeneratedYangParser.YangfileContext ctx) {
        // TODO method implementation
    }

    /**
     * It is called when parser exits from grammar rule (yangfile), it perform
     * validations and update the data model tree.
     *
     * @param listener Listener's object.
     * @param ctx context object of the grammar rule.
     */
    public static void processYangFileExit(TreeWalkListener listener, GeneratedYangParser.YangfileContext ctx) {
        // TODO method implementation
    }
}
