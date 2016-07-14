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

import org.onosproject.yangutils.datamodel.YangDataTypes;
import org.onosproject.yangutils.datamodel.YangDerivedInfo;
import org.onosproject.yangutils.datamodel.YangLeaf;
import org.onosproject.yangutils.datamodel.YangLeafList;
import org.onosproject.yangutils.datamodel.YangNode;
import org.onosproject.yangutils.datamodel.YangNodeIdentifier;
import org.onosproject.yangutils.datamodel.YangType;
import org.onosproject.yangutils.datamodel.YangTypeDef;
import org.onosproject.yangutils.datamodel.YangUnion;
import org.onosproject.yangutils.datamodel.exceptions.DataModelException;
import org.onosproject.yangutils.datamodel.utils.Parsable;
import org.onosproject.yangutils.linker.impl.YangResolutionInfoImpl;
import org.onosproject.yangutils.parser.antlrgencode.GeneratedYangParser;
import org.onosproject.yangutils.parser.exceptions.ParserException;
import org.onosproject.yangutils.parser.impl.TreeWalkListener;

import static org.onosproject.yangutils.datamodel.utils.DataModelUtils.addResolutionInfo;
import static org.onosproject.yangutils.datamodel.utils.GeneratedLanguage.JAVA_GENERATION;
import static org.onosproject.yangutils.datamodel.utils.ResolvableStatus.UNRESOLVED;
import static org.onosproject.yangutils.datamodel.utils.YangConstructType.TYPE_DATA;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorLocation.ENTRY;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorLocation.EXIT;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorMessageConstruction.constructExtendedListenerErrorMessage;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorMessageConstruction.constructListenerErrorMessage;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorType.INVALID_HOLDER;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorType.MISSING_CURRENT_HOLDER;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorType.MISSING_HOLDER;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorType.UNHANDLED_PARSED_DATA;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerUtil.getValidNodeIdentifier;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerValidation.checkStackIsNotEmpty;
import static org.onosproject.yangutils.translator.tojava.YangDataModelFactory.getYangType;

/*
 * Reference: RFC6020 and YANG ANTLR Grammar
 *
 * ABNF grammar as per RFC6020
 *  type-stmt           = type-keyword sep identifier-ref-arg-str optsep
 *                        (";" /
 *                         "{" stmtsep
 *                            type-body-stmts
 *                         "}")
 *
 * ANTLR grammar rule
 * typeStatement : TYPE_KEYWORD string (STMTEND | LEFT_CURLY_BRACE typeBodyStatements RIGHT_CURLY_BRACE);
 */

/**
 * Represents listener based call back function corresponding to the "type" rule
 * defined in ANTLR grammar file for corresponding ABNF rule in RFC 6020.
 */
public final class TypeListener {

    /**
     * Creates a new type listener.
     */
    private TypeListener() {
    }

