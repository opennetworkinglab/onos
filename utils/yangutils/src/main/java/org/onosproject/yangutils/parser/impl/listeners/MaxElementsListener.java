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
 * ABNF grammar as per RFC6020
 *  max-elements-stmt   = max-elements-keyword sep
 *                        max-value-arg-str stmtend
 *  max-value-arg-str   = < a string that matches the rule
 *                          max-value-arg >
 *
 * ANTLR grammar rule
 * maxElementsStatement :  MAX_ELEMENTS_KEYWORD maxValueArgument STMTEND;
 */

/**
 * Implements listener based call back function corresponding to the "max-elements"
 * rule defined in ANTLR grammar file for corresponding ABNF rule in RFC 6020.
 */
public final class MaxElementsListener {

    /**
     * Creates a new max-elements listener.
     */
    private MaxElementsListener() {
    }

    /**
     * It is called when parser receives an input matching the grammar
     * rule (max-elements), performs validation and updates the data model
     * tree.
     *
     * @param listener listener's object.
     * @param ctx context object of the grammar rule.
     */
    public static void processMaxElementsEntry(TreeWalkListener listener,
                                             GeneratedYangParser.MaxElementsStatementContext ctx) {
        // TODO method implementation
    }

    /**
     * It is called when parser exits from grammar rule (max-elements), it performs
     * validation and updates the data model tree.
     *
     * @param listener listener's object.
     * @param ctx context object of the grammar rule.
     */
    public static void processMaxElementsExit(TreeWalkListener listener,
                                            GeneratedYangParser.MaxElementsStatementContext ctx) {
        // TODO method implementation
    }
}