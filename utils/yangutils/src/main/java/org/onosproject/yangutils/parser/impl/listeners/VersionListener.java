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
 * module-header-stmts = ;; these stmts can appear in any order
 *                       [yang-version-stmt stmtsep]
 *                        namespace-stmt stmtsep
 *                        prefix-stmt stmtsep
 *
 * submodule-header-stmts =
 *                            ;; these stmts can appear in any order
 *                            [yang-version-stmt stmtsep]
 *                             belongs-to-stmt stmtsep
 *
 * yang-version-stmt   = yang-version-keyword sep yang-version-arg-str
 *                       optsep stmtend
 *
 *
 * ANTLR grammar rule
 * module_header_statement : yang_version_stmt? namespace_stmt prefix_stmt
 *                         | yang_version_stmt? prefix_stmt namespace_stmt
 *                         | namespace_stmt yang_version_stmt? prefix_stmt
 *                         | namespace_stmt prefix_stmt yang_version_stmt?
 *                         | prefix_stmt namespace_stmt yang_version_stmt?
 *                         | prefix_stmt yang_version_stmt? namespace_stmt?
 *                         ;
 * submodule_header_statement : yang_version_stmt? belongs_to_stmt
 *                            | belongs_to_stmt yang_version_stmt?
 *                            ;
 * yang_version_stmt :   YANG_VERSION_KEYWORD INTEGER STMTEND;
 */

/**
 * Implements listener based call back function corresponding to the "version"
 * rule defined in ANTLR grammar file for corresponding ABNF rule in RFC 6020.
 */
public final class VersionListener {

    /**
     * Creates a new version listener.
     */
    private VersionListener() {
    }

    /**
     * It is called when parser receives an input matching the grammar
     * rule (version), perform validations and update the data model
     * tree.
     *
     * @param listener Listener's object.
     * @param ctx context object of the grammar rule.
     */
    public static void processVersionEntry(TreeWalkListener listener,
                                           GeneratedYangParser.YangVersionStatementContext ctx) {
        // TODO method implementation
    }
}
