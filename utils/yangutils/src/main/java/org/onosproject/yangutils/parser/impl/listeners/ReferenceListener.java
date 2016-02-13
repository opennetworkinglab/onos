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

import org.onosproject.yangutils.datamodel.YangReference;
import org.onosproject.yangutils.parser.Parsable;
import org.onosproject.yangutils.parser.ParsableDataType;
import org.onosproject.yangutils.parser.antlrgencode.GeneratedYangParser;
import org.onosproject.yangutils.parser.exceptions.ParserException;
import org.onosproject.yangutils.parser.impl.TreeWalkListener;
import org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorLocation;
import org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorMessageConstruction;
import org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorType;
import org.onosproject.yangutils.parser.impl.parserutils.ListenerValidation;

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
 * Implements listener based call back function corresponding to the "reference"
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
     * @param listener listener's object.
     * @param ctx context object of the grammar rule.
     */
    public static void processReferenceEntry(TreeWalkListener listener,
                                             GeneratedYangParser.ReferenceStatementContext ctx) {

        // Check for stack to be non empty.
        ListenerValidation.checkStackIsNotEmpty(listener, ListenerErrorType.MISSING_HOLDER,
                ParsableDataType.REFERENCE_DATA, String.valueOf(ctx.string().getText()),
                ListenerErrorLocation.ENTRY);

        Parsable tmpData = listener.getParsedDataStack().peek();
        if (tmpData instanceof YangReference) {
            YangReference reference = (YangReference) tmpData;
            reference.setReference(ctx.string().getText());
        } else {
            throw new ParserException(ListenerErrorMessageConstruction
                    .constructListenerErrorMessage(ListenerErrorType.INVALID_HOLDER,
                            ParsableDataType.REFERENCE_DATA,
                            String.valueOf(ctx.string().getText()),
                            ListenerErrorLocation.ENTRY));
        }
    }
}