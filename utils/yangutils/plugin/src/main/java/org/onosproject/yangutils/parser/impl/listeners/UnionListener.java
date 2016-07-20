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
 * type-body-stmts     = numerical-restrictions /
 *                       decimal64-specification /
 *                      string-restrictions /
 *                       enum-specification /
 *                       leafref-specification /
 *                       identityref-specification /
 *                       instance-identifier-specification /
 *                       bits-specification /
 *                       union-specification
 *
 * union-specification = 1*(type-stmt stmtsep)
 *
 * ANTLR grammar rule
 * typeBodyStatements : numericalRestrictions | stringRestrictions | enumSpecification
 *                 | leafrefSpecification | identityrefSpecification | instanceIdentifierSpecification
 *                 | bitsSpecification | unionSpecification;
 *
 * unionSpecification : typeStatement+;
 */

import org.onosproject.yangutils.datamodel.YangLeaf;
import org.onosproject.yangutils.datamodel.YangLeafList;
import org.onosproject.yangutils.datamodel.YangNode;
import org.onosproject.yangutils.datamodel.YangType;
import org.onosproject.yangutils.datamodel.YangTypeDef;
import org.onosproject.yangutils.datamodel.YangUnion;
import org.onosproject.yangutils.datamodel.exceptions.DataModelException;
import org.onosproject.yangutils.datamodel.utils.Parsable;
import org.onosproject.yangutils.parser.antlrgencode.GeneratedYangParser;
import org.onosproject.yangutils.parser.exceptions.ParserException;
import org.onosproject.yangutils.parser.impl.TreeWalkListener;

import static org.onosproject.yangutils.datamodel.utils.GeneratedLanguage.JAVA_GENERATION;
import static org.onosproject.yangutils.datamodel.utils.YangConstructType.TYPE_DATA;
import static org.onosproject.yangutils.datamodel.utils.YangConstructType.UNION_DATA;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorLocation.ENTRY;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorLocation.EXIT;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorMessageConstruction.constructExtendedListenerErrorMessage;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorMessageConstruction.constructListenerErrorMessage;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorType.INVALID_HOLDER;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorType.MISSING_CURRENT_HOLDER;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorType.MISSING_HOLDER;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorType.UNHANDLED_PARSED_DATA;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerValidation.checkStackIsNotEmpty;
import static org.onosproject.yangutils.translator.tojava.YangDataModelFactory.getYangUnionNode;

/**
 * Represents listener based call back function corresponding to the "union" rule
 * defined in ANTLR grammar file for corresponding ABNF rule in RFC 6020.
 */
public final class UnionListener {

    /**
     * Suffix to be used while creating union class.
     */
    private static final String UNION_CLASS_SUFFIX = "_union";

    /**
     * Creates a new union listener.
     */
    private UnionListener() {
    }

