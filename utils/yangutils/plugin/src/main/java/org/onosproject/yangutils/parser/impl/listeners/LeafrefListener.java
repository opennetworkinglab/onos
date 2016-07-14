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

import org.onosproject.yangutils.datamodel.YangLeaf;
import org.onosproject.yangutils.datamodel.YangLeafList;
import org.onosproject.yangutils.datamodel.YangLeafRef;
import org.onosproject.yangutils.datamodel.YangNode;
import org.onosproject.yangutils.datamodel.YangType;
import org.onosproject.yangutils.datamodel.exceptions.DataModelException;
import org.onosproject.yangutils.datamodel.utils.Parsable;
import org.onosproject.yangutils.linker.impl.YangResolutionInfoImpl;
import org.onosproject.yangutils.parser.antlrgencode.GeneratedYangParser;
import org.onosproject.yangutils.parser.exceptions.ParserException;
import org.onosproject.yangutils.parser.impl.TreeWalkListener;

import static org.onosproject.yangutils.datamodel.utils.DataModelUtils.addResolutionInfo;
import static org.onosproject.yangutils.datamodel.utils.ResolvableStatus.UNRESOLVED;
import static org.onosproject.yangutils.datamodel.utils.YangConstructType.LEAFREF_DATA;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorLocation.ENTRY;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorLocation.EXIT;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorMessageConstruction.constructExtendedListenerErrorMessage;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorMessageConstruction.constructListenerErrorMessage;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorType.INVALID_HOLDER;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorType.MISSING_CURRENT_HOLDER;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorType.MISSING_HOLDER;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorType.UNHANDLED_PARSED_DATA;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerValidation.checkStackIsNotEmpty;

/*
 * Reference: RFC6020 and YANG ANTLR Grammar
 *
 * ABNF grammar as per RFC6020
 * type-body-stmts     = numerical-restrictions /
 *                       decimal64-specification /
 *                       string-restrictions /
 *                       enum-specification /
 *                       leafref-specification /
 *                       identityref-specification /
 *                       instance-identifier-specification /
 *                       bits-specification /
 *                       union-specification
 *
 * leafref-specification =
 *                         ;; these stmts can appear in any order
 *                        path-stmt stmtsep
 *                        [require-instance-stmt stmtsep]
 *
 * ANTLR grammar rule
 *
 * typeBodyStatements : numericalRestrictions | stringRestrictions | enumSpecification
 *                 | leafrefSpecification | identityrefSpecification | instanceIdentifierSpecification
 *                 | bitsSpecification | unionSpecification;
 *
 * leafrefSpecification : (pathStatement (requireInstanceStatement)?) | ((requireInstanceStatement)? pathStatement);
 */

/**
 * Represents listener based call back function corresponding to the
 * "leafref" rule defined in ANTLR grammar file for corresponding ABNF rule
 * in RFC 6020.
 */
public final class LeafrefListener {

    /**
     * Creates a new leafref listener.
     */
    private LeafrefListener() {
    }