    /**
     * It is called when parser receives an input matching the grammar rule
     * (type), performs validation and updates the data model tree.
     *
     * @param listener listener's object
     * @param ctx      context object of the grammar rule
     */
    public static void processTypeEntry(TreeWalkListener listener,
                                        GeneratedYangParser.TypeStatementContext ctx) {

        // Check for stack to be non empty.
        checkStackIsNotEmpty(listener, MISSING_HOLDER, TYPE_DATA, ctx.string().getText(), ENTRY);

        // Validate node identifier.
        YangNodeIdentifier nodeIdentifier = getValidNodeIdentifier(ctx.string().getText(), TYPE_DATA,
                ctx);

        // Obtain the YANG data type.
        YangDataTypes yangDataTypes = YangDataTypes.getType(ctx.string().getText());

        // Create YANG type object and fill the values.
        YangType<?> type = getYangType(JAVA_GENERATION);
        type.setNodeIdentifier(nodeIdentifier);
        type.setDataType(yangDataTypes);

        int errorLine = ctx.getStart().getLine();
        int errorPosition = ctx.getStart().getCharPositionInLine();

        Parsable tmpData = listener.getParsedDataStack().peek();
        switch (tmpData.getYangConstructType()) {
            case LEAF_DATA:
                YangLeaf leaf = (YangLeaf) tmpData;
                leaf.setDataType(type);

                /*
                 * If data type is derived, resolution information to be added
                 * in resolution list.
                 */
                if (yangDataTypes == YangDataTypes.DERIVED) {
                    // Parent YANG node of leaf to be added in resolution information.
                    Parsable leafData = listener.getParsedDataStack().pop();
                    Parsable parentNodeOfLeaf = listener.getParsedDataStack().peek();
                    listener.getParsedDataStack().push(leafData);

                    // Verify parent node of leaf
                    if (!(parentNodeOfLeaf instanceof YangNode)) {
                        throw new ParserException(constructListenerErrorMessage(INVALID_HOLDER, TYPE_DATA,
                                ctx.string().getText(), EXIT));
                    }

                    // Create empty derived info and attach it to type extended info.
                    YangDerivedInfo<?> yangDerivedInfo = new YangDerivedInfo<>();
                    ((YangType<YangDerivedInfo>) type).setDataTypeExtendedInfo(yangDerivedInfo);

                    type.setResolvableStatus(UNRESOLVED);

                    // Add resolution information to the list
                    YangResolutionInfoImpl resolutionInfo = new YangResolutionInfoImpl<YangType>(type,
                            (YangNode) parentNodeOfLeaf, errorLine, errorPosition);
                    addToResolutionList(resolutionInfo, ctx);
                }
                break;
            case LEAF_LIST_DATA:
                YangLeafList leafList = (YangLeafList) tmpData;
                leafList.setDataType(type);

                /*
                 * If data type is derived, resolution information to be added
                 * in resolution list.
                 */
                if (yangDataTypes == YangDataTypes.DERIVED) {
                    // Parent YANG node of leaf list to be added in resolution information.
                    Parsable leafListData = listener.getParsedDataStack().pop();
                    Parsable parentNodeOfLeafList = listener.getParsedDataStack().peek();
                    listener.getParsedDataStack().push(leafListData);

                    // Verify parent node of leaf
                    if (!(parentNodeOfLeafList instanceof YangNode)) {
                        throw new ParserException(constructListenerErrorMessage(INVALID_HOLDER, TYPE_DATA,
                                ctx.string().getText(), EXIT));
                    }

                    // Create empty derived info and attach it to type extended info.
                    YangDerivedInfo<?> yangDerivedInfo = new YangDerivedInfo<>();
                    ((YangType<YangDerivedInfo>) type).setDataTypeExtendedInfo(yangDerivedInfo);

                    // Add resolution information to the list
                    YangResolutionInfoImpl resolutionInfo =
                            new YangResolutionInfoImpl<YangType>(type, (YangNode) parentNodeOfLeafList, errorLine,
                                    errorPosition);
                    addToResolutionList(resolutionInfo, ctx);
                }
                break;
            case UNION_DATA:
                YangUnion unionNode = (YangUnion) tmpData;
                try {
                    unionNode.addType(type);
                } catch (DataModelException e) {
                    ParserException parserException = new ParserException(e.getMessage());
                    parserException.setLine(ctx.getStart().getLine());
                    parserException.setCharPosition(ctx.getStart().getCharPositionInLine());
                    throw parserException;
                }

                /*
                 * If data type is derived, resolution information to be added
                 * in resolution list.
                 */
                if (yangDataTypes == YangDataTypes.DERIVED) {

                    // Create empty derived info and attach it to type extended info.
                    YangDerivedInfo<?> yangDerivedInfo = new YangDerivedInfo<>();
                    ((YangType<YangDerivedInfo>) type).setDataTypeExtendedInfo(yangDerivedInfo);

                    type.setResolvableStatus(UNRESOLVED);

                    // Add resolution information to the list
                    YangResolutionInfoImpl resolutionInfo =
                            new YangResolutionInfoImpl<YangType>(type, unionNode, errorLine, errorPosition);
                    addToResolutionList(resolutionInfo, ctx);
                }

                break;
            case TYPEDEF_DATA:
                /* Prepare the base type info and set in derived type */
                YangTypeDef typeDef = (YangTypeDef) tmpData;
                typeDef.setDataType(type);

                /*
                 * If data type is derived, resolution information to be added
                 * in resolution list.
                 */
                if (yangDataTypes == YangDataTypes.DERIVED) {
                    // Create empty derived info and attach it to type extended info.
                    YangDerivedInfo<?> yangDerivedInfo = new YangDerivedInfo<>();
                    ((YangType<YangDerivedInfo>) type).setDataTypeExtendedInfo(yangDerivedInfo);

                    type.setResolvableStatus(UNRESOLVED);

                    // Add resolution information to the list
                    YangResolutionInfoImpl resolutionInfo =
                            new YangResolutionInfoImpl<YangType>(type, typeDef, errorLine, errorPosition);
                    addToResolutionList(resolutionInfo, ctx);
                }
                break;
            //TODO: deviate replacement statement.

            default:
                throw new ParserException(constructListenerErrorMessage(INVALID_HOLDER, TYPE_DATA,
                        ctx.string().getText(), EXIT));
        }

        // Push the type to the stack.
        listener.getParsedDataStack().push(type);
    }

    /**
     * It is called when parser exits from grammar rule (type), it perform
     * validations and update the data model tree.
     *
     * @param listener Listener's object
     * @param ctx      context object of the grammar rule
     */
    public static void processTypeExit(TreeWalkListener listener,
                                       GeneratedYangParser.TypeStatementContext ctx) {

        // Check for stack to be non empty.
        checkStackIsNotEmpty(listener, MISSING_CURRENT_HOLDER, TYPE_DATA, ctx.string().getText(), EXIT);

        Parsable parsableType = listener.getParsedDataStack().pop();
        if (!(parsableType instanceof YangType)) {
            throw new ParserException(constructListenerErrorMessage(INVALID_HOLDER, TYPE_DATA,
                    ctx.string().getText(), EXIT));
        }
    }

    /**
     * Adds to resolution list.
     *
     * @param resolutionInfo resolution information
     * @param ctx            context object of the grammar rule
     */
    private static void addToResolutionList(YangResolutionInfoImpl<YangType> resolutionInfo,
                                            GeneratedYangParser.TypeStatementContext ctx) {
        try {
            addResolutionInfo(resolutionInfo);
        } catch (DataModelException e) {
            throw new ParserException(constructExtendedListenerErrorMessage(UNHANDLED_PARSED_DATA,
                    TYPE_DATA, ctx.string().getText(), ENTRY, e.getMessage()));
        }
    }
}
