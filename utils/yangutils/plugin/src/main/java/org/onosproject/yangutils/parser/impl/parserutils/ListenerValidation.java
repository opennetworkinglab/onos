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

package org.onosproject.yangutils.parser.impl.parserutils;

import java.util.Iterator;
import java.util.List;

import org.antlr.v4.runtime.ParserRuleContext;
import org.onosproject.yangutils.datamodel.YangContainer;
import org.onosproject.yangutils.datamodel.YangList;
import org.onosproject.yangutils.datamodel.YangNode;
import org.onosproject.yangutils.datamodel.utils.Parsable;
import org.onosproject.yangutils.datamodel.utils.YangConstructType;
import org.onosproject.yangutils.parser.exceptions.ParserException;
import org.onosproject.yangutils.parser.impl.TreeWalkListener;

import static org.onosproject.yangutils.datamodel.utils.YangConstructType.getYangConstructType;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorMessageConstruction.constructListenerErrorMessage;

/**
 * Represents a utility to carry out listener validation.
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
    public static void validateCardinalityMaxOne(List<?> childContext, YangConstructType yangChildConstruct,
            YangConstructType yangParentConstruct, String parentName)
            throws ParserException {

        if (!childContext.isEmpty() && childContext.size() != 1) {
            ParserException parserException = new ParserException("YANG file error: \""
                    + getYangConstructType(yangChildConstruct) + "\" is defined more than once in \""
                    + getYangConstructType(yangParentConstruct) + " " + parentName + "\".");

            Iterator<?> context = childContext.iterator();
            parserException.setLine(((ParserRuleContext) context.next()).getStart().getLine());
            parserException.setCharPosition(((ParserRuleContext) context.next()).getStart().getCharPositionInLine());
            throw parserException;
        }
    }

    /**
     * Checks if a rule occurrences is exactly 1.
     *
     * @param childContext child's context
     * @param yangChildConstruct child construct for whom cardinality is to be
     *                           validated
     * @param yangParentConstruct parent construct
     * @param parentName parent name
     * @param parentContext parents's context
     * @throws ParserException exception if cardinality check fails
     */
    public static void validateCardinalityEqualsOne(List<?> childContext, YangConstructType yangChildConstruct,
            YangConstructType yangParentConstruct, String parentName,
            ParserRuleContext parentContext)
            throws ParserException {

        if (childContext.isEmpty()) {
            ParserException parserException = new ParserException("YANG file error: Missing \""
                    + getYangConstructType(yangChildConstruct) + "\" in \"" + getYangConstructType(yangParentConstruct)
                    + " " + parentName + "\".");
            parserException.setLine(parentContext.getStart().getLine());
            parserException.setCharPosition(parentContext.getStart().getCharPositionInLine());
            throw parserException;
        } else if (!childContext.isEmpty() && childContext.size() != 1) {
            Iterator<?> childcontext = childContext.iterator();
            ParserException parserException = new ParserException("YANG file error: \""
                    + getYangConstructType(yangChildConstruct) + "\" is present more than once in \""
                    + getYangConstructType(yangParentConstruct) + " " + parentName + "\".");
            parserException.setLine(((ParserRuleContext) childcontext.next()).getStart().getLine());
            parserException.setCharPosition(((ParserRuleContext) childcontext.next()).getStart()
                    .getCharPositionInLine());
            throw parserException;
        }
    }

    /**
     * Checks if a rule occurrences is minimum 1.
     *
     * @param childContext child's context
     * @param yangChildConstruct child construct for whom cardinality is to be
     *                           validated
     * @param yangParentConstruct parent construct
     * @param parentName parent name
     * @param parentContext parents's context
     * @throws ParserException exception if cardinality check fails
     */
    public static void validateCardinalityNonZero(List<?> childContext, YangConstructType yangChildConstruct,
            YangConstructType yangParentConstruct, String parentName,
            ParserRuleContext parentContext)
            throws ParserException {

        if (childContext.isEmpty()) {
            ParserException parserException = new ParserException("YANG file error: Missing \""
                    + getYangConstructType(yangChildConstruct) + "\" in \"" + getYangConstructType(yangParentConstruct)
                    + " " + parentName + "\".");

            parserException.setLine(parentContext.getStart().getLine());
            parserException.setCharPosition(parentContext.getStart().getCharPositionInLine());
            throw parserException;
        }
    }

    /**
     * Checks if a either of one construct occurrence.
     *
     * @param child1Context first optional child's context
     * @param yangChild1Construct first child construct for whom cardinality is
     *                            to be validated
     * @param child2Context second optional child's context
     * @param yangChild2Construct second child construct for whom cardinality is
     *                            to be validated
     * @param yangParentConstruct parent construct
     * @param parentName parent name
     * @throws ParserException exception if cardinality check fails
     */
    public static void validateMutuallyExclusiveChilds(List<?> child1Context, YangConstructType yangChild1Construct,
            List<?> child2Context, YangConstructType yangChild2Construct,
            YangConstructType yangParentConstruct, String parentName)
            throws ParserException {

        if (!child1Context.isEmpty() && !child2Context.isEmpty()) {
            ParserException parserException = new ParserException("YANG file error: \""
                    + getYangConstructType(yangChild1Construct) + "\" & \"" + getYangConstructType(yangChild2Construct)
                    + "\" should be mutually exclusive in \"" + getYangConstructType(yangParentConstruct) + " "
                    + parentName + "\".");

            parserException.setLine(((ParserRuleContext) child2Context).getStart().getLine());
            parserException.setCharPosition(((ParserRuleContext) child2Context).getStart().getCharPositionInLine());
            throw parserException;
        }
    }
}