    /**
     * It is called when parser enters grammar rule (union), it perform
     * validations and updates the data model tree.
     *
     * @param listener listener's object
     * @param ctx      context object of the grammar rule
     */
    public static void processUnionEntry(TreeWalkListener listener,
                                         GeneratedYangParser.UnionSpecificationContext ctx) {

        // Check for stack to be non empty.
        checkStackIsNotEmpty(listener, MISSING_HOLDER, UNION_DATA, "", ENTRY);

        if (listener.getParsedDataStack().peek() instanceof YangType) {
            YangUnion unionNode = getYangUnionNode(JAVA_GENERATION);
            Parsable typeData = listener.getParsedDataStack().pop();

            // Check for stack to be non empty.
            checkStackIsNotEmpty(listener, MISSING_HOLDER, UNION_DATA, "", ENTRY);

            Parsable tmpData = listener.getParsedDataStack().peek();

            switch (tmpData.getYangConstructType()) {
                case LEAF_DATA:
                    // Set the name of union same as leaf.
                    unionNode.setName(((YangLeaf) tmpData).getName() + UNION_CLASS_SUFFIX);
                    // Pop the stack entry to obtain the parent YANG node.
                    Parsable leaf = listener.getParsedDataStack().pop();
                    // Add the union node to the parent holder of leaf.
                    addChildToParentNode(listener, unionNode);
                    // Push the popped entry back to the stack.
                    listener.getParsedDataStack().push(leaf);
                    break;
                case LEAF_LIST_DATA:
                    // Set the name of union same as leaf list.
                    unionNode.setName(((YangLeafList) tmpData).getName() + UNION_CLASS_SUFFIX);
                    // Pop the stack entry to obtain the parent YANG node.
                    Parsable leafList = listener.getParsedDataStack().pop();
                    // Add the union node to the parent holder of leaf.
                    addChildToParentNode(listener, unionNode);
                    // Push the popped entry back to the stack.
                    listener.getParsedDataStack().push(leafList);
                    break;
                case UNION_DATA:
                    YangUnion parentUnion = (YangUnion) tmpData;
                    /*
                     * In case parent of union is again a union, name of the
                     * child union is parent union name suffixed with running
                     * integer number, this is done because under union there
                     * could be multiple child union types.
                     */
                    unionNode.setName(parentUnion.getName() + UNION_CLASS_SUFFIX + parentUnion.getChildUnionNumber());
                    // Increment the running number.
                    parentUnion.setChildUnionNumber(parentUnion.getChildUnionNumber() + 1);
                    // Add union as a child to parent union.
                    addChildToParentNode(listener, unionNode);
                    break;
                case TYPEDEF_DATA:
                    YangTypeDef typeDef = (YangTypeDef) tmpData;
                    // Set the name of union same as typedef name.
                    unionNode.setName(typeDef.getName() + UNION_CLASS_SUFFIX);
                    // Add union as a child to parent type def.
                    addChildToParentNode(listener, unionNode);
                    break;
                // TODO deviate.
                default:
                    throw new ParserException(constructListenerErrorMessage(INVALID_HOLDER, TYPE_DATA,
                            ((YangType<?>) typeData).getDataTypeName(), ENTRY));
            }
            listener.getParsedDataStack().push(typeData);
            listener.getParsedDataStack().push(unionNode);
        } else {
            throw new ParserException(constructListenerErrorMessage(INVALID_HOLDER, UNION_DATA, "", ENTRY));
        }
    }

    /**
     * It is called when parser exits from grammar rule (union), it perform
     * validations and update the data model tree.
     *
     * @param listener Listener's object
     * @param ctx      context object of the grammar rule
     */
    public static void processUnionExit(TreeWalkListener listener,
                                       GeneratedYangParser.UnionSpecificationContext ctx) {

        // Check for stack to be non empty.
        checkStackIsNotEmpty(listener, MISSING_HOLDER, UNION_DATA, "", EXIT);

        Parsable tmpUnionNode = listener.getParsedDataStack().peek();
        if (tmpUnionNode instanceof YangUnion) {
            YangUnion unionNode = (YangUnion) tmpUnionNode;
            listener.getParsedDataStack().pop();

            // Check for stack to be non empty.
            checkStackIsNotEmpty(listener, MISSING_HOLDER, UNION_DATA, "", EXIT);

            Parsable tmpNode = listener.getParsedDataStack().peek();
            switch (tmpNode.getYangConstructType()) {
                case TYPE_DATA: {
                    YangType<YangUnion> typeNode = (YangType<YangUnion>) tmpNode;
                    typeNode.setDataTypeExtendedInfo(unionNode);
                    break;
                }
                default:
                    throw new ParserException(
                            constructListenerErrorMessage(INVALID_HOLDER, UNION_DATA, "", EXIT));
            }
        } else {
            throw new ParserException(
                    constructListenerErrorMessage(MISSING_CURRENT_HOLDER, UNION_DATA, "", EXIT));
        }
    }

    /**
     * Adds the union node to the parent holder.
     *
     * @param listener listener's object
     * @param unionNode union node which needs to be added to parent
     */
    private static void addChildToParentNode(TreeWalkListener listener, YangUnion unionNode) {
        if (!(listener.getParsedDataStack().peek() instanceof YangNode)) {
            throw new ParserException(constructListenerErrorMessage(INVALID_HOLDER, UNION_DATA,
                    "", ENTRY));
        } else {
            YangNode curNode = (YangNode) listener.getParsedDataStack().peek();
            try {
                curNode.addChild(unionNode);
            } catch (DataModelException e) {
                throw new ParserException(constructExtendedListenerErrorMessage(UNHANDLED_PARSED_DATA,
                        UNION_DATA, "", ENTRY, e.getMessage()));
            }
        }
    }
}
