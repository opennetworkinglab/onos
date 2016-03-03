/*
 * Copyright 2014-2016 Open Networking Laboratory
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
import org.onosproject.yangutils.datamodel.YangType;
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
import static org.onosproject.yangutils.utils.YangConstructType.ENUMERATION_DATA;
import static org.onosproject.yangutils.utils.YangConstructType.TYPE_DATA;

/**
 * Implements listener based call back function corresponding to the
 * "enumeration" rule defined in ANTLR grammar file for corresponding ABNF rule
 * in RFC 6020.
 */
public final class EnumerationListener {

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
            YangEnumeration enumerationNode = new YangEnumeration();
            Parsable typeData = listener.getParsedDataStack().pop();

            // Check for stack to be non empty.
            checkStackIsNotEmpty(listener, MISSING_HOLDER, ENUMERATION_DATA, "", ENTRY);

            Parsable tmpData = listener.getParsedDataStack().peek();

            switch (tmpData.getYangConstructType()) {
                case LEAF_DATA:
                    enumerationNode.setEnumerationName(((YangLeaf) tmpData).getLeafName());
                    break;
                case LEAF_LIST_DATA:
                    enumerationNode.setEnumerationName(((YangLeafList) tmpData).getLeafName());
                    break;
                // TODO typedef, union, deviate.
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
}
