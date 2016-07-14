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
 * decimal64-specification = fraction-digits-stmt
 *
 * fraction-digits-stmt = fraction-digits-keyword sep
 *                        fraction-digits-arg-str stmtend
 *
 * fraction-digits-arg-str = < a string that matches the rule
 *                             fraction-digits-arg >
 *
 * fraction-digits-arg = ("1" ["0" / "1" / "2" / "3" / "4" /
 *                              "5" / "6" / "7" / "8"])
 *                       / "2" / "3" / "4" / "5" / "6" / "7" / "8" / "9"
 *
 * ANTLR grammar rule
 * decimal64Specification : FRACTION_DIGITS_KEYWORD fraction STMTEND;
 *
 * fraction : string;
 */

import org.onosproject.yangutils.datamodel.YangDecimal64;
import org.onosproject.yangutils.datamodel.YangRangeRestriction;
import org.onosproject.yangutils.datamodel.YangType;
import org.onosproject.yangutils.datamodel.utils.Parsable;
import org.onosproject.yangutils.parser.antlrgencode.GeneratedYangParser;
import org.onosproject.yangutils.parser.exceptions.ParserException;
import org.onosproject.yangutils.parser.impl.TreeWalkListener;

import static org.onosproject.yangutils.datamodel.utils.YangConstructType.FRACTION_DIGITS_DATA;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorLocation.ENTRY;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorMessageConstruction.constructListenerErrorMessage;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorType.INVALID_HOLDER;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorType.MISSING_HOLDER;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerValidation.checkStackIsNotEmpty;

/**
 * Represents listener based call back function corresponding to the "fraction-digits"
 * rule defined in ANTLR grammar file for corresponding ABNF rule in RFC 6020.
 */
public final class FractionDigitsListener {

    /**
     * Creates a new bit listener.
     */
    private FractionDigitsListener() {
    }

    /**
     * It is called when parser enters grammar rule (fraction-digits), it perform
     * validations and updates the data model tree.
     *
     * @param listener listener's object
     * @param ctx context object of the grammar rule
     */
    public static void processFractionDigitsEntry(TreeWalkListener listener,
                                        GeneratedYangParser.FractionDigitStatementContext ctx) {

        // Check for stack to be non empty.
        checkStackIsNotEmpty(listener, MISSING_HOLDER, FRACTION_DIGITS_DATA, ctx.fraction().getText(), ENTRY);

        int value = getValidFractionDigits(ctx);

        Parsable tmpNode = listener.getParsedDataStack().peek();
        if (tmpNode instanceof YangType) {
            YangType<YangDecimal64<YangRangeRestriction>> typeNode =
                    (YangType<YangDecimal64<YangRangeRestriction>>) tmpNode;
            YangDecimal64 decimal64Node = typeNode.getDataTypeExtendedInfo();
            decimal64Node.setFractionDigit(value);
        } else {
            throw new ParserException(constructListenerErrorMessage(INVALID_HOLDER, FRACTION_DIGITS_DATA,
                                                                    ctx.fraction().getText(), ENTRY));
        }
    }

    /**
     * Validate fraction digits.
     *
     * @param ctx context object of the grammar rule
     * @return validated fraction-digits
     */
    public static int getValidFractionDigits(GeneratedYangParser.FractionDigitStatementContext ctx) {
        String value = ctx.fraction().getText().trim();
        ParserException parserException;

        int fractionDigits = Integer.parseInt(value);
        if ((fractionDigits >= YangDecimal64.MIN_FRACTION_DIGITS_VALUE) &&
                (fractionDigits <= YangDecimal64.MAX_FRACTION_DIGITS_VALUE)) {
            return fractionDigits;
        } else {
            parserException =
                    new ParserException("YANG file error : fraction-digits value should be between 1 and 18.");
            parserException.setLine(ctx.getStart().getLine());
            parserException.setCharPosition(ctx.getStart().getCharPositionInLine());
            throw parserException;
        }
    }
}
