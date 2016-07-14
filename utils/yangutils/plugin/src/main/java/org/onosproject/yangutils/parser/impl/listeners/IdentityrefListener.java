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

import org.onosproject.yangutils.datamodel.YangIdentityRef;
import org.onosproject.yangutils.datamodel.YangNode;
import org.onosproject.yangutils.datamodel.YangNodeIdentifier;
import org.onosproject.yangutils.datamodel.YangType;
import org.onosproject.yangutils.datamodel.exceptions.DataModelException;
import org.onosproject.yangutils.datamodel.utils.Parsable;
import org.onosproject.yangutils.datamodel.utils.builtindatatype.YangDataTypes;
import org.onosproject.yangutils.linker.impl.YangResolutionInfoImpl;
import org.onosproject.yangutils.parser.antlrgencode.GeneratedYangParser;
import org.onosproject.yangutils.parser.exceptions.ParserException;
import org.onosproject.yangutils.parser.impl.TreeWalkListener;

import static org.onosproject.yangutils.datamodel.utils.DataModelUtils.addResolutionInfo;
import static org.onosproject.yangutils.datamodel.utils.ResolvableStatus.UNRESOLVED;
import static org.onosproject.yangutils.datamodel.utils.YangConstructType.BASE_DATA;
import static org.onosproject.yangutils.datamodel.utils.YangConstructType.IDENTITYREF_DATA;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorLocation.ENTRY;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorLocation.EXIT;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorMessageConstruction.constructExtendedListenerErrorMessage;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorMessageConstruction.constructListenerErrorMessage;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorType.MISSING_HOLDER;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorType.INVALID_HOLDER;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorType.MISSING_CURRENT_HOLDER;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorType.UNHANDLED_PARSED_DATA;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerUtil.getValidNodeIdentifier;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerValidation.checkStackIsNotEmpty;

/**
 * Reference: RFC6020 and YANG ANTLR Grammar
 *
 * ABNF grammar as per RFC6020
 * identityref-specification =
 *                        base-stmt stmtsep
 * base-stmt           = base-keyword sep identifier-ref-arg-str
 *                          optsep stmtend*
 * identifier-ref-arg  = [prefix ":"] identifier
 */

/**
 * Represents listener based call back function corresponding to the "identityref"
 * rule defined in ANTLR grammar file for corresponding ABNF rule in RFC 6020.
 */
public final class IdentityrefListener {

    //Creates a new type listener.
    private IdentityrefListener() {
    }

