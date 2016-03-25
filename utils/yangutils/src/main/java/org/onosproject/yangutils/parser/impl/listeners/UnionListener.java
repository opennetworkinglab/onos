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
import org.onosproject.yangutils.datamodel.YangType;
import org.onosproject.yangutils.datamodel.YangUnion;
import org.onosproject.yangutils.parser.Parsable;
import org.onosproject.yangutils.parser.antlrgencode.GeneratedYangParser;
import org.onosproject.yangutils.parser.exceptions.ParserException;
import org.onosproject.yangutils.parser.impl.TreeWalkListener;

import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorLocation.ENTRY;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorLocation.EXIT;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorMessageConstruction.constructListenerErrorMessage;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorType.INVALID_HOLDER;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorType.MISSING_CURRENT_HOLDER;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorType.MISSING_HOLDER;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerValidation.checkStackIsNotEmpty;
import static org.onosproject.yangutils.utils.YangConstructType.TYPE_DATA;
import static org.onosproject.yangutils.utils.YangConstructType.UNION_DATA;

/**
 * Implements listener based call back function corresponding to the "union" rule
 * defined in ANTLR grammar file for corresponding ABNF rule in RFC 6020.
 */
public final class UnionListener {
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
            YangUnion unionNode = new YangUnion();
            Parsable typeData = listener.getParsedDataStack().pop();

            // Check for stack to be non empty.
            checkStackIsNotEmpty(listener, MISSING_HOLDER, UNION_DATA, "", ENTRY);

            Parsable tmpData = listener.getParsedDataStack().peek();

            switch (tmpData.getYangConstructType()) {
                case LEAF_DATA:
                    unionNode.setUnionName(((YangLeaf) tmpData).getLeafName());
                    break;
                case LEAF_LIST_DATA:
                    unionNode.setUnionName(((YangLeafList) tmpData).getLeafName());
                    break;
                case UNION_DATA:
                    unionNode.setUnionName(((YangUnion) tmpData).getUnionName());
                    break;
                // TODO typedef, deviate.
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
}