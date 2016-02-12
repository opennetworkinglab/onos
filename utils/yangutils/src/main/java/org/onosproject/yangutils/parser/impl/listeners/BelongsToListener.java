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
 * submodule-header-stmts =
 *                            ;; these stmts can appear in any order
 *                            [yang-version-stmt stmtsep]
 *                             belongs-to-stmt stmtsep
 *
 * belongs-to-stmt     = belongs-to-keyword sep identifier-arg-str
 *                       optsep
 *                       "{" stmtsep
 *                           prefix-stmt stmtsep
 *                       "}"
 *
 * ANTLR grammar rule
 * submodule_header_statement : yang_version_stmt? belongs_to_stmt
 *                            | belongs_to_stmt yang_version_stmt?
 *                            ;
 * belongs_to_stmt : BELONGS_TO_KEYWORD IDENTIFIER LEFT_CURLY_BRACE belongs_to_stmt_body RIGHT_CURLY_BRACE;
 * belongs_to_stmt_body : prefix_stmt;
 */

/**
 * Implements listener based call back function corresponding to the "belongs to"
 * rule defined in ANTLR grammar file for corresponding ABNF rule in RFC 6020.
 */
public final class BelongsToListener {

    /**
     * Creates a new belongto listener.
     */
    private BelongsToListener() {
    }

    /**
     * It is called when parser receives an input matching the grammar
     * rule (belongsto), perform validations and update the data model
     * tree.
     *
     * @param listener Listener's object.
     * @param ctx context object of the grammar rule.
     */
    public static void processBelongsToEntry(TreeWalkListener listener,
                                             GeneratedYangParser.BelongstoStatementContext ctx) {
        // TODO method implementation
    }

    /**
     * It is called when parser exits from grammar rule (belongsto), it perform
     * validations and update the data model tree.
     *
     * @param listener Listener's object.
     * @param ctx context object of the grammar rule.
     */
    public static void processBelongsToExit(TreeWalkListener listener,
                                            GeneratedYangParser.BelongstoStatementContext ctx) {
        // TODO method implementation
    }
}
