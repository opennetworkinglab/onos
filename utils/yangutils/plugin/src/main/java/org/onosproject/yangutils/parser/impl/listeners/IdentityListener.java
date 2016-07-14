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
package org.onosproject.yangutils.parser.impl.listeners;

import org.onosproject.yangutils.datamodel.YangIdentity;
import org.onosproject.yangutils.datamodel.YangModule;
import org.onosproject.yangutils.datamodel.YangNode;
import org.onosproject.yangutils.datamodel.YangSubModule;
import org.onosproject.yangutils.datamodel.exceptions.DataModelException;
import org.onosproject.yangutils.datamodel.utils.Parsable;
import org.onosproject.yangutils.parser.antlrgencode.GeneratedYangParser;
import org.onosproject.yangutils.parser.exceptions.ParserException;
import org.onosproject.yangutils.parser.impl.TreeWalkListener;

import static org.onosproject.yangutils.datamodel.utils.GeneratedLanguage.JAVA_GENERATION;
import static org.onosproject.yangutils.translator.tojava.YangDataModelFactory.getYangIdentityNode;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorLocation.ENTRY;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorLocation.EXIT;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorMessageConstruction.constructExtendedListenerErrorMessage;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorMessageConstruction.constructListenerErrorMessage;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorType.MISSING_HOLDER;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorType.UNHANDLED_PARSED_DATA;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorType.INVALID_HOLDER;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorType.MISSING_CURRENT_HOLDER;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerUtil.getValidIdentifier;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerValidation.checkStackIsNotEmpty;
import static org.onosproject.yangutils.datamodel.utils.YangConstructType.IDENTITY_DATA;

/**
 * Reference: RFC6020 and YANG ANTLR Grammar.
 *
 * ABNF grammar as per RFC6020
 * identity-stmt       = identity-keyword sep identifier-arg-str optsep
 *                       (";" /
 *                        "{" stmtsep
 *                            ;; these stmts can appear in any order
 *                            [base-stmt stmtsep]
 *                            [status-stmt stmtsep]
 *                            [description-stmt stmtsep]
 *                            [reference-stmt stmtsep]
 *                        "}")
 */

/**
 * Represents listener based call back function corresponding to the "identity"
 * rule defined in ANTLR grammar file for corresponding ABNF rule in RFC 6020.
 */
public final class IdentityListener {

    //Creates a identity listener.
    private IdentityListener() {
    }

    /**
     * Performs validations and update the data model tree when parser receives an input
     * matching the grammar rule (identity).
     *
     * @param listener Listener's object
     * @param ctx      context object of the grammar rule
     */
    public static void processIdentityEntry(TreeWalkListener listener,
                                            GeneratedYangParser.IdentityStatementContext ctx) {

        // Check for stack to be non empty.
        checkStackIsNotEmpty(listener, MISSING_HOLDER, IDENTITY_DATA, ctx.identifier().getText(), ENTRY);

        String identifier = getValidIdentifier(ctx.identifier().getText(), IDENTITY_DATA, ctx);

        YangIdentity identity = getYangIdentityNode(JAVA_GENERATION);
        identity.setName(identifier);

        Parsable curData = listener.getParsedDataStack().peek();
        if (curData instanceof YangModule || curData instanceof YangSubModule) {
            YangNode curNode = (YangNode) curData;
            try {
                curNode.addChild(identity);
            } catch (DataModelException e) {
                throw new ParserException(constructExtendedListenerErrorMessage(UNHANDLED_PARSED_DATA,
                        IDENTITY_DATA, ctx.identifier().getText(), ENTRY, e.getMessage()));
            }
            // Push identity node to the stack.
            listener.getParsedDataStack().push(identity);
        } else {
            throw new ParserException(constructListenerErrorMessage(INVALID_HOLDER, IDENTITY_DATA,
                    ctx.identifier().getText(), ENTRY));
        }

    }

    /**
     * Performs validations and update the data model tree when parser exits from grammar
     * rule (identity).
     *
     * @param listener Listener's object
     * @param ctx      context object of the grammar rule
     */
    public static void processIdentityExit(TreeWalkListener listener,
                                       GeneratedYangParser.IdentityStatementContext ctx) {

        // Check for stack to be non empty.
        checkStackIsNotEmpty(listener, MISSING_CURRENT_HOLDER, IDENTITY_DATA, ctx.identifier().getText(), EXIT);

        Parsable parsableType = listener.getParsedDataStack().pop();
        if (!(parsableType instanceof YangIdentity)) {
            throw new ParserException(constructListenerErrorMessage(INVALID_HOLDER, IDENTITY_DATA,
                    ctx.identifier().getText(), EXIT));
        }
    }

}