    /**
     * It is called when parser receives an input matching the grammar rule
     * (leafref), perform validations and updates the data model tree.
     *
     * @param listener listener's object
     * @param ctx context object of the grammar rule
     */
    public static void processLeafrefEntry(TreeWalkListener listener,
            GeneratedYangParser.LeafrefSpecificationContext ctx) {

        // Check for stack to be non empty.
        checkStackIsNotEmpty(listener, MISSING_HOLDER, LEAFREF_DATA, "", ENTRY);

        int errorLine = ctx.getStart().getLine();
        int errorPosition = ctx.getStart().getCharPositionInLine();

        YangLeafRef<?> leafRef = new YangLeafRef<>();

        leafRef.setLineNumber(errorLine);
        leafRef.setCharPosition(errorPosition);
        Parsable typeData = listener.getParsedDataStack().pop();

        if (!(typeData instanceof YangType)) {
            throw new ParserException(constructListenerErrorMessage(INVALID_HOLDER, LEAFREF_DATA,
                    "", ENTRY));
        }

        YangType type = (YangType) typeData;
        type.setDataTypeExtendedInfo(leafRef);

        // Setting by default the value of require-instance as true.
        leafRef.setRequireInstance(true);
        Parsable tmpData = listener.getParsedDataStack().peek();

        switch (tmpData.getYangConstructType()) {

            case LEAF_DATA:

                // Parent YANG node of leaf to be added in resolution information.
                YangLeaf leaf = (YangLeaf) listener.getParsedDataStack().pop();
                Parsable parentNodeOfLeaf = listener.getParsedDataStack().peek();
                listener.getParsedDataStack().push(leaf);

                // Verify parent node of leaf.
                if (!(parentNodeOfLeaf instanceof YangNode)) {
                    throw new ParserException(constructListenerErrorMessage(INVALID_HOLDER, LEAFREF_DATA,
                            "", ENTRY));
                }

                leafRef.setResolvableStatus(UNRESOLVED);
                leafRef.setParentNodeOfLeafref((YangNode) parentNodeOfLeaf);
                if (listener.getGroupingDepth() == 0) {
                    // Add resolution information to the list.
                    YangResolutionInfoImpl resolutionInfo = new YangResolutionInfoImpl<YangLeafRef>(leafRef,
                            (YangNode) parentNodeOfLeaf, errorLine, errorPosition);
                    addToResolutionList(resolutionInfo);
                }
                break;

            case LEAF_LIST_DATA:

                // Parent YANG node of leaf-list to be added in resolution information.
                YangLeafList leafList = (YangLeafList) listener.getParsedDataStack().pop();
                Parsable parentNodeOfLeafList = listener.getParsedDataStack().peek();
                listener.getParsedDataStack().push(leafList);

                // Verify parent node of leaf-list.
                if (!(parentNodeOfLeafList instanceof YangNode)) {
                    throw new ParserException(constructListenerErrorMessage(INVALID_HOLDER, LEAFREF_DATA,
                            "", ENTRY));
                }

                leafRef.setResolvableStatus(UNRESOLVED);
                leafRef.setParentNodeOfLeafref((YangNode) parentNodeOfLeafList);

                if (listener.getGroupingDepth() == 0) {
                    // Add resolution information to the list.
                    YangResolutionInfoImpl resolutionInfoImpl = new YangResolutionInfoImpl<YangLeafRef>(leafRef,
                            (YangNode) parentNodeOfLeafList, errorLine, errorPosition);
                    addToResolutionList(resolutionInfoImpl);
                }
                break;

            case TYPEDEF_DATA:
                Parsable parentNodeOfLeafref = listener.getParsedDataStack().peek();
                leafRef.setParentNodeOfLeafref((YangNode) parentNodeOfLeafref);
                /*
                 * Do not add the leaf ref to resolution list. It needs to be
                 * added to resolution list, when leaf/leaf list references to
                 * this typedef. At this time that leaf/leaf-list becomes the
                 * parent for the leafref.
                 */
                break;

            default:
                throw new ParserException(constructListenerErrorMessage(INVALID_HOLDER, LEAFREF_DATA,
                        "", ENTRY));
        }
        listener.getParsedDataStack().push(typeData);
        listener.getParsedDataStack().push(leafRef);
    }

    /**
     * It is called when parser exits from grammar rule (leafref), it performs
     * validation and updates the data model tree.
     *
     * @param listener listener's object
     * @param ctx context object of the grammar rule
     */
    public static void processLeafrefExit(TreeWalkListener listener,
            GeneratedYangParser.LeafrefSpecificationContext ctx) {

        // Check for stack to be non empty.
        checkStackIsNotEmpty(listener, MISSING_CURRENT_HOLDER, LEAFREF_DATA, "", EXIT);

        Parsable parsableType = listener.getParsedDataStack().pop();
        if (!(parsableType instanceof YangLeafRef)) {
            throw new ParserException(constructListenerErrorMessage(INVALID_HOLDER, LEAFREF_DATA,
                    "", EXIT));
        }
    }

    /**
     * Adds to resolution list.
     *
     * @param resolutionInfo resolution information
     */
    private static void addToResolutionList(YangResolutionInfoImpl resolutionInfo) {

        try {
            addResolutionInfo(resolutionInfo);
        } catch (DataModelException e) {
            throw new ParserException(constructExtendedListenerErrorMessage(UNHANDLED_PARSED_DATA,
                    LEAFREF_DATA, "", ENTRY, e.getMessage()));
        }
    }
}
