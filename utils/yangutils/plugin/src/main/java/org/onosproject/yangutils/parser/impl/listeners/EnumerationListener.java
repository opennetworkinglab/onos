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
 * enum-specification  = 1*(enum-stmt stmtsep)
 *
 * ANTLR grammar rule
 *
 * typeBodyStatements : numericalRestrictions | stringRestrictions | enumSpecification
 *                 | leafrefSpecification | identityrefSpecification | instanceIdentifierSpecification
 *                 | bitsSpecification | unionSpecification;
 *
 * enumSpecification : enumStatement+;
 */

import org.onosproject.yangutils.datamodel.YangEnumeration;
import org.onosproject.yangutils.datamodel.YangLeaf;
import org.onosproject.yangutils.datamodel.YangLeafList;
import org.onosproject.yangutils.datamodel.YangNode;
import org.onosproject.yangutils.datamodel.YangType;
import org.onosproject.yangutils.datamodel.YangTypeDef;
import org.onosproject.yangutils.datamodel.YangUnion;
import org.onosproject.yangutils.datamodel.exceptions.DataModelException;
import org.onosproject.yangutils.datamodel.utils.Parsable;
import org.onosproject.yangutils.datamodel.utils.YangConstructType;
import org.onosproject.yangutils.parser.antlrgencode.GeneratedYangParser;
import org.onosproject.yangutils.parser.exceptions.ParserException;
import org.onosproject.yangutils.parser.impl.TreeWalkListener;

import static org.onosproject.yangutils.datamodel.utils.GeneratedLanguage.JAVA_GENERATION;
import static org.onosproject.yangutils.datamodel.utils.YangConstructType.ENUMERATION_DATA;
import static org.onosproject.yangutils.datamodel.utils.YangConstructType.TYPE_DATA;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorLocation.ENTRY;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorLocation.EXIT;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorMessageConstruction.constructExtendedListenerErrorMessage;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorMessageConstruction.constructListenerErrorMessage;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorType.INVALID_HOLDER;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorType.MISSING_CURRENT_HOLDER;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorType.MISSING_HOLDER;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorType.UNHANDLED_PARSED_DATA;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerValidation.checkStackIsNotEmpty;
import static org.onosproject.yangutils.translator.tojava.YangDataModelFactory.getYangEnumerationNode;

/**
 * Represents listener based call back function corresponding to the
 * "enumeration" rule defined in ANTLR grammar file for corresponding ABNF rule
 * in RFC 6020.
 */
public final class EnumerationListener {

    /**
     * Suffix to be used while creating enumeration class.
     */
    private static final String ENUMERATION_CLASS_SUFFIX = "_enum";

    /**
     * Creates a new enumeration listener.
     */
    private EnumerationListener() {
    }

