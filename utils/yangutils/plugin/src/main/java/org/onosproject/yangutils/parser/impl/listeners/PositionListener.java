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
 * position-stmt       = position-keyword sep
 *                       position-value-arg-str stmtend
 * position-value-arg-str = < a string that matches the rule
 *                            position-value-arg >
 * position-value-arg  = non-negative-integer-value
 * non-negative-integer-value = "0" / positive-integer-value
 * positive-integer-value = (non-zero-digit *DIGIT)
 * zero-integer-value  = 1*DIGIT
 *
 * ANTLR grammar rule
 * positionStatement : POSITION_KEYWORD position STMTEND;
 * position          : string;
 */

import org.onosproject.yangutils.datamodel.YangBit;
import org.onosproject.yangutils.datamodel.YangBits;
import org.onosproject.yangutils.datamodel.utils.Parsable;
import org.onosproject.yangutils.parser.antlrgencode.GeneratedYangParser;
import org.onosproject.yangutils.parser.exceptions.ParserException;
import org.onosproject.yangutils.parser.impl.TreeWalkListener;

import static org.onosproject.yangutils.datamodel.utils.YangConstructType.POSITION_DATA;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorLocation.ENTRY;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorMessageConstruction.constructListenerErrorMessage;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorType.INVALID_HOLDER;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorType.MISSING_HOLDER;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerUtil.getValidNonNegativeIntegerValue;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerValidation.checkStackIsNotEmpty;

/**
 * Represents listener based call back function corresponding to the "position"
 * rule defined in ANTLR grammar file for corresponding ABNF rule in RFC 6020.
 */
public final class PositionListener {

    /**
     * Creates a new position listener.
     */
    private PositionListener() {
    }

    /**
     * It is called when parser receives an input matching the grammar rule
     * (position), perform validations and update the data model tree.
     *
     * @param listener Listener's object
     * @param ctx context object of the grammar rule
     */
    public static void processPositionEntry(TreeWalkListener listener,
            GeneratedYangParser.PositionStatementContext ctx) {

        // Check for stack to be non empty.
        checkStackIsNotEmpty(listener, MISSING_HOLDER, POSITION_DATA, ctx.position().getText(), ENTRY);

        // Obtain the node of the stack.
        Parsable tmpNode = listener.getParsedDataStack().peek();
        switch (tmpNode.getYangConstructType()) {
            case BIT_DATA: {
                YangBit bitNode = (YangBit) tmpNode;
                int positionValue = getValidBitPosition(listener, ctx);
                bitNode.setPosition(positionValue);
                break;
            }
            default:
                throw new ParserException(
                        constructListenerErrorMessage(INVALID_HOLDER, POSITION_DATA, ctx.position().getText(), ENTRY));
        }
    }

    /**
     * Validates BITS position value correctness and uniqueness.
     *
     * @param listener Listener's object
     * @param ctx context object of the grammar rule
     * @return position value
     */
    private static int getValidBitPosition(TreeWalkListener listener,
            GeneratedYangParser.PositionStatementContext ctx) {
        Parsable bitNode = listener.getParsedDataStack().pop();

        // Check for stack to be non empty.
        checkStackIsNotEmpty(listener, MISSING_HOLDER, POSITION_DATA, ctx.position().getText(), ENTRY);

        int positionValue = getValidNonNegativeIntegerValue(ctx.position().getText(), POSITION_DATA, ctx);

        Parsable tmpNode = listener.getParsedDataStack().peek();
        switch (tmpNode.getYangConstructType()) {
            case BITS_DATA: {
                YangBits yangBits = (YangBits) tmpNode;
                for (YangBit curBit : yangBits.getBitSet()) {
                    if (positionValue == curBit.getPosition()) {
                        listener.getParsedDataStack().push(bitNode);
                        ParserException parserException = new ParserException("YANG file error: Duplicate value of " +
                                "position is invalid.");
                        parserException.setLine(ctx.getStart().getLine());
                        parserException.setCharPosition(ctx.getStart().getCharPositionInLine());
                        throw parserException;
                    }
                }
                listener.getParsedDataStack().push(bitNode);
                return positionValue;
            }
            default:
                listener.getParsedDataStack().push(bitNode);
                throw new ParserException(
                        constructListenerErrorMessage(INVALID_HOLDER, POSITION_DATA, ctx.position().getText(), ENTRY));
        }
    }
}