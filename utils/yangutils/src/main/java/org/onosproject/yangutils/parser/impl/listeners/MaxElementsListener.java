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

import org.onosproject.yangutils.datamodel.YangLeafList;
import org.onosproject.yangutils.datamodel.YangList;
import org.onosproject.yangutils.parser.Parsable;
import org.onosproject.yangutils.parser.antlrgencode.GeneratedYangParser;
import org.onosproject.yangutils.parser.exceptions.ParserException;
import org.onosproject.yangutils.parser.impl.TreeWalkListener;

import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorLocation.ENTRY;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorMessageConstruction.constructListenerErrorMessage;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorType.INVALID_HOLDER;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorType.MISSING_HOLDER;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerValidation.checkStackIsNotEmpty;
import static org.onosproject.yangutils.utils.YangConstructType.MAX_ELEMENT_DATA;

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
 * Implements listener based call back function corresponding to the
 * "max-elements" rule defined in ANTLR grammar file for corresponding ABNF rule
 * in RFC 6020.
 */
public final class MaxElementsListener {

    /**
     * Creates a new max-elements listener.
     */
    private MaxElementsListener() {
    }

    /**
     * It is called when parser receives an input matching the grammar rule
     * (max-elements), performs validation and updates the data model tree.
     *
     * @param listener listener's object
     * @param ctx context object of the grammar rule
     */
    public static void processMaxElementsEntry(TreeWalkListener listener,
            GeneratedYangParser.MaxElementsStatementContext ctx) {
        int maxElementsValue;

        // Check for stack to be non empty.
        checkStackIsNotEmpty(listener, MISSING_HOLDER, MAX_ELEMENT_DATA, "", ENTRY);

        if (ctx.maxValueArgument().UNBOUNDED_KEYWORD() != null) {
            maxElementsValue = Integer.MAX_VALUE;
        } else {
            maxElementsValue = Integer.parseInt(ctx.maxValueArgument().INTEGER().getText());
        }

        Parsable tmpData = listener.getParsedDataStack().peek();
        switch (tmpData.getYangConstructType()) {
            case LEAF_LIST_DATA:
                YangLeafList leafList = (YangLeafList) tmpData;
                leafList.setMaxElelements(maxElementsValue);
                break;
            case LIST_DATA:
                YangList yangList = (YangList) tmpData;
                yangList.setMaxElements(maxElementsValue);
                break;
            default:
                throw new ParserException(constructListenerErrorMessage(INVALID_HOLDER, MAX_ELEMENT_DATA, "", ENTRY));
        }
    }
}