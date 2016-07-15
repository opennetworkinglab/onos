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

import org.onosproject.yangutils.datamodel.YangExtension;
import org.onosproject.yangutils.datamodel.YangModule;
import org.onosproject.yangutils.datamodel.YangSubModule;
import org.onosproject.yangutils.datamodel.utils.Parsable;
import org.onosproject.yangutils.parser.antlrgencode.GeneratedYangParser;
import org.onosproject.yangutils.parser.exceptions.ParserException;
import org.onosproject.yangutils.parser.impl.TreeWalkListener;

import static org.onosproject.yangutils.datamodel.utils.YangConstructType.EXTENSION_DATA;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorLocation.ENTRY;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorLocation.EXIT;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorMessageConstruction.constructListenerErrorMessage;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorType.INVALID_HOLDER;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorType.MISSING_CURRENT_HOLDER;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorType.MISSING_HOLDER;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerUtil.getValidIdentifier;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerValidation.checkStackIsNotEmpty;

/*
 * Reference: RFC6020 and YANG ANTLR Grammar
 *
 * ABNF grammar as per RFC6020
 * extension-stmt      = extension-keyword sep identifier-arg-str optsep
 *                       (";" /
 *                        "{" stmtsep
 *                            ;; these stmts can appear in any order
 *                            [argument-stmt stmtsep]
 *                            [status-stmt stmtsep]
 *                            [description-stmt stmtsep]
 *                            [reference-stmt stmtsep]
 *                        "}")
 *
 * ANTLR grammar rule
 * extensionStatement : EXTENSION_KEYWORD identifier (STMTEND | LEFT_CURLY_BRACE extensionBody RIGHT_CURLY_BRACE);
 */

/**
 * Represents listener based call back function corresponding to the "extension"
 * rule defined in ANTLR grammar file for corresponding ABNF rule in RFC 6020.
 */
public final class ExtensionListener {

    /**
     * Creates a new extension listener.
     */
    private ExtensionListener() {
    }

    /**
     * Performs validation and updates the data model tree. It is called when parser
     * receives an input matching the grammar rule (extension).
     *
     * @param listener listener's object
     * @param ctx      context object of the grammar rule
     */
    public static void processExtensionEntry(TreeWalkListener listener,
                                             GeneratedYangParser.ExtensionStatementContext ctx) {

        // Check for stack to be non empty.
        checkStackIsNotEmpty(listener, MISSING_HOLDER, EXTENSION_DATA, ctx.identifier().getText(), ENTRY);

        String identifier = getValidIdentifier(ctx.identifier().getText(), EXTENSION_DATA, ctx);

        YangExtension extension = new YangExtension();
        extension.setName(identifier);

        Parsable curData = listener.getParsedDataStack().peek();
        switch (curData.getYangConstructType()) {
            case MODULE_DATA:
                YangModule module = ((YangModule) curData);
                module.addExtension(extension);
                break;
            case SUB_MODULE_DATA:
                YangSubModule subModule = ((YangSubModule) curData);
                subModule.addExtension(extension);
                break;
            default:
                throw new ParserException(constructListenerErrorMessage(INVALID_HOLDER, EXTENSION_DATA,
                        ctx.identifier().getText(), ENTRY));
        }
        listener.getParsedDataStack().push(extension);
    }

    /**
     * Performs validation and updates the data model tree. It is called when parser exits
     * from grammar rule(extension).
     *
     * @param listener listener's object
     * @param ctx      context object of the grammar rule
     */
    public static void processExtensionExit(TreeWalkListener listener,
                                            GeneratedYangParser.ExtensionStatementContext ctx) {

        // Check for stack to be non empty.
        checkStackIsNotEmpty(listener, MISSING_HOLDER, EXTENSION_DATA, ctx.identifier().getText(), EXIT);

        if (!(listener.getParsedDataStack().peek() instanceof YangExtension)) {
            throw new ParserException(constructListenerErrorMessage(MISSING_CURRENT_HOLDER, EXTENSION_DATA,
                    ctx.identifier().getText(), EXIT));
        }
        listener.getParsedDataStack().pop();
    }
}
