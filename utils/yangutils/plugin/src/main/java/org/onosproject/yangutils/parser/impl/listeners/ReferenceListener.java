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

import org.onosproject.yangutils.datamodel.YangReference;
import org.onosproject.yangutils.datamodel.utils.Parsable;
import org.onosproject.yangutils.parser.antlrgencode.GeneratedYangParser;
import org.onosproject.yangutils.parser.exceptions.ParserException;
import org.onosproject.yangutils.parser.impl.TreeWalkListener;

import static org.onosproject.yangutils.datamodel.utils.YangConstructType.REFERENCE_DATA;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorLocation.ENTRY;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorMessageConstruction.constructListenerErrorMessage;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorType.INVALID_HOLDER;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorType.MISSING_HOLDER;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerValidation.checkStackIsNotEmpty;

/*
 * Reference: RFC6020 and YANG ANTLR Grammar
 *
 * ABNF grammar as per RFC6020
 * reference-stmt      = reference-keyword sep string optsep stmtend
 *
 * ANTLR grammar rule
 * referenceStatement : REFERENCE_KEYWORD string STMTEND;
 */

/**
 * Represents listener based call back function corresponding to the "reference"
 * rule defined in ANTLR grammar file for corresponding ABNF rule in RFC 6020.
 */
public final class ReferenceListener {

    /**
     * Creates a new reference listener.
     */
    private ReferenceListener() {
    }

    /**
     * It is called when parser receives an input matching the grammar
     * rule (reference), performs validation and updates the data model
     * tree.
     *
     * @param listener listener's object
     * @param ctx context object of the grammar rule
     */
    public static void processReferenceEntry(TreeWalkListener listener,
                                             GeneratedYangParser.ReferenceStatementContext ctx) {

        // Check for stack to be non empty.
        checkStackIsNotEmpty(listener, MISSING_HOLDER, REFERENCE_DATA, ctx.string().getText(), ENTRY);

        Parsable tmpData = listener.getParsedDataStack().peek();
        if (tmpData instanceof YangReference) {
            YangReference reference = (YangReference) tmpData;
            reference.setReference(ctx.string().getText());
        } else {
            throw new ParserException(constructListenerErrorMessage(INVALID_HOLDER, REFERENCE_DATA,
                            ctx.string().getText(), ENTRY));
        }
    }
}