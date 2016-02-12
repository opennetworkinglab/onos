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

/**
 * Implements listener based call back function corresponding to the "leaf"
 * rule defined in ANTLR grammar file for corresponding ABNF rule in RFC 6020.
 */
package org.onosproject.yangutils.parser.impl.listeners;

import org.onosproject.yangutils.parser.antlrgencode.GeneratedYangParser;
import org.onosproject.yangutils.parser.impl.TreeWalkListener;

/*
 * Reference: RFC6020 and YANG ANTLR Grammar
 *
 * ABNF grammar as per RFC6020
 *  leaf-stmt           = leaf-keyword sep identifier-arg-str optsep
 *                        "{" stmtsep
 *                            ;; these stmts can appear in any order
 *                            [when-stmt stmtsep]
 *                            *(if-feature-stmt stmtsep)
 *                            type-stmt stmtsep
 *                            [units-stmt stmtsep]
 *                            *(must-stmt stmtsep)
 *                            [default-stmt stmtsep]
 *                            [config-stmt stmtsep]
 *                            [mandatory-stmt stmtsep]
 *                            [status-stmt stmtsep]
 *                            [description-stmt stmtsep]
 *                            [reference-stmt stmtsep]
 *                         "}"
 *
 * ANTLR grammar rule
 *  leafStatement : LEAF_KEYWORD IDENTIFIER LEFT_CURLY_BRACE (whenStatement | ifFeatureStatement | typeStatement |
 *  unitsStatement | mustStatement | defaultStatement | configStatement | mandatoryStatement | statusStatement  |
 *  descriptionStatement | referenceStatement)* RIGHT_CURLY_BRACE;
 */

/**
 * Implements listener based call back function corresponding to the "leaf"
 * rule defined in ANTLR grammar file for corresponding ABNF rule in RFC 6020.
 */
public final class LeafListener {

    /**
     * Creates a new leaf listener.
     */
    private LeafListener() {
    }

    /**
     * It is called when parser receives an input matching the grammar
     * rule (leaf), performs validation and updates the data model
     * tree.
     *
     * @param listener listener's object.
     * @param ctx context object of the grammar rule.
     */
    public static void processLeafEntry(TreeWalkListener listener,
                                        GeneratedYangParser.LeafStatementContext ctx) {
        // TODO method implementation
    }

    /**
     * It is called when parser exits from grammar rule (leaf), performs
     * validation and updates the data model tree.
     *
     * @param listener listener's object.
     * @param ctx context object of the grammar rule.
     */
    public static void processLeafExit(TreeWalkListener listener,
                                       GeneratedYangParser.LeafStatementContext ctx) {
        // TODO method implementation
    }
}