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

import org.onosproject.yangutils.datamodel.YangList;
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
 * key-stmt            = key-keyword sep key-arg-str stmtend
 *
 * ANTLR grammar rule
 * keyStatement : KEY_KEYWORD string STMTEND;
 */

/**
 * Implements listener based call back function corresponding to the "key"
 * rule defined in ANTLR grammar file for corresponding ABNF rule in RFC 6020.
 */
public final class KeyListener {

    /**
     * Creates a new key listener.
     */
    private KeyListener() {
    }

    /**
     * It is called when parser receives an input matching the grammar
     * rule (key), perform validations and updates the data model
     * tree.
     *
     * @param listener listener's object.
     * @param ctx context object of the grammar rule.
     */
    public static void processKeyEntry(TreeWalkListener listener,
                                         GeneratedYangParser.KeyStatementContext ctx) {

        // Check for stack to be non empty.
        ListenerValidation.checkStackIsNotEmpty(listener, ListenerErrorType.MISSING_HOLDER,
                ParsableDataType.KEY_DATA, String.valueOf(ctx.string().getText()),
                ListenerErrorLocation.ENTRY);

        Parsable tmpData = listener.getParsedDataStack().peek();
        if (listener.getParsedDataStack().peek() instanceof YangList) {
            YangList yangList = (YangList) tmpData;
            String tmpKeyValue = ctx.string().getText().replace("\"", "");
            if (tmpKeyValue.contains(" ")) {
                String[] keyValues = tmpKeyValue.split(" ");
                for (String keyValue : keyValues) {
                    yangList.addKey(keyValue);
                }
            } else {
                yangList.addKey(tmpKeyValue);
            }
        } else {
            throw new ParserException(ListenerErrorMessageConstruction
                    .constructListenerErrorMessage(ListenerErrorType.INVALID_HOLDER,
                            ParsableDataType.KEY_DATA,
                            String.valueOf(ctx.string().getText()),
                            ListenerErrorLocation.ENTRY));
        }
    }
}