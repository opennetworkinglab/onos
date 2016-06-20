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

/*
 * Reference: RFC6020 and YANG ANTLR Grammar
 *
 * ABNF grammar as per RFC6020
 *  feature-stmt        = feature-keyword sep identifier-arg-str optsep
 *                        (";" /
 *                         "{" stmtsep
 *                             ;; these stmts can appear in any order
 *                             *(if-feature-stmt stmtsep)
 *                             [status-stmt stmtsep]
 *                             [description-stmt stmtsep]
 *                             [reference-stmt stmtsep]
 *                         "}")
 *
 *
 *
 * ANTLR grammar rule
 * featureStatement : FEATURE_KEYWORD string (STMTEND | LEFT_CURLY_BRACE featureBody RIGHT_CURLY_BRACE);
 */

import org.onosproject.yangutils.datamodel.YangFeature;
import org.onosproject.yangutils.datamodel.YangFeatureHolder;
import org.onosproject.yangutils.datamodel.utils.Parsable;
import org.onosproject.yangutils.parser.antlrgencode.GeneratedYangParser;
import org.onosproject.yangutils.parser.exceptions.ParserException;
import org.onosproject.yangutils.parser.impl.TreeWalkListener;

import static org.onosproject.yangutils.datamodel.utils.YangConstructType.FEATURE_DATA;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorLocation.ENTRY;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorLocation.EXIT;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorMessageConstruction.constructListenerErrorMessage;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorType.INVALID_HOLDER;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorType.MISSING_CURRENT_HOLDER;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorType.MISSING_HOLDER;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerUtil.getValidIdentifier;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerValidation.checkStackIsNotEmpty;

/**
 * Represents listener based call back function corresponding to the "feature"
 * rule defined in ANTLR grammar file for corresponding ABNF rule in RFC 6020.
 */
public final class FeatureListener {

    /**
     * Creates a new feature listener.
     */
    private FeatureListener() {
    }

    /**
     * Performs validation and updates the data model tree.It is called when parser receives
     * an input matching the grammar rule (feature).
     *
     * @param listener listener's object
     * @param ctx      context object of the grammar rule
     */
    public static void processFeatureEntry(TreeWalkListener listener,
                                           GeneratedYangParser.FeatureStatementContext ctx) {

        // Check for stack to be non empty.
        checkStackIsNotEmpty(listener, MISSING_HOLDER, FEATURE_DATA, ctx.string().getText(), ENTRY);

        String identifier = getValidIdentifier(ctx.string().getText(), FEATURE_DATA, ctx);

        // Obtain the node of the stack.
        Parsable tmpNode = listener.getParsedDataStack().peek();
        if (tmpNode instanceof YangFeatureHolder) {
            YangFeatureHolder featureHolder = (YangFeatureHolder) tmpNode;

            YangFeature feature = new YangFeature();
            feature.setName(identifier);

            featureHolder.addFeatureList(feature);
            listener.getParsedDataStack().push(feature);
        } else {
            throw new ParserException(constructListenerErrorMessage(INVALID_HOLDER, FEATURE_DATA,
                    ctx.string().getText(), ENTRY));
        }
    }

    /**
     * Perform validations and updates the data model tree.It is called when parser exits from
     * grammar rule(feature).
     *
     * @param listener listener's object
     * @param ctx      context object of the grammar rule
     */
    public static void processFeatureExit(TreeWalkListener listener,
                                          GeneratedYangParser.FeatureStatementContext ctx) {

        // Check for stack to be non empty.
        checkStackIsNotEmpty(listener, MISSING_HOLDER, FEATURE_DATA, ctx.string().getText(), EXIT);

        if (listener.getParsedDataStack().peek() instanceof YangFeature) {
            listener.getParsedDataStack().pop();
        } else {
            throw new ParserException(constructListenerErrorMessage(MISSING_CURRENT_HOLDER, FEATURE_DATA,
                    ctx.string().getText(), EXIT));
        }
    }
}
