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

/**
 * Error information while doing a listener's based walk is maintained in it.
 */
public class ListenerError {

    // Maintains the state of exception.
    private boolean errorFlag = false;

    // Maintains the reason of exception.
    private String errorMsg;

    // Maintains the line number of exception.
    private int lineNumber;

    // Maintains the character position in lin of exception.
    private int charPositionInLine;

    /**
     * Returns error flag.
     *
     * @return error flag.
     */
    public boolean isErrorFlag() {
        return errorFlag;
    }

    /**
     * Returns reason for error.
     *
     * @return error message
     */
    public String getErrorMsg() {
        return errorMsg;
    }

    /**
     * Returns error line number.
     *
     * @return error line number.
     */
    public int getLineNumber() {
        return lineNumber;
    }

    /**
     * Returns error position in line.
     *
     * @return error character position in line.
     */
    public int getCharPositionInLine() {
        return charPositionInLine;
    }

    /**
     * Set error flag.
     *
     * @param errorFlag error existence flag.
     */
    public void setErrorFlag(boolean errorFlag) {
        this.errorFlag = errorFlag;
    }

    /**
     * Set error message.
     *
      * @param errorMsg reason for error.
     */
    public void setErrorMsg(String errorMsg) {
        this.errorMsg = errorMsg;
    }

    /**
     * Set error line number.
     *
     * @param lineNumber line number of error.
     */
    public void setLineNumber(int lineNumber) {
        this.lineNumber = lineNumber;
    }

    /**
     * Set error character position in line.
     *
     * @param charPositionInLine error character position in line.
     */
    public void setCharPositionInLine(int charPositionInLine) {
        this.charPositionInLine = charPositionInLine;
    }
}