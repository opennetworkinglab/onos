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

package org.onosproject.yangutils.datamodel;

/**
 * Abstraction of location information, this is used during resolution is
 * carried out and line/character position in line is required to point
 * out the error location in YANG file.
 */
public interface LocationInfo {

    /**
     * Returns the line number YANG construct in file.
     *
     * @return the line number YANG construct in file
     */
    int getLineNumber();

    /**
     * Returns the character position in line.
     *
     * @return the character position in line
     */
    int getCharPosition();

    /**
     * Sets line number of YANG construct.
     *
     * @param lineNumber the line number of YANG construct in file
     */
    void setLineNumber(int lineNumber);

    /**
     * Sets character position of YANG construct.
     *
     * @param charPositionInLine character position of YANG construct in file
     */
    void setCharPosition(int charPositionInLine);
}
