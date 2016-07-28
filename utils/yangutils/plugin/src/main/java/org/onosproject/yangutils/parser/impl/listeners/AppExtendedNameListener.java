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

import org.onosproject.yangutils.datamodel.YangAppExtendedName;
import org.onosproject.yangutils.datamodel.YangCompilerAnnotation;
import org.onosproject.yangutils.datamodel.utils.Parsable;
import org.onosproject.yangutils.parser.antlrgencode.GeneratedYangParser;
import org.onosproject.yangutils.parser.exceptions.ParserException;
import org.onosproject.yangutils.parser.impl.TreeWalkListener;

import static org.onosproject.yangutils.datamodel.utils.YangConstructType.APP_EXTENDED_NAME_DATA;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorLocation.ENTRY;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorMessageConstruction.constructListenerErrorMessage;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorType.INVALID_HOLDER;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorType.MISSING_HOLDER;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerUtil.getValidPrefix;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerUtil.removeQuotesAndHandleConcat;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerValidation.checkStackIsNotEmpty;

/*
 * Reference: RFC6020 and YANG ANTLR Grammar
 *
 * ABNF grammar as per RFC6020
 *   app-extended-stmt = prefix:app-extended-name-keyword string ";"
 *
 * ANTLR grammar rule
 * appExtendedStatement : APP_EXTENDED extendedName STMTEND;
 */

/**
 * Represents listener based call back function corresponding to the "app-extended-name"
 * rule defined in ANTLR grammar file for corresponding ABNF rule in RFC 6020.
 */
public final class AppExtendedNameListener {

    /**
     * Creates a new app-extended-name listener.
     */
    private AppExtendedNameListener() {
    }

    /**
     * Performs validation and updates the data model tree. It is called when parser receives an
     * input matching the grammar rule(app-extended-name).
     *
     * @param listener listener's object
     * @param ctx      context object of the grammar rule
     */
    public static void processAppExtendedNameEntry(TreeWalkListener listener,
                                                   GeneratedYangParser.AppExtendedStatementContext ctx) {

        checkStackIsNotEmpty(listener, MISSING_HOLDER, APP_EXTENDED_NAME_DATA, ctx.extendedName().getText(), ENTRY);

        String prefix = getValidPrefix(ctx.APP_EXTENDED().getText(), APP_EXTENDED_NAME_DATA, ctx);
        YangAppExtendedName extendedName = new YangAppExtendedName();
        extendedName.setPrefix(prefix);
        extendedName.setYangAppExtendedName(removeQuotesAndHandleConcat(ctx.extendedName().getText()));

        Parsable curData = listener.getParsedDataStack().peek();
        if (curData instanceof YangCompilerAnnotation) {
            YangCompilerAnnotation compilerAnnotation = ((YangCompilerAnnotation) curData);
            compilerAnnotation.setYangAppExtendedName(extendedName);
        } else {
            throw new ParserException(constructListenerErrorMessage(INVALID_HOLDER, APP_EXTENDED_NAME_DATA,
                    ctx.extendedName().getText(), ENTRY));
        }
    }
}
