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

package org.onosproject.yangutils.parser.impl.parserutils;

import org.antlr.v4.runtime.ParserRuleContext;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.regex.Pattern;
import org.onosproject.yangutils.utils.YangConstructType;
import org.onosproject.yangutils.parser.exceptions.ParserException;

/**
 * It's a utility for listener.
 */
public final class ListenerUtil {
    private static final Pattern IDENTIFIER_PATTERN = Pattern.compile("[a-zA-Z_][a-zA-Z0-9_.-]*");
    private static final String PLUS = "+";
    private static final int IDENTIFIER_LENGTH = 64;

    /**
     * Creates a new listener util.
     */
    private ListenerUtil() {
    }

    /**
     * Removes doubles quotes and concatenates if string has plus symbol.
     *
     * @param yangStringData string from yang file
     * @return concatenated string after removing double quotes
     */
    public static String removeQuotesAndHandleConcat(String yangStringData) {

        yangStringData = yangStringData.replace("\"", "");
        String[] tmpData = yangStringData.split(Pattern.quote(PLUS));
        StringBuilder builder = new StringBuilder();
        for (String yangString : tmpData) {
            builder.append(yangString);
        }
        return builder.toString();
    }

    /**
     * Validates identifier and returns concatenated string if string contains plus symbol.
     *
     * @param identifier string from yang file
     * @param yangConstruct yang construct for creating error message
     * @param ctx yang construct's context to get the line number and character position
     * @return concatenated string after removing double quotes
     */
    public static String getValidIdentifier(String identifier, YangConstructType yangConstruct, ParserRuleContext ctx) {

        String identifierString = removeQuotesAndHandleConcat(identifier);
        ParserException parserException;

        if (identifierString.length() > IDENTIFIER_LENGTH) {
            parserException = new ParserException("YANG file error : " +
                    YangConstructType.getYangConstructType(yangConstruct) + " name " + identifierString + " is " +
                    "greater than 64 characters.");
        } else if (!IDENTIFIER_PATTERN.matcher(identifierString).matches()) {
            parserException = new ParserException("YANG file error : " +
                    YangConstructType.getYangConstructType(yangConstruct) + " name " + identifierString + " is not " +
                    "valid.");
        } else {
            return identifierString;
        }

        parserException.setLine(ctx.getStart().getLine());
        parserException.setCharPosition(ctx.getStart().getCharPositionInLine());
        throw parserException;
    }

    /**
     * Validates the revision date.
     *
     * @param dateToValidate input revision date
     * @return validation result, true for success, false for failure
     */
    public static boolean isDateValid(String dateToValidate) {

        if (dateToValidate == null || !dateToValidate.matches("[0-9]{4}-[0-9]{2}-[0-9]{2}")) {
            return false;
        }

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        sdf.setLenient(false);

        try {
            //if not valid, it will throw ParseException
            sdf.parse(dateToValidate);
        } catch (ParseException e) {
            return false;
        }

        return true;
    }
}