    /**
     * Performs validation and updates the data model tree when parser receives an input
     * matching the grammar rule (identityref).
     *
     * @param listener listener's object
     * @param ctx      context object of the grammar rule
     */
    public static void processIdentityrefEntry(TreeWalkListener listener,
                                        GeneratedYangParser.IdentityrefSpecificationContext ctx) {

        // Check for stack to be non empty.
        checkStackIsNotEmpty(listener, MISSING_HOLDER, IDENTITYREF_DATA, "", ENTRY);

        if (listener.getParsedDataStack().peek() instanceof YangType) {

            YangIdentityRef identityRef = new YangIdentityRef();
            Parsable typeData = listener.getParsedDataStack().pop();
            YangDataTypes yangDataTypes = ((YangType) typeData).getDataType();
            YangResolutionInfoImpl resolutionInfo;

            // Validate node identifier.
            YangNodeIdentifier nodeIdentifier = getValidNodeIdentifier(ctx.baseStatement().string().getText(),
                                                                       BASE_DATA, ctx);
            identityRef.setBaseIdentity(nodeIdentifier);
            ((YangType) typeData).setDataTypeExtendedInfo(identityRef);

            int errorLine = ctx.getStart().getLine();
            int errorPosition = ctx.getStart().getCharPositionInLine();

            Parsable tmpData = listener.getParsedDataStack().peek();
            switch (tmpData.getYangConstructType()) {
                case LEAF_DATA:

                    // Pop the stack entry to obtain the parent YANG node.
                    Parsable leaf = listener.getParsedDataStack().pop();
                    Parsable parentNodeOfLeaf = listener.getParsedDataStack().peek();

                    // Push the popped entry back to the stack.
                    listener.getParsedDataStack().push(leaf);

                    // Verify parent node of leaf
                    if (!(parentNodeOfLeaf instanceof YangNode)) {
                        throw new ParserException(constructListenerErrorMessage(INVALID_HOLDER,
                                                                                IDENTITYREF_DATA, ctx.getText(), EXIT));
                    }

                    identityRef.setResolvableStatus(UNRESOLVED);

                    // Add resolution information to the list
                    resolutionInfo =  new YangResolutionInfoImpl<YangIdentityRef>(identityRef,
                                                  (YangNode) parentNodeOfLeaf, errorLine, errorPosition);
                    addToResolutionList(resolutionInfo, ctx);

                    break;
                case LEAF_LIST_DATA:

                    // Pop the stack entry to obtain the parent YANG node.
                    Parsable leafList = listener.getParsedDataStack().pop();
                    Parsable parentNodeOfLeafList = listener.getParsedDataStack().peek();

                    // Push the popped entry back to the stack.
                    listener.getParsedDataStack().push(leafList);

                    // Verify parent node of leaf
                    if (!(parentNodeOfLeafList instanceof YangNode)) {
                        throw new ParserException(constructListenerErrorMessage(INVALID_HOLDER,
                                                                                IDENTITYREF_DATA, ctx.getText(), EXIT));
                    }

                    identityRef.setResolvableStatus(UNRESOLVED);

                    // Add resolution information to the list
                    resolutionInfo = new YangResolutionInfoImpl<YangIdentityRef>(identityRef,
                                               (YangNode) parentNodeOfLeafList, errorLine, errorPosition);
                    addToResolutionList(resolutionInfo, ctx);
                    break;
                case UNION_DATA:

                    Parsable parentNodeOfUnionNode = listener.getParsedDataStack().peek();

                    // Verify parent node of leaf
                    if (!(parentNodeOfUnionNode instanceof YangNode)) {
                        throw new ParserException(constructListenerErrorMessage(INVALID_HOLDER,
                                                                                IDENTITYREF_DATA, ctx.getText(), EXIT));
                    }

                    identityRef.setResolvableStatus(UNRESOLVED);

                    // Add resolution information to the list
                    resolutionInfo = new YangResolutionInfoImpl<YangIdentityRef>(identityRef,
                                              (YangNode) parentNodeOfUnionNode, errorLine, errorPosition);
                    addToResolutionList(resolutionInfo, ctx);

                    break;
                case TYPEDEF_DATA:
                    /**
                     * Do not add the identity ref to resolution list. It needs to be
                     * added to resolution list, when leaf/leaf list references to
                     * this typedef. At this time that leaf/leaf-list becomes the
                     * parent for the identityref.
                     */
                    break;
                default:
                    throw new ParserException(constructListenerErrorMessage(INVALID_HOLDER, IDENTITYREF_DATA,
                                                                            ctx.getText(), EXIT));
            }
            listener.getParsedDataStack().push(typeData);
            listener.getParsedDataStack().push(identityRef);
         } else {
            throw new ParserException(constructListenerErrorMessage(INVALID_HOLDER, IDENTITYREF_DATA, "", ENTRY));
        }
    }

    /**
     * Performs validations and update the data model tree when parser exits from grammar
     * rule (identityref).
     *
     * @param listener Listener's object
     * @param ctx      context object of the grammar rule
     */
    public static void processIdentityrefExit(TreeWalkListener listener,
                                       GeneratedYangParser.IdentityrefSpecificationContext ctx) {

        // Check for stack to be non empty.
        checkStackIsNotEmpty(listener, MISSING_CURRENT_HOLDER, IDENTITYREF_DATA, ctx.getText(), EXIT);

        Parsable parsableType = listener.getParsedDataStack().pop();
        if (!(parsableType instanceof YangIdentityRef)) {
            throw new ParserException(constructListenerErrorMessage(INVALID_HOLDER, IDENTITYREF_DATA,
                                                                    ctx.getText(), EXIT));
        }
    }

    /**
     * Adds to resolution list.
     *
     * @param resolutionInfo resolution information
     * @param ctx            context object of the grammar rule
     */
    private static void addToResolutionList(YangResolutionInfoImpl<YangIdentityRef> resolutionInfo,
                                            GeneratedYangParser.IdentityrefSpecificationContext ctx) {
        try {
            addResolutionInfo(resolutionInfo);
        } catch (DataModelException e) {
            throw new ParserException(constructExtendedListenerErrorMessage(UNHANDLED_PARSED_DATA,
                                               IDENTITYREF_DATA, ctx.getText(), ENTRY, e.getMessage()));
        }
    }
}
