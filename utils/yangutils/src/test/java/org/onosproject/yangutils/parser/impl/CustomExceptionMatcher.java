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

package org.onosproject.yangutils.parser.impl;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.onosproject.yangutils.parser.exceptions.ParserException;

/**
 * ExpectedException framework can use the Hamcrest matcher's to test
 * custom/extended exceptions. This class extends the type safe matcher to
 * define the custom exception matcher.
 */
public final class CustomExceptionMatcher extends TypeSafeMatcher<ParserException> {

    private int actualLine;
    private final int expectedLine;
    private int actualCharPosition;
    private final int expectedCharPosition;

    /**
     * Customized exception matcher to match error location.
     *
     * @param line error line
     * @param charPosition error character position
     * @return customized exception matcher to match error location
     */
    public static CustomExceptionMatcher errorLocation(int line, int charPosition) {
        return new CustomExceptionMatcher(line, charPosition);
    }

    private CustomExceptionMatcher(int expectedLine, int expectedCharPosition) {
        this.expectedLine = expectedLine;
        this.expectedCharPosition = expectedCharPosition;
    }

    @Override
    protected boolean matchesSafely(final ParserException exception) {
        actualLine = exception.getLineNumber();
        actualCharPosition = exception.getCharPositionInLine();
        return ((actualLine == expectedLine) && (actualCharPosition == expectedCharPosition));
    }

    @Override
    public void describeTo(Description description) {
        description.appendText(" Error reported location ")
                .appendText("Line " + actualLine + ", " + "CharPosition " + actualCharPosition)
                .appendText(" instead of expected ")
                .appendText("Line " + expectedLine + ", " + "CharPosition " + expectedCharPosition);
    }
}