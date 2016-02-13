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

import org.onosproject.yangutils.datamodel.YangModule;
import org.onosproject.yangutils.datamodel.YangNameSpace;
import org.onosproject.yangutils.parser.Parsable;
import org.onosproject.yangutils.parser.ParsableDataType;
import org.onosproject.yangutils.parser.antlrgencode.GeneratedYangParser;
import org.onosproject.yangutils.parser.exceptions.ParserException;
import org.onosproject.yangutils.parser.impl.TreeWalkListener;
import org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorLocation;
import org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorMessageConstruction;
import org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorType;
import org.onosproject.yangutils.parser.impl.parserutils.ListenerValidation;

import java.net.URI;

/*
 * Reference: RFC6020 and YANG ANTLR Grammar
 *
 * ABNF grammar as per RFC6020
 * module-header-stmts = ;; these stmts can appear in any order
 *                       [yang-version-stmt stmtsep]
 *                        namespace-stmt stmtsep
 *                        prefix-stmt stmtsep
 *
 * namespace-stmt      = namespace-keyword sep uri-str optsep stmtend
 *
 * ANTLR grammar rule
 * module_header_statement : yang_version_stmt? namespace_stmt prefix_stmt
 *                         | yang_version_stmt? prefix_stmt namespace_stmt
 *                         | namespace_stmt yang_version_stmt? prefix_stmt
 *                         | namespace_stmt prefix_stmt yang_version_stmt?
 *                         | prefix_stmt namespace_stmt yang_version_stmt?
 *                         | prefix_stmt yang_version_stmt? namespace_stmt
 *                         ;
 * namespace_stmt : NAMESPACE_KEYWORD string STMTEND;
 */

/**
 * Implements listener based call back function corresponding to the "namespace"
 * rule defined in ANTLR grammar file for corresponding ABNF rule in RFC 6020.
 */
public final class NamespaceListener {

    /**
     * Creates a new namespace listener.
     */
    private NamespaceListener() {
    }

    /**
     * It is called when parser receives an input matching the grammar
     * rule (namespace), perform validations and update the data model
     * tree.
     *
     * @param listener Listener's object.
     * @param ctx context object of the grammar rule.
     */
    public static void processNamespaceEntry(TreeWalkListener listener,
                                             GeneratedYangParser.NamespaceStatementContext ctx) {

        // Check for stack to be non empty.
        ListenerValidation.checkStackIsNotEmpty(listener, ListenerErrorType.MISSING_HOLDER,
                                                ParsableDataType.NAMESPACE_DATA,
                                                String.valueOf(ctx.string().getText()), ListenerErrorLocation.ENTRY);

        if (!validateUriValue(String.valueOf(ctx.string().getText()))) {
            ParserException parserException = new ParserException("Invalid namespace URI");
            parserException.setLine(ctx.string().STRING(0).getSymbol().getLine());
            parserException.setCharPosition(ctx.string().STRING(0).getSymbol().getCharPositionInLine());
            throw parserException;
        }

        // Obtain the node of the stack.
        Parsable tmpNode = listener.getParsedDataStack().peek();
        switch (tmpNode.getParsableDataType()) {
        case MODULE_DATA: {
            YangModule module = (YangModule) tmpNode;
            YangNameSpace uri = new YangNameSpace();
            uri.setUri(String.valueOf(ctx.string().getText()));
            module.setNameSpace(uri);
            break;
        }
        default:
            throw new ParserException(
                                      ListenerErrorMessageConstruction
                                              .constructListenerErrorMessage(ListenerErrorType.INVALID_HOLDER,
                                                                             ParsableDataType.NAMESPACE_DATA,
                                                                             String.valueOf(ctx.string().getText()),
                                                                             ListenerErrorLocation.ENTRY));
        }
    }

    /**
     * Validate input URI.
     *
     * @param uri input namespace URI
     * @return validation result
     */
    private static boolean validateUriValue(String uri) {
        uri = uri.replace("\"", "");
        final URI tmpUri;
        try {
            tmpUri = URI.create(uri);
        } catch (Exception e1) {
            return false;
        }
        return true;
    }
}