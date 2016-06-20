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
 *
 * if-feature-stmt     = if-feature-keyword sep identifier-ref-arg-str
 *                        optsep stmtend
 *
 * ANTLR grammar rule
 * ifFeatureStatement : IF_FEATURE_KEYWORD string STMTEND;
 */

import org.onosproject.yangutils.datamodel.YangFeature;
import org.onosproject.yangutils.datamodel.YangIfFeature;
import org.onosproject.yangutils.datamodel.YangIfFeatureHolder;
import org.onosproject.yangutils.datamodel.YangLeaf;
import org.onosproject.yangutils.datamodel.YangLeafList;
import org.onosproject.yangutils.datamodel.YangNode;
import org.onosproject.yangutils.datamodel.YangNodeIdentifier;
import org.onosproject.yangutils.datamodel.YangResolutionInfo;
import org.onosproject.yangutils.datamodel.exceptions.DataModelException;
import org.onosproject.yangutils.datamodel.utils.Parsable;
import org.onosproject.yangutils.linker.impl.YangResolutionInfoImpl;
import org.onosproject.yangutils.parser.antlrgencode.GeneratedYangParser;
import org.onosproject.yangutils.parser.exceptions.ParserException;
import org.onosproject.yangutils.parser.impl.TreeWalkListener;

import static org.onosproject.yangutils.datamodel.utils.DataModelUtils.addResolutionInfo;
import static org.onosproject.yangutils.datamodel.utils.ResolvableStatus.UNRESOLVED;
import static org.onosproject.yangutils.datamodel.utils.YangConstructType.IF_FEATURE_DATA;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorLocation.ENTRY;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorLocation.EXIT;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorMessageConstruction.constructExtendedListenerErrorMessage;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorMessageConstruction.constructListenerErrorMessage;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorType.INVALID_HOLDER;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorType.MISSING_HOLDER;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorType.UNHANDLED_PARSED_DATA;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerUtil.getValidNodeIdentifier;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerValidation.checkStackIsNotEmpty;

/**
 * Represents listener based call back function corresponding to the "if-feature"
 * rule defined in ANTLR grammar file for corresponding ABNF rule in RFC 6020.
 */
public final class IfFeatureListener {

    /**
     * Creates a new IfFeature listener.
     */
    private IfFeatureListener() {
    }

    /**
     * Performs validation and updates the data model tree.It is called when parser receives
     * an input matching the grammar rule (if-feature).
     *
     * @param listener listener's object
     * @param ctx      context object of the grammar rule
     */
    public static void processIfFeatureEntry(TreeWalkListener listener,
                                             GeneratedYangParser.IfFeatureStatementContext ctx) {
        // Check for stack to be non empty.
        checkStackIsNotEmpty(listener, MISSING_HOLDER, IF_FEATURE_DATA, ctx.string().getText(), ENTRY);

        // Validate if-feature argument string
        YangNodeIdentifier nodeIdentifier = getValidNodeIdentifier(ctx.string().getText(),
                IF_FEATURE_DATA, ctx);

        YangIfFeature ifFeature = new YangIfFeature();
        ifFeature.setName(nodeIdentifier);
        ifFeature.setResolvableStatus(UNRESOLVED);
        YangIfFeatureHolder ifFeatureHolder;

        // Obtain the node of the stack.
        Parsable tmpNode = listener.getParsedDataStack().peek();
        if (tmpNode instanceof YangIfFeatureHolder) {
            ifFeatureHolder = (YangIfFeatureHolder) tmpNode;
            ifFeatureHolder.addIfFeatureList(ifFeature);
        } else {
            throw new ParserException(constructListenerErrorMessage(INVALID_HOLDER, IF_FEATURE_DATA,
                    ctx.string().getText(), ENTRY));
        }

        // Add resolution information to the list
        Parsable parentNode;
        if (tmpNode instanceof YangLeafList || tmpNode instanceof YangLeaf
                || tmpNode instanceof YangFeature) {
            Parsable leafData = listener.getParsedDataStack().pop();
            parentNode = listener.getParsedDataStack().peek();
            listener.getParsedDataStack().push(leafData);
        } else {
            parentNode = tmpNode;
        }

        // Verify parent node of leaf
        if (!(parentNode instanceof YangNode)) {
            throw new ParserException(constructListenerErrorMessage(INVALID_HOLDER, IF_FEATURE_DATA,
                    ctx.string().getText(), EXIT));
        }

        int errorLine = ctx.getStart().getLine();
        int errorPosition = ctx.getStart().getCharPositionInLine();
        YangResolutionInfoImpl resolutionInfo = new YangResolutionInfoImpl<YangIfFeature>(ifFeature,
                (YangNode) parentNode, errorLine,
                errorPosition);
        addToResolutionList(resolutionInfo, ctx);
    }

    /**
     * Add to resolution list.
     *
     * @param resolutionInfo resolution information.
     * @param ctx            context object of the grammar rule
     */
    private static void addToResolutionList(YangResolutionInfo<YangIfFeature> resolutionInfo,
                                            GeneratedYangParser.IfFeatureStatementContext ctx) {

        try {
            addResolutionInfo(resolutionInfo);
        } catch (DataModelException e) {
            throw new ParserException(constructExtendedListenerErrorMessage(UNHANDLED_PARSED_DATA,
                    IF_FEATURE_DATA, ctx.string().getText(), EXIT, e.getMessage()));
        }
    }
}
