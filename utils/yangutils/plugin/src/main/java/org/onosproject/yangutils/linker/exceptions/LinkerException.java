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

package org.onosproject.yangutils.linker.exceptions;

/**
 * Represents base class for exceptions in linker operations.
 */
public class LinkerException extends RuntimeException {

    private static final long serialVersionUID = 20160211L;
    private int lineNumber;
    private int charPositionInLine;
    private String fileName;

    /**
     * Creates a new linker exception.
     */
    public LinkerException() {
        super();
    }

    /**
     * Creates a new linker exception with given message.
     *
     * @param message the detail of exception in string
     */
    public LinkerException(String message) {
        super(message);
    }

    /**
     * Creates a new linker exception from given message and cause.
     *
     * @param message the detail of exception in string
     * @param cause   underlying cause of the error
     */
    public LinkerException(final String message, final Throwable cause) {
        super(message, cause);
    }

    /**
     * Creates a new linker exception from cause.
     *
     * @param cause underlying cause of the error
     */
    public LinkerException(final Throwable cause) {
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
     * Returns YANG file name of the exception.
     *
     * @return YANG file name of the exception
     */
    public String getFileName() {
        return this.fileName;
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

    /**
     * Sets file name in parser exception.
     *
     * @param fileName YANG file name
     */
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
}
