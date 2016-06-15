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

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import org.onosproject.yangutils.datamodel.YangDataTypes;
import org.onosproject.yangutils.datamodel.YangDerivedInfo;
import org.onosproject.yangutils.datamodel.YangPatternRestriction;
import org.onosproject.yangutils.datamodel.YangStringRestriction;
import org.onosproject.yangutils.datamodel.YangType;
import org.onosproject.yangutils.datamodel.utils.Parsable;
import org.onosproject.yangutils.datamodel.utils.YangConstructType;
import org.onosproject.yangutils.parser.antlrgencode.GeneratedYangParser;
import org.onosproject.yangutils.parser.exceptions.ParserException;
import org.onosproject.yangutils.parser.impl.TreeWalkListener;

import static org.onosproject.yangutils.datamodel.YangDataTypes.DERIVED;
import static org.onosproject.yangutils.datamodel.utils.YangConstructType.PATTERN_DATA;
import static org.onosproject.yangutils.datamodel.utils.YangConstructType.TYPE_DATA;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorLocation.ENTRY;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorLocation.EXIT;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorMessageConstruction.constructListenerErrorMessage;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorType.INVALID_HOLDER;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorType.MISSING_CURRENT_HOLDER;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorType.MISSING_HOLDER;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerValidation.checkStackIsNotEmpty;

/*
 * Reference: RFC6020 and YANG ANTLR Grammar
 *
 * ABNF grammar as per RFC6020
 *  pattern-stmt        = pattern-keyword sep string optsep
 *                        (";" /
 *                         "{" stmtsep
 *                             ;; these stmts can appear in any order
 *                             [error-message-stmt stmtsep]
 *                             [error-app-tag-stmt stmtsep]
 *                             [description-stmt stmtsep]
 *                             [reference-stmt stmtsep]
 *                          "}")
 *
 * ANTLR grammar rule
 *  patternStatement : PATTERN_KEYWORD string (STMTEND | LEFT_CURLY_BRACE commonStatements RIGHT_CURLY_BRACE);
 */

/**
 * Represents listener based call back function corresponding to the "pattern"
 * rule defined in ANTLR grammar file for corresponding ABNF rule in RFC 6020.
 */
public final class PatternRestrictionListener {

    private static final String EMPTY_STRING = "";

    /**
     * Creates a new pattern restriction listener.
     */
    private PatternRestrictionListener() {
    }

    /**
     * It is called when parser receives an input matching the grammar
     * rule (pattern), performs validation and updates the data model
     * tree.
     *
     * @param listener listener's object
     * @param ctx      context object of the grammar rule
     */
    public static void processPatternRestrictionEntry(TreeWalkListener listener,
                                                      GeneratedYangParser.PatternStatementContext ctx) {

        // Check for stack to be non empty.
        checkStackIsNotEmpty(listener, MISSING_HOLDER, PATTERN_DATA, ctx.string().getText(), ENTRY);

        Parsable tmpData = listener.getParsedDataStack().peek();
        if (tmpData.getYangConstructType() == TYPE_DATA) {
            YangType type = (YangType) tmpData;
            setPatternRestriction(listener, type, ctx);
        } else {
            throw new ParserException(constructListenerErrorMessage(INVALID_HOLDER, PATTERN_DATA,
                    ctx.string().getText(), ENTRY));
        }
    }

    /**
     * Sets the pattern restriction to type.
     *
     * @param listener listener's object
     * @param type     Yang type for which pattern restriction to be set
     * @param ctx      context object of the grammar rule
     */
    private static void setPatternRestriction(TreeWalkListener listener, YangType type,
                                              GeneratedYangParser.PatternStatementContext ctx) {

        if (type.getDataType() != YangDataTypes.STRING && type.getDataType() != YangDataTypes.DERIVED) {

            ParserException parserException = new ParserException("YANG file error : " +
                    YangConstructType.getYangConstructType(PATTERN_DATA) + " name " + ctx.string().getText() +
                    " can be used to restrict the built-in type string or types derived from string.");
            parserException.setLine(ctx.getStart().getLine());
            parserException.setCharPosition(ctx.getStart().getCharPositionInLine());
            throw parserException;
        }

        // Validate and get valid pattern restriction string.
        String patternArgument = getValidPattern(ctx);

        if (type.getDataType() == YangDataTypes.STRING) {
            YangStringRestriction stringRestriction = (YangStringRestriction) type.getDataTypeExtendedInfo();
            if (stringRestriction == null) {
                stringRestriction = new YangStringRestriction();
                type.setDataTypeExtendedInfo(stringRestriction);
                stringRestriction.addPattern(patternArgument);
            } else {
                stringRestriction.addPattern(patternArgument);
            }
            listener.getParsedDataStack().push(stringRestriction);
        } else {
            YangPatternRestriction patternRestriction = (YangPatternRestriction) ((YangDerivedInfo<?>) type
                    .getDataTypeExtendedInfo()).getPatternRestriction();
            if (patternRestriction == null) {
                patternRestriction = new YangPatternRestriction();
                ((YangDerivedInfo<?>) type.getDataTypeExtendedInfo()).setPatternRestriction(patternRestriction);
                patternRestriction.addPattern(patternArgument);
            } else {
                ((YangDerivedInfo<?>) type.getDataTypeExtendedInfo()).setPatternRestriction(patternRestriction);
                patternRestriction.addPattern(patternArgument);
            }
        }
    }

    /**
     * Performs validation and updates the data model tree.
     * It is called when parser exits from grammar rule (pattern).
     *
     * @param listener listener's object
     * @param ctx      context object of the grammar rule
     */
    public static void processPatternRestrictionExit(TreeWalkListener listener,
                                                    GeneratedYangParser.PatternStatementContext ctx) {

        // Check for stack to be non empty.
        checkStackIsNotEmpty(listener, MISSING_HOLDER, PATTERN_DATA, ctx.string().getText(), EXIT);

        Parsable tmpData = listener.getParsedDataStack().peek();
        if (tmpData instanceof YangStringRestriction) {
            listener.getParsedDataStack().pop();
        } else if (tmpData instanceof YangType
                && ((YangType) tmpData).getDataType() == DERIVED) {
            // TODO : need to handle in linker
        } else {
            throw new ParserException(constructListenerErrorMessage(MISSING_CURRENT_HOLDER, PATTERN_DATA,
                    ctx.string().getText(), EXIT));
        }
    }

    /**
     * Validates and return the valid pattern.
     *
     * @param ctx context object of the grammar rule
     * @return validated string
     */
    private static String getValidPattern(GeneratedYangParser.PatternStatementContext ctx) {
        String userInputPattern = ctx.string().getText().replace("\"", EMPTY_STRING);
        try {
            Pattern.compile(userInputPattern);
        } catch (PatternSyntaxException exception) {
            ParserException parserException = new ParserException("YANG file error : " +
                    YangConstructType.getYangConstructType(PATTERN_DATA) + " name " + ctx.string().getText() +
                    " is not a valid regular expression");
            parserException.setLine(ctx.getStart().getLine());
            parserException.setCharPosition(ctx.getStart().getCharPositionInLine());
            throw parserException;
        }
        return userInputPattern;
    }
}
