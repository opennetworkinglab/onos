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
package org.onosproject.yangutils.datamodel.exceptions;

/**
 * Represents base class for exceptions in data model operations.
 */
public class DataModelException extends Exception {

    private static final long serialVersionUID = 201601270658L;
    private int lineNumber;
    private int charPositionInLine;

    /**
     * Creates a data model exception with message.
     *
     * @param message the detail of exception in string
     */
    public DataModelException(String message) {
        super(message);
    }

    /**
     * Creates exception from message and cause.
     *
     * @param message the detail of exception in string
     * @param cause underlying cause of the error
     */
    public DataModelException(final String message, final Throwable cause) {
        super(message, cause);
    }

    /**
     * Creates exception from cause.
     *
     * @param cause underlying cause of the error
     */
    public DataModelException(final Throwable cause) {
        super(cause);
    }

    /**
     * Returns line number of the exception.
     *
     * @return line number of the exception
     */
    public int getLineNumber() {
        return this.lineNumber;
    }

    /**
     * Returns position of the exception.
     *
     * @return position of the exception
     */
    public int getCharPositionInLine() {
        return this.charPositionInLine;
    }

    /**
     * Sets line number of YANG file.
     *
     * @param line line number of YANG file
     */
    public void setLine(int line) {
        this.lineNumber = line;
    }

    /**
     * Sets position of exception.
     *
     * @param charPosition position of exception
     */
    public void setCharPosition(int charPosition) {
        this.charPositionInLine = charPosition;
    }
}
