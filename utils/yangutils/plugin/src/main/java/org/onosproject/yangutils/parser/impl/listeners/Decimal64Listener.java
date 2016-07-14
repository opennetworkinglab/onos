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
 *                       string-restrictions /
 *                       enum-specification /
 *                       leafref-specification /
 *                       identityref-specification /
 *                       instance-identifier-specification /
 *                       bits-specification /
 *                       union-specification
 *
 * decimal64-specification = fraction-digits-stmt [range-stmt stmtsep]
 *
 * fraction-digits-stmt = fraction-digits-keyword sep
 *                         fraction-digits-arg-str stmtend
 *
 * fraction-digits-arg-str = < a string that matches the rule
 *                             fraction-digits-arg >
 *
 * fraction-digits-arg = ("1" ["0" / "1" / "2" / "3" / "4" /
 *                              "5" / "6" / "7" / "8"])
 *                        / "2" / "3" / "4" / "5" / "6" / "7" / "8" / "9"
 *
 * range-stmt          = range-keyword sep range-arg-str optsep
 *                        (";" /
 *                         "{" stmtsep
 *                             ;; these stmts can appear in any order
 *                             [error-message-stmt stmtsep]
 *                             [error-app-tag-stmt stmtsep]
 *                             [description-stmt stmtsep]
 *                             [reference-stmt stmtsep]
 *                          "}")
 * ANTLR grammar rule
 *
 * typeBodyStatements : numericalRestrictions | decimal64Specification | stringRestrictions | enumSpecification
 *                     | leafrefSpecification | identityrefSpecification | instanceIdentifierSpecification
 *                     | bitsSpecification | unionSpecification;
 *
 * decimal64Specification : fractionDigitStatement rangeStatement?;
 *
 * fractionDigitStatement : FRACTION_DIGITS_KEYWORD fraction STMTEND;
 *
 * fraction : string;
 */

import org.onosproject.yangutils.datamodel.YangDecimal64;
import org.onosproject.yangutils.datamodel.YangRangeRestriction;
import org.onosproject.yangutils.datamodel.YangType;
import org.onosproject.yangutils.datamodel.exceptions.DataModelException;
import org.onosproject.yangutils.datamodel.utils.Parsable;
import org.onosproject.yangutils.parser.antlrgencode.GeneratedYangParser;
import org.onosproject.yangutils.parser.exceptions.ParserException;
import org.onosproject.yangutils.parser.impl.TreeWalkListener;

import static org.onosproject.yangutils.datamodel.utils.YangConstructType.DECIMAL64_DATA;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorLocation.ENTRY;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorLocation.EXIT;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorMessageConstruction.constructListenerErrorMessage;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorType.INVALID_HOLDER;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorType.MISSING_HOLDER;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerValidation.checkStackIsNotEmpty;

/**
 * Represents listener based call back function corresponding to the "decimal64" rule
 * defined in ANTLR grammar file for corresponding ABNF rule in RFC 6020.
 */
public final class Decimal64Listener {

    /**
     * Creates a new Decimal64 listener.
     */
    private Decimal64Listener() {
    }

    /**
     * It is called when parser enters grammar rule (decimal64), it perform
     * validations and updates the data model tree.
     *
     * @param listener listener's object
     * @param ctx context object of the grammar rule
     */
    public static void processDecimal64Entry(TreeWalkListener listener,
            GeneratedYangParser.Decimal64SpecificationContext ctx) {

        // Check for stack to be non empty.
        checkStackIsNotEmpty(listener, MISSING_HOLDER, DECIMAL64_DATA, "", ENTRY);

        Parsable tmpNode = listener.getParsedDataStack().peek();
        if (tmpNode instanceof YangType) {
            YangType<YangDecimal64<YangRangeRestriction>> typeNode =
                    (YangType<YangDecimal64<YangRangeRestriction>>) tmpNode;
            YangDecimal64 decimal64Node = new YangDecimal64();
            typeNode.setDataTypeExtendedInfo(decimal64Node);
        } else {
            throw new ParserException(constructListenerErrorMessage(INVALID_HOLDER, DECIMAL64_DATA, "", ENTRY));
        }
    }

    /**
     * Performs validation and updates the data model tree.
     * It is called when parser exits from grammar rule (decimal64).
     *
     * @param listener listener's object
     * @param ctx      context object of the grammar rule
     */
    public static void processDecimal64Exit(TreeWalkListener listener,
                                            GeneratedYangParser.Decimal64SpecificationContext ctx) {

        // Check for stack to be non empty.
        checkStackIsNotEmpty(listener, MISSING_HOLDER, DECIMAL64_DATA, "", EXIT);

        Parsable tmpNode = listener.getParsedDataStack().peek();
        if (tmpNode instanceof YangType) {
            YangType<YangDecimal64<YangRangeRestriction>> typeNode =
                    (YangType<YangDecimal64<YangRangeRestriction>>) tmpNode;
            YangDecimal64<YangRangeRestriction> decimal64Node = typeNode.getDataTypeExtendedInfo();
            try {
                decimal64Node.validateRange();
            } catch (DataModelException e) {
                throw new ParserException(e);
            }
        } else {
            throw new ParserException(constructListenerErrorMessage(INVALID_HOLDER, DECIMAL64_DATA, "", EXIT));
        }
    }
}