    /**
     * It is called when parser enters grammar rule (enumeration), it perform
     * validations and updates the data model tree.
     *
     * @param listener listener's object
     * @param ctx context object of the grammar rule
     */
    public static void processEnumerationEntry(TreeWalkListener listener,
            GeneratedYangParser.EnumSpecificationContext ctx) {

        // Check for stack to be non empty.
        checkStackIsNotEmpty(listener, MISSING_HOLDER, ENUMERATION_DATA, "", ENTRY);

        if (listener.getParsedDataStack().peek() instanceof YangType) {
            YangEnumeration enumerationNode = getYangEnumerationNode(JAVA_GENERATION);
            Parsable typeData = listener.getParsedDataStack().pop();

            // Check for stack to be non empty.
            checkStackIsNotEmpty(listener, MISSING_HOLDER, ENUMERATION_DATA, "", ENTRY);

            Parsable tmpData = listener.getParsedDataStack().peek();

            switch (tmpData.getYangConstructType()) {
                case LEAF_DATA:
                    // Set the name of enumeration same as leaf.
                    enumerationNode.setName(((YangLeaf) tmpData).getName() + ENUMERATION_CLASS_SUFFIX);
                    // Pop the stack entry to obtain the parent YANG node.
                    Parsable leaf = listener.getParsedDataStack().pop();
                    // Add the enumeration node to the parent holder of leaf.
                    addChildToParentNode(listener, enumerationNode);
                    // Push the popped entry back to the stack.
                    listener.getParsedDataStack().push(leaf);
                    break;
                case LEAF_LIST_DATA:
                    // Set the name of enumeration same as leaf list.
                    enumerationNode.setName(((YangLeafList) tmpData).getName() + ENUMERATION_CLASS_SUFFIX);
                    // Pop the stack entry to obtain the parent YANG node.
                    Parsable leafList = listener.getParsedDataStack().pop();
                    // Add the enumeration node to the parent holder of leaf.
                    addChildToParentNode(listener, enumerationNode);
                    // Push the popped entry back to the stack.
                    listener.getParsedDataStack().push(leafList);
                    break;
                case UNION_DATA:
                    YangUnion yangUnion = (YangUnion) tmpData;
                    /*
                     * In case parent of enumeration is a union, name of the
                     * enumeration is parent union name suffixed with running
                     * integer number, this is done because under union there
                     * could be multiple child union types.
                     */
                    enumerationNode.setName(yangUnion.getName() + ENUMERATION_CLASS_SUFFIX
                            + yangUnion.getChildUnionNumber());
                    // Increment the running number.
                    yangUnion.setChildUnionNumber(yangUnion.getChildUnionNumber() + 1);
                    // Add union as a child to parent union.
                    addChildToParentNode(listener, enumerationNode);
                    break;
                case TYPEDEF_DATA:
                    YangTypeDef typeDef = (YangTypeDef) tmpData;
                    // Set the name of enumeration same as typedef name.
                    enumerationNode.setName(typeDef.getName() + ENUMERATION_CLASS_SUFFIX);
                    // Add enumeration as a child to parent type def.
                    addChildToParentNode(listener, enumerationNode);
                    break;
                // TODO deviate.
                default:
                    throw new ParserException(constructListenerErrorMessage(INVALID_HOLDER, TYPE_DATA,
                            ((YangType<?>) typeData).getDataTypeName(), ENTRY));
            }
            listener.getParsedDataStack().push(typeData);
            listener.getParsedDataStack().push(enumerationNode);
        } else {
            throw new ParserException(constructListenerErrorMessage(INVALID_HOLDER, ENUMERATION_DATA, "", ENTRY));
        }
    }

    /**
     * It is called when parser exits from grammar rule (enumeration), it
     * perform validations and update the data model tree.
     *
     * @param listener Listener's object
     * @param ctx context object of the grammar rule
     */
    public static void processEnumerationExit(TreeWalkListener listener,
            GeneratedYangParser.EnumSpecificationContext ctx) {

        // Check for stack to be non empty.
        checkStackIsNotEmpty(listener, MISSING_HOLDER, ENUMERATION_DATA, "", EXIT);

        Parsable tmpEnumerationNode = listener.getParsedDataStack().peek();
        if (tmpEnumerationNode instanceof YangEnumeration) {
            YangEnumeration enumerationNode = (YangEnumeration) tmpEnumerationNode;
            listener.getParsedDataStack().pop();

            // Check for stack to be non empty.
            checkStackIsNotEmpty(listener, MISSING_HOLDER, ENUMERATION_DATA, "", EXIT);

            Parsable tmpNode = listener.getParsedDataStack().peek();
            switch (tmpNode.getYangConstructType()) {
                case TYPE_DATA: {
                    YangType<YangEnumeration> typeNode = (YangType<YangEnumeration>) tmpNode;
                    typeNode.setDataTypeExtendedInfo(enumerationNode);
                    break;
                }
                default:
                    throw new ParserException(
                            constructListenerErrorMessage(INVALID_HOLDER, ENUMERATION_DATA, "", EXIT));
            }
        } else {
            throw new ParserException(
                    constructListenerErrorMessage(MISSING_CURRENT_HOLDER, ENUMERATION_DATA, "", EXIT));
        }
    }

    /**
     * Adds the enumeration node to the parent holder.
     *
     * @param listener listener's object
     * @param enumerationNode enumeration node which needs to be added to parent
     */
    private static void addChildToParentNode(TreeWalkListener listener, YangEnumeration enumerationNode) {
        if (!(listener.getParsedDataStack().peek() instanceof YangNode)) {
            throw new ParserException(constructListenerErrorMessage(INVALID_HOLDER, ENUMERATION_DATA,
                    "", ENTRY));
        } else {
            YangNode curNode = (YangNode) listener.getParsedDataStack().peek();
            try {
                curNode.addChild(enumerationNode);
            } catch (DataModelException e) {
                throw new ParserException(constructExtendedListenerErrorMessage(UNHANDLED_PARSED_DATA,
                        YangConstructType.ENUMERATION_DATA, "", ENTRY, e.getMessage()));
            }
        }
    }
}
