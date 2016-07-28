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

import org.onosproject.yangutils.datamodel.YangAppDataStructure;
import org.onosproject.yangutils.datamodel.utils.Parsable;
import org.onosproject.yangutils.parser.antlrgencode.GeneratedYangParser;
import org.onosproject.yangutils.parser.exceptions.ParserException;
import org.onosproject.yangutils.parser.impl.TreeWalkListener;

import static org.onosproject.yangutils.datamodel.utils.YangConstructType.KEY_DATA;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorLocation.ENTRY;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorMessageConstruction.constructListenerErrorMessage;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorType.INVALID_HOLDER;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorType.MISSING_HOLDER;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerUtil.removeQuotesAndHandleConcat;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerValidation.checkStackIsNotEmpty;
import static org.onosproject.yangutils.utils.UtilConstants.SPACE;

/*
 * Reference: RFC6020 and YANG ANTLR Grammar
 *
 * ABNF grammar as per RFC6020
 *   data-structure-key-stmt = prefix:key-keyword string ";"
 *
 * ANTLR grammar rule
 *   dataStructureKeyStatement : DATA_STRUCTURE_KEY string STMTEND;
 */

/**
 * Represents listener based call back function corresponding to the "key"
 * rule defined in ANTLR grammar file for corresponding ABNF rule in RFC 6020.
 */
public final class DataStructureKeyListener {

    /**
     * Creates a new data-structure-key listener.
     */
    private DataStructureKeyListener() {
    }

    /**
     * Performs validation and updates the data model tree. It is called when parser receives an
     * input matching the grammar rule(key).
     *
     * @param listener listener's object
     * @param ctx      context object of the grammar rule
     */
    public static void processDataStructureKeyEntry(TreeWalkListener listener,
                                                    GeneratedYangParser.DataStructureKeyStatementContext ctx) {

        // Check for stack to be non empty.
        checkStackIsNotEmpty(listener, MISSING_HOLDER, KEY_DATA, ctx.string().getText(), ENTRY);

        Parsable tmpData = listener.getParsedDataStack().peek();
        if (listener.getParsedDataStack().peek() instanceof YangAppDataStructure) {
            YangAppDataStructure dataStructure = (YangAppDataStructure) tmpData;
            String tmpKeyValue = removeQuotesAndHandleConcat(ctx.string().getText());
            if (tmpKeyValue.contains(SPACE)) {
                String[] keyValues = tmpKeyValue.split(SPACE);
                for (String keyValue : keyValues) {
                    dataStructure.addKey(keyValue);
                }
            } else {
                dataStructure.addKey(tmpKeyValue);
            }
        } else {
            throw new ParserException(constructListenerErrorMessage(INVALID_HOLDER, KEY_DATA, ctx.string().getText(),
                    ENTRY));
        }
    }
}

