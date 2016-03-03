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

import java.util.List;

import org.onosproject.yangutils.datamodel.YangContainer;
import org.onosproject.yangutils.datamodel.YangList;
import org.onosproject.yangutils.datamodel.YangNode;
import org.onosproject.yangutils.parser.Parsable;
import org.onosproject.yangutils.parser.exceptions.ParserException;
import org.onosproject.yangutils.parser.impl.TreeWalkListener;
import org.onosproject.yangutils.utils.YangConstructType;

import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorMessageConstruction.constructListenerErrorMessage;
import static org.onosproject.yangutils.utils.YangConstructType.getYangConstructType;

/**
 * It's a utility to carry out listener validation.
 */
public final class ListenerValidation {

    /**
     * Creates a new listener validation.
     */
    private ListenerValidation() {
    }

    /**
     * Checks parsed data stack is not empty.
     *
     * @param listener Listener's object
     * @param errorType error type needs to be set in error message
     * @param yangConstructType type of parsable data in which error occurred
     * @param parsableDataTypeName name of parsable data type in which error
     *            occurred
     * @param errorLocation location where error occurred
     */
    public static void checkStackIsNotEmpty(TreeWalkListener listener, ListenerErrorType errorType,
            YangConstructType yangConstructType, String parsableDataTypeName,
            ListenerErrorLocation errorLocation) {
        if (listener.getParsedDataStack().empty()) {
            /*
             * If stack is empty it indicates error condition, value of
             * parsableDataTypeName will be null in case there is no name
             * attached to parsable data type.
             */
            String message = constructListenerErrorMessage(errorType, yangConstructType, parsableDataTypeName,
                    errorLocation);
            throw new ParserException(message);
        }
    }

    /**
     * Checks parsed data stack is empty.
     *
     * @param listener Listener's object
     * @param errorType error type needs to be set in error message
     * @param yangConstructType type of parsable data in which error occurred
     * @param parsableDataTypeName name of parsable data type in which error
     *            occurred
     * @param errorLocation location where error occurred
     */
    public static void checkStackIsEmpty(TreeWalkListener listener, ListenerErrorType errorType,
            YangConstructType yangConstructType, String parsableDataTypeName,
            ListenerErrorLocation errorLocation) {

        if (!listener.getParsedDataStack().empty()) {
            /*
             * If stack is empty it indicates error condition, value of
             * parsableDataTypeName will be null in case there is no name
             * attached to parsable data type.
             */
            String message = constructListenerErrorMessage(errorType, yangConstructType, parsableDataTypeName,
                    errorLocation);
            throw new ParserException(message);
        }
    }

    /**
     * Returns parent node config value, if top node does not specify a config
     * statement then default value true is returned.
     *
     * @param listener listener's object
     * @return true/false parent's config value
     */
    public static boolean getParentNodeConfig(TreeWalkListener listener) {
        YangNode parentNode;
        Parsable curData = listener.getParsedDataStack().peek();
        if (curData instanceof YangNode) {
            parentNode = ((YangNode) curData).getParent();
            if (parentNode instanceof YangContainer) {
                return ((YangContainer) parentNode).isConfig();
            } else if (parentNode instanceof YangList) {
                return ((YangList) parentNode).isConfig();
            }
        }
        return true;
    }

    /**
     * Checks if a rule occurrences is as per the expected YANG grammar's
     * cardinality.
     *
     * @param childContext child's context
     * @param yangChildConstruct child construct for whom cardinality is to be
     *            validated
     * @param yangParentConstruct parent construct
     * @param parentName parent name
     * @throws ParserException exception if cardinality check fails
     */
    public static void validateCardinality(List<?> childContext, YangConstructType yangChildConstruct,
            YangConstructType yangParentConstruct, String parentName)
                    throws ParserException {

        if (!childContext.isEmpty() && childContext.size() != 1) {
            ParserException parserException = new ParserException("YANG file error: Invalid cardinality of "
                    + getYangConstructType(yangChildConstruct) + " in " + getYangConstructType(yangParentConstruct)
                    + " \"" + parentName + "\".");
            throw parserException;
        }
    }

    /**
     * Checks if a rule occurrences is exactly 1.
     *
     * @param childContext child's context
     * @param yangChildConstruct child construct for whom cardinality is to be
     *            validated
     * @param yangParentConstruct parent construct
     * @param parentName parent name
     * @throws ParserException exception if cardinality check fails
     */
    public static void validateCardinalityEqualsOne(List<?> childContext, YangConstructType yangChildConstruct,
            YangConstructType yangParentConstruct, String parentName) throws ParserException {

        if (childContext.isEmpty() || childContext.size() != 1) {
            ParserException parserException = new ParserException("YANG file error: Invalid cardinality of "
                    + getYangConstructType(yangChildConstruct) + " in " + getYangConstructType(yangParentConstruct)
                    + " \"" + parentName + "\".");
            throw parserException;
        }
    }

    /**
     * Checks if a rule occurrences is minimum 1.
     *
     * @param childContext child's context
     * @param yangChildConstruct child construct for whom cardinality is to be
     *            validated
     * @param yangParentConstruct parent construct
     * @param parentName parent name
     * @throws ParserException exception if cardinality check fails
     */
    public static void validateCardinalityNonNull(List<?> childContext, YangConstructType yangChildConstruct,
            YangConstructType yangParentConstruct, String parentName) throws ParserException {

        if (childContext.isEmpty()) {
            ParserException parserException = new ParserException("YANG file error: Invalid cardinality of "
                    + getYangConstructType(yangChildConstruct) + " in " + getYangConstructType(yangParentConstruct)
                    + " \"" + parentName + "\".");
            throw parserException;
        }
    }
}
