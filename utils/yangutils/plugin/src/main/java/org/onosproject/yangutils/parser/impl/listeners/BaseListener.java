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

import org.onosproject.yangutils.datamodel.YangBase;
import org.onosproject.yangutils.datamodel.YangIdentity;
import org.onosproject.yangutils.datamodel.YangIdentityRef;
import org.onosproject.yangutils.datamodel.YangNode;
import org.onosproject.yangutils.datamodel.YangNodeIdentifier;
import org.onosproject.yangutils.datamodel.exceptions.DataModelException;
import org.onosproject.yangutils.linker.impl.YangResolutionInfoImpl;
import org.onosproject.yangutils.datamodel.utils.Parsable;
import org.onosproject.yangutils.parser.antlrgencode.GeneratedYangParser;
import org.onosproject.yangutils.parser.exceptions.ParserException;
import org.onosproject.yangutils.parser.impl.TreeWalkListener;

import static org.onosproject.yangutils.datamodel.utils.DataModelUtils.addResolutionInfo;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorLocation.ENTRY;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorLocation.EXIT;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorMessageConstruction.constructExtendedListenerErrorMessage;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorMessageConstruction.constructListenerErrorMessage;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorType.*;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerUtil.getValidNodeIdentifier;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerValidation.checkStackIsNotEmpty;
import static org.onosproject.yangutils.datamodel.utils.YangConstructType.BASE_DATA;

/**
 * base-stmt           = base-keyword sep identifier-ref-arg-str
 *                          optsep stmtend*
 * identifier-ref-arg  = [prefix ":"] identifier
 */

/**
 * Represents listener based call back function corresponding to the "base"
 * rule defined in ANTLR grammar file for corresponding ABNF rule in RFC 6020.
 */
public final class BaseListener {

    //Creates a new base listener.
    private BaseListener() {
    }

    /**
     * Performs validation and updates the data model tree when parser receives an
     * input matching the grammar rule (base).
     *
     * @param listener listener's object
     * @param ctx context object of the grammar rule
     */
    public static void processBaseEntry(TreeWalkListener listener,
                                          GeneratedYangParser.BaseStatementContext ctx) {

        // Check for stack to be non empty.
        checkStackIsNotEmpty(listener, MISSING_HOLDER, BASE_DATA, ctx.string().getText(), ENTRY);

        YangNodeIdentifier nodeIdentifier = getValidNodeIdentifier(ctx.string().getText(), BASE_DATA, ctx);

        Parsable tmpData = listener.getParsedDataStack().peek();

        /**
         * For identityref base node identifier is copied in identity listener itself, so no need to process
         * base statement for indentityref
         */
        if (tmpData instanceof YangIdentityRef) {
            return;
        }

        if (!(tmpData instanceof YangIdentity)) {
             throw new ParserException(constructListenerErrorMessage(INVALID_HOLDER, BASE_DATA,
                                                                     ctx.string().getText(), ENTRY));
        }

        YangBase yangBase = new YangBase();
        yangBase.setBaseIdentifier(nodeIdentifier);
        ((YangIdentity) tmpData).setBaseNode(yangBase);

        int errorLine = ctx.getStart().getLine();
        int errorPosition = ctx.getStart().getCharPositionInLine();

        // Add resolution information to the list
        YangResolutionInfoImpl resolutionInfo =
                new YangResolutionInfoImpl<YangBase>(yangBase, (YangNode) tmpData, errorLine, errorPosition);
        addToResolutionList(resolutionInfo, ctx);
    }

    /**
     * Add to resolution list.
     *
     * @param resolutionInfo resolution information
     * @param ctx context object of the grammar rule
     */
    private static void addToResolutionList(YangResolutionInfoImpl<YangBase> resolutionInfo,
                                            GeneratedYangParser.BaseStatementContext ctx) {

        try {
            addResolutionInfo(resolutionInfo);
        } catch (DataModelException e) {
            throw new ParserException(constructExtendedListenerErrorMessage(UNHANDLED_PARSED_DATA,
                    BASE_DATA, ctx.string().getText(), EXIT, e.getMessage()));
        }
    }

}
