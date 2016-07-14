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

import org.antlr.v4.runtime.ParserRuleContext;
import org.onosproject.yangutils.datamodel.YangAtomicPath;
import org.onosproject.yangutils.datamodel.YangImport;
import org.onosproject.yangutils.datamodel.YangLeaf;
import org.onosproject.yangutils.datamodel.YangLeafRef;
import org.onosproject.yangutils.datamodel.YangLeavesHolder;
import org.onosproject.yangutils.datamodel.YangList;
import org.onosproject.yangutils.datamodel.YangModule;
import org.onosproject.yangutils.datamodel.YangNode;
import org.onosproject.yangutils.datamodel.YangNodeIdentifier;
import org.onosproject.yangutils.datamodel.YangPathPredicate;
import org.onosproject.yangutils.datamodel.YangReferenceResolver;
import org.onosproject.yangutils.datamodel.YangRelativePath;
import org.onosproject.yangutils.datamodel.YangSubModule;
import org.onosproject.yangutils.datamodel.utils.YangConstructType;
import org.onosproject.yangutils.parser.antlrgencode.GeneratedYangParser;
import org.onosproject.yangutils.parser.exceptions.ParserException;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

import static org.onosproject.yangutils.datamodel.YangPathArgType.ABSOLUTE_PATH;
import static org.onosproject.yangutils.datamodel.YangPathArgType.RELATIVE_PATH;
import static org.onosproject.yangutils.datamodel.YangPathOperator.EQUALTO;
import static org.onosproject.yangutils.utils.UtilConstants.ADD;
import static org.onosproject.yangutils.utils.UtilConstants.ANCESTOR_ACCESSOR;
import static org.onosproject.yangutils.utils.UtilConstants.ANCESTOR_ACCESSOR_IN_PATH;
import static org.onosproject.yangutils.utils.UtilConstants.CARET;
import static org.onosproject.yangutils.utils.UtilConstants.CHAR_OF_CLOSE_SQUARE_BRACKET;
import static org.onosproject.yangutils.utils.UtilConstants.CHAR_OF_OPEN_SQUARE_BRACKET;
import static org.onosproject.yangutils.utils.UtilConstants.CHAR_OF_SLASH;
import static org.onosproject.yangutils.utils.UtilConstants.CLOSE_PARENTHESIS;
import static org.onosproject.yangutils.utils.UtilConstants.COLON;
import static org.onosproject.yangutils.utils.UtilConstants.CURRENT;
import static org.onosproject.yangutils.utils.UtilConstants.EMPTY_STRING;
import static org.onosproject.yangutils.utils.UtilConstants.FALSE;
import static org.onosproject.yangutils.utils.UtilConstants.OPEN_SQUARE_BRACKET;
import static org.onosproject.yangutils.utils.UtilConstants.QUOTES;
import static org.onosproject.yangutils.utils.UtilConstants.SLASH;
import static org.onosproject.yangutils.utils.UtilConstants.SLASH_FOR_STRING;
import static org.onosproject.yangutils.utils.UtilConstants.TRUE;
import static org.onosproject.yangutils.utils.UtilConstants.YANG_FILE_ERROR;

/**
 * Represents an utility for listener.
 */
public final class ListenerUtil {

    private static final Pattern IDENTIFIER_PATTERN = Pattern.compile("[a-zA-Z_][a-zA-Z0-9_.-]*");
    private static final String DATE_PATTERN = "[0-9]{4}-([0-9]{2}|[0-9])-([0-9]{2}|[0-9])";
    private static final String NON_NEGATIVE_INTEGER_PATTERN = "[0-9]+";
    private static final Pattern INTEGER_PATTERN = Pattern.compile("[-][0-9]+|[0-9]+");
    private static final Pattern PATH_PREDICATE_PATTERN = Pattern.compile("\\[(.*?)\\]");
    private static final String XML = "xml";
    private static final String ONE = "1";
    private static final int IDENTIFIER_LENGTH = 64;
    private static final String DATE_FORMAT = "yyyy-MM-dd";

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

        yangStringData = yangStringData.replace("\"", EMPTY_STRING);
        String[] tmpData = yangStringData.split(Pattern.quote(ADD));
        StringBuilder builder = new StringBuilder();
        for (String yangString : tmpData) {
            builder.append(yangString);
        }
        return builder.toString();
    }

    /**
     * Validates identifier and returns concatenated string if string contains plus symbol.
     *
     * @param identifier    string from yang file
     * @param yangConstruct yang construct for creating error message
     * @param ctx           yang construct's context to get the line number and character position
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
        } else if (identifierString.toLowerCase().startsWith(XML)) {
            parserException = new ParserException("YANG file error : " +
                    YangConstructType.getYangConstructType(yangConstruct) + " identifier " + identifierString +
                    " must not start with (('X'|'x') ('M'|'m') ('L'|'l')).");
        } else {
            return identifierString;
        }

        parserException.setLine(ctx.getStart().getLine());
        parserException.setCharPosition(ctx.getStart().getCharPositionInLine());
        throw parserException;
    }

    /**
     * Validates identifier and returns concatenated string if string contains plus symbol.
     *
     * @param identifier    string from yang file
     * @param yangConstruct yang construct for creating error message
     * @param ctx           yang construct's context to get the line number and character position
     * @param yangLeafRef   instance of leafref where the path argument has to be set
     * @return concatenated string after removing double quotes
     */
    public static String getValidIdentifierForLeafref(String identifier, YangConstructType yangConstruct,
                                                      ParserRuleContext ctx, YangLeafRef yangLeafRef) {

        String identifierString = removeQuotesAndHandleConcat(identifier);
        ParserException parserException;

        if (identifierString.length() > IDENTIFIER_LENGTH) {
            parserException = new ParserException("YANG file error : " + " identifier " + identifierString + " in " +
                    YangConstructType.getYangConstructType(yangConstruct) + " " + yangLeafRef.getPath() + " is " +
                    "greater than 64 characters.");
        } else if (!IDENTIFIER_PATTERN.matcher(identifierString).matches()) {
            parserException = new ParserException("YANG file error : " + " identifier " + identifierString + " in " +
                    YangConstructType.getYangConstructType(yangConstruct) + " " + yangLeafRef.getPath() + " is not " +
                    "valid.");
        } else if (identifierString.toLowerCase().startsWith(XML)) {
            parserException = new ParserException("YANG file error : " + " identifier " + identifierString + " in " +
                    YangConstructType.getYangConstructType(yangConstruct) + " " + yangLeafRef.getPath() +
                    " must not start with (('X'|'x') ('M'|'m') ('L'|'l')).");
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
        if (dateToValidate == null || !dateToValidate.matches(DATE_PATTERN)) {
            return false;
        }

        SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);
        sdf.setLenient(false);

        try {
            //if not valid, it will throw ParseException
            sdf.parse(dateToValidate);
        } catch (ParseException e) {
            return false;
        }

        return true;
    }

    /**
     * Validates YANG version.
     *
     * @param ctx version context object of the grammar rule
     * @return valid version
     */
    public static byte getValidVersion(GeneratedYangParser.YangVersionStatementContext ctx) {

        String value = removeQuotesAndHandleConcat(ctx.version().getText());
        if (!value.equals(ONE)) {
            ParserException parserException = new ParserException("YANG file error: Input version not supported");
            parserException.setLine(ctx.getStart().getLine());
            parserException.setCharPosition(ctx.getStart().getCharPositionInLine());
            throw parserException;
        }

        return Byte.valueOf(value);
    }

    /**
     * Validates non negative integer value.
     *
     * @param integerValue  integer to be validated
     * @param yangConstruct yang construct for creating error message
     * @param ctx           context object of the grammar rule
     * @return valid non negative integer value
     */
    public static int getValidNonNegativeIntegerValue(String integerValue, YangConstructType yangConstruct,
                                                      ParserRuleContext ctx) {

        String value = removeQuotesAndHandleConcat(integerValue);
        if (!value.matches(NON_NEGATIVE_INTEGER_PATTERN)) {
            ParserException parserException = new ParserException("YANG file error : " +
                    YangConstructType.getYangConstructType(yangConstruct) + " value " + value + " is not " +
                    "valid.");
            parserException.setLine(ctx.getStart().getLine());
            parserException.setCharPosition(ctx.getStart().getCharPositionInLine());
            throw parserException;
        }

        int valueInInteger;
        try {
            valueInInteger = Integer.parseInt(value);
        } catch (NumberFormatException e) {
            ParserException parserException = new ParserException("YANG file error : " +
                    YangConstructType.getYangConstructType(yangConstruct) + " value " + value + " is not " +
                    "valid.");
            parserException.setLine(ctx.getStart().getLine());
            parserException.setCharPosition(ctx.getStart().getCharPositionInLine());
            throw parserException;
        }
        return valueInInteger;
    }

    /**
     * Validates integer value.
     *
     * @param integerValue  integer to be validated
     * @param yangConstruct yang construct for creating error message
     * @param ctx           context object of the grammar rule
     * @return valid integer value
     */
    public static int getValidIntegerValue(String integerValue, YangConstructType yangConstruct,
                                           ParserRuleContext ctx) {

        String value = removeQuotesAndHandleConcat(integerValue);
        if (!INTEGER_PATTERN.matcher(value).matches()) {
            ParserException parserException = new ParserException("YANG file error : " +
                    YangConstructType.getYangConstructType(yangConstruct) + " value " + value + " is not " +
                    "valid.");
            parserException.setLine(ctx.getStart().getLine());
            parserException.setCharPosition(ctx.getStart().getCharPositionInLine());
            throw parserException;
        }

        int valueInInteger;
        try {
            valueInInteger = Integer.parseInt(value);
        } catch (NumberFormatException e) {
            ParserException parserException = new ParserException("YANG file error : " +
                    YangConstructType.getYangConstructType(yangConstruct) + " value " + value + " is not " +
                    "valid.");
            parserException.setLine(ctx.getStart().getLine());
            parserException.setCharPosition(ctx.getStart().getCharPositionInLine());
            throw parserException;
        }
        return valueInInteger;
    }

    /**
     * Validates boolean value.
     *
     * @param booleanValue  value to be validated
     * @param yangConstruct yang construct for creating error message
     * @param ctx           context object of the grammar rule
     * @return boolean value either true or false
     */
    public static boolean getValidBooleanValue(String booleanValue, YangConstructType yangConstruct,
                                               ParserRuleContext ctx) {

        String value = removeQuotesAndHandleConcat(booleanValue);
        if (value.equals(TRUE)) {
            return true;
        } else if (value.equals(FALSE)) {
            return false;
        } else {
            ParserException parserException = new ParserException("YANG file error : " +
                    YangConstructType.getYangConstructType(yangConstruct) + " value " + value + " is not " +
                    "valid.");
            parserException.setLine(ctx.getStart().getLine());
            parserException.setCharPosition(ctx.getStart().getCharPositionInLine());
            throw parserException;
        }
    }

    /**
     * Returns current date and makes it in usable format for revision.
     *
     * @return usable current date format for revision
     */
    public static Date getCurrentDateForRevision() {

        SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);

        Date date = new Date();
        String dateInString = dateFormat.format(date);
        try {
            //if not valid, it will throw ParseException
            Date now = dateFormat.parse(dateInString);
            return date;
        } catch (ParseException e) {
            ParserException parserException = new ParserException("YANG file error: Input date is not correct");
            throw parserException;
        }
    }

    /**
     * Checks and return valid node identifier.
     *
     * @param nodeIdentifierString string from yang file
     * @param yangConstruct        yang construct for creating error message
     * @param ctx                  yang construct's context to get the line number and character position
     * @return valid node identifier
     */
    public static YangNodeIdentifier getValidNodeIdentifier(String nodeIdentifierString,
                                                            YangConstructType yangConstruct, ParserRuleContext ctx) {
        String tmpIdentifierString = removeQuotesAndHandleConcat(nodeIdentifierString);
        String[] tmpData = tmpIdentifierString.split(Pattern.quote(COLON));
        if (tmpData.length == 1) {
            YangNodeIdentifier nodeIdentifier = new YangNodeIdentifier();
            nodeIdentifier.setName(getValidIdentifier(tmpData[0], yangConstruct, ctx));
            return nodeIdentifier;
        } else if (tmpData.length == 2) {
            YangNodeIdentifier nodeIdentifier = new YangNodeIdentifier();
            nodeIdentifier.setPrefix(getValidIdentifier(tmpData[0], yangConstruct, ctx));
            nodeIdentifier.setName(getValidIdentifier(tmpData[1], yangConstruct, ctx));
            return nodeIdentifier;
        } else {
            ParserException parserException = new ParserException("YANG file error : " +
                    YangConstructType.getYangConstructType(yangConstruct) + " name " + nodeIdentifierString +
                    " is not valid.");
            parserException.setLine(ctx.getStart().getLine());
            parserException.setCharPosition(ctx.getStart().getCharPositionInLine());
            throw parserException;
        }
    }

    /**
     * Checks and return valid node identifier specific to nodes in leafref path.
     *
     * @param nodeIdentifierString string from yang file
     * @param yangConstruct        yang construct for creating error message
     * @param ctx                  yang construct's context to get the line number and character position
     * @param yangLeafRef          instance of leafref where the path argument has to be set
     * @return valid node identifier
     */
    public static YangNodeIdentifier getValidNodeIdentifierForLeafref(String nodeIdentifierString,
                                                                      YangConstructType yangConstruct,
                                                                      ParserRuleContext ctx, YangLeafRef yangLeafRef) {

        String tmpIdentifierString = removeQuotesAndHandleConcat(nodeIdentifierString);
        String[] tmpData = tmpIdentifierString.split(Pattern.quote(COLON));
        if (tmpData.length == 1) {
            YangNodeIdentifier nodeIdentifier = new YangNodeIdentifier();
            nodeIdentifier.setName(getValidIdentifierForLeafref(tmpData[0], yangConstruct, ctx, yangLeafRef));
            return nodeIdentifier;
        } else if (tmpData.length == 2) {
            YangNodeIdentifier nodeIdentifier = new YangNodeIdentifier();
            nodeIdentifier.setPrefix(getValidIdentifierForLeafref(tmpData[0], yangConstruct, ctx, yangLeafRef));
            nodeIdentifier.setName(getValidIdentifierForLeafref(tmpData[1], yangConstruct, ctx, yangLeafRef));
            return nodeIdentifier;
        } else {
            ParserException parserException = new ParserException("YANG file error : " +
                    YangConstructType.getYangConstructType(yangConstruct) + yangLeafRef.getPath() +
                    " is not valid.");
            parserException.setLine(ctx.getStart().getLine());
            parserException.setCharPosition(ctx.getStart().getCharPositionInLine());
            throw parserException;
        }
    }

    /**
     * Validates the path argument. It can be either absolute or relative path.
     *
     * @param pathString    the path string from the path type
     * @param yangConstruct yang construct for creating error message
     * @param ctx           yang construct's context to get the line number and character position
     * @param yangLeafRef   instance of leafref where the path argument has to be set
     */
    public static void validatePathArgument(String pathString, YangConstructType yangConstruct,
                                            ParserRuleContext ctx, YangLeafRef yangLeafRef) {

        String completePathString = removeQuotesAndHandleConcat(pathString);
        yangLeafRef.setPath(completePathString);
        if (completePathString.startsWith(SLASH)) {
            yangLeafRef.setPathType(ABSOLUTE_PATH);
            List<YangAtomicPath> yangAtomicPathListList = validateAbsolutePath(completePathString, yangConstruct, ctx,
                    yangLeafRef);
            validatePrefixAndYangNode(yangAtomicPathListList, yangLeafRef);
            yangLeafRef.setAtomicPath(yangAtomicPathListList);
        } else if (completePathString.startsWith(ANCESTOR_ACCESSOR)) {
            yangLeafRef.setPathType(RELATIVE_PATH);
            validateRelativePath(completePathString, yangConstruct, ctx, yangLeafRef);
        } else {
            ParserException parserException = new ParserException("YANG file error : " +
                    YangConstructType.getYangConstructType(yangConstruct) + yangLeafRef.getPath() +
                    " does not follow valid path syntax");
            parserException.setLine(ctx.getStart().getLine());
            parserException.setCharPosition(ctx.getStart().getCharPositionInLine());
            throw parserException;
        }
    }

    /**
     * Validates the prefixes in the leafref and assigns them to the respective imported name in map.
     *
     * @param yangAtomicPathList list of atomic poth
     * @param yangLeafRef      instance YANG leafref
     */
    private static void validatePrefixAndYangNode(List<YangAtomicPath> yangAtomicPathList, YangLeafRef yangLeafRef) {
        Iterator<YangAtomicPath> yangAtomicPathIterator = yangAtomicPathList.listIterator();
        while (yangAtomicPathIterator.hasNext()) {
            YangAtomicPath atomicPath = yangAtomicPathIterator.next();
            String prefix = atomicPath.getNodeIdentifier().getPrefix();
            YangNode parentNodeOfLeafref = yangLeafRef.getParentNodeOfLeafref();
            YangNode moduleOrSubModule = getModuleOrSubmoduleInFileOfTheCurrentNode(parentNodeOfLeafref);
            YangModule moduleNode = null;
            if (moduleOrSubModule instanceof YangModule) {
                moduleNode = (YangModule) moduleOrSubModule;
            }
            if (moduleNode != null) {
                updatePrefixWithTheImportedList(moduleNode, prefix, yangLeafRef);
            }
        }
    }

    /**
     * Updates the prefix with the imported list in the module.
     *
     * @param moduleNode   root node of the leafref
     * @param prefixInPath prefix in the path
     * @param yangLeafRef  instance YANG leafref
     */
    private static void updatePrefixWithTheImportedList(YangModule moduleNode, String prefixInPath, YangLeafRef
            yangLeafRef) {
        if (prefixInPath != null && prefixInPath != EMPTY_STRING && !prefixInPath.equals(moduleNode.getPrefix())) {
            List<YangImport> moduleImportList = moduleNode.getImportList();
            if (moduleImportList != null && !moduleImportList.isEmpty()) {
                Iterator<YangImport> yangImportIterator = moduleImportList.listIterator();
                while (yangImportIterator.hasNext()) {
                    YangImport yangImport = yangImportIterator.next();
                    if (yangImport.getPrefixId().equals(prefixInPath)) {
                        HashMap prefixMap = new HashMap();
                        prefixMap.put(prefixInPath, yangImport.getModuleName());
                        yangLeafRef.setPrefixAndItsImportedModule(prefixMap);
                    }
                }
            }
        } else {
            HashMap prefixMap = new HashMap();
            prefixMap.put(prefixInPath, moduleNode.getName());
            yangLeafRef.setPrefixAndItsImportedModule(prefixMap);
        }
    }

    /**
     * Returns module or submodule node from the current node.
     *
     * @param node current node
     * @return root node
     */
    private static YangNode getModuleOrSubmoduleInFileOfTheCurrentNode(YangNode node) {
        while (!(node instanceof YangModule) && !(node instanceof YangSubModule)) {
            if (node == null) {
                throw new ParserException("Internal datamodel error: Datamodel tree is not correct");
            }
            node = node.getParent();
        }
        return node;
    }

    /**
     * Validates the unique syntax from the reference path.
     *
     * @param uniquePath   path of unique
     * @param prefixOfFile current file's prefix
     * @param ctx          yang construct's context to get the line number and character position
     * @return list of absolute path
     */
    private static List<YangAtomicPath> validateUniqueValues(String uniquePath, String prefixOfFile,
                                                               ParserRuleContext ctx) {
        List<YangAtomicPath> atomicPath = new LinkedList<>();
        String[] pathInUnique = uniquePath.split(SLASH_FOR_STRING);
        for (String uniqueValue : pathInUnique) {
            YangAtomicPath yangAtomicPathPath = new YangAtomicPath();
            YangNodeIdentifier nodeIdentifier = getValidNodeIdentifier(uniqueValue, YangConstructType.UNIQUE_DATA, ctx);
            yangAtomicPathPath.setNodeIdentifier(nodeIdentifier);
            atomicPath.add(yangAtomicPathPath);
            if (nodeIdentifier.getPrefix() != null && nodeIdentifier.getPrefix() != prefixOfFile) {
                ParserException parserException = new ParserException("YANG file error : A leaf reference, in unique," +
                        " must refer to a leaf in the list");
                parserException.setLine(ctx.getStart().getLine());
                parserException.setCharPosition(ctx.getStart().getCharPositionInLine());
                throw parserException;
            }
        }
        return atomicPath;
    }

    /**
     * Validates unique field from the list.
     *
     * @param yangList instance of YANG list
     * @param ctx      yang construct's context to get the line number and character position
     */
    public static void validateUniqueInList(YangList yangList, ParserRuleContext ctx) {
        YangLeaf leaf;
        // Returns the prefix for the file where unique is present.
        String prefixOfTheFile = getPrefixInFileOfTheCurrentNode(yangList);
        List<String> uniques = yangList.getUniqueList();
        if (uniques != null && !uniques.isEmpty()) {
            Iterator<String> uniqueList = uniques.listIterator();
            while (uniqueList.hasNext()) {
                String pathInUnique = uniqueList.next();
                List<YangAtomicPath> atomicPathInUnique = validateUniqueValues(pathInUnique, prefixOfTheFile, ctx);
                YangAtomicPath leafInPath = atomicPathInUnique.get(atomicPathInUnique.size() - 1);
                if (atomicPathInUnique.size() == 1) {
                    leaf = getReferenceLeafFromUnique(yangList, leafInPath);
                } else {
                    atomicPathInUnique.remove(atomicPathInUnique.size() - 1);
                    YangNode holderOfLeaf = getNodeUnderListFromPath(atomicPathInUnique, yangList, ctx);
                    leaf = getReferenceLeafFromUnique(holderOfLeaf, leafInPath);
                }
                if (leaf == null) {
                    ParserException parserException = new ParserException("YANG file error : A leaf reference, in " +
                            "unique," +
                            " must refer to a leaf under the list");
                    parserException.setLine(ctx.getStart().getLine());
                    parserException.setCharPosition(ctx.getStart().getCharPositionInLine());
                    throw parserException;
                }
            }
        }
    }

    /**
     * Returns the last node under the unique path.
     *
     * @param uniquePath atomic path list
     * @param node       root node from where it starts searching
     * @param ctx        yang construct's context to get the line number and character position
     * @return last node in the list
     */
    private static YangNode getNodeUnderListFromPath(List<YangAtomicPath> uniquePath, YangNode node,
                                                     ParserRuleContext ctx) {
        Iterator<YangAtomicPath> nodesInReference = uniquePath.listIterator();
        YangNode potentialReferredNode = node.getChild();
        while (nodesInReference.hasNext()) {
            YangAtomicPath nodeInUnique = nodesInReference.next();
            YangNode referredNode = getReferredNodeFromTheUniqueNodes(nodeInUnique.getNodeIdentifier(),
                    potentialReferredNode);
            if (referredNode == null) {
                ParserException parserException = new ParserException("YANG file error : The target node in unique " +
                        "reference path is invalid");
                parserException.setLine(ctx.getStart().getLine());
                parserException.setCharPosition(ctx.getStart().getCharPositionInLine());
                throw parserException;
            } else {
                potentialReferredNode = referredNode.getChild();
            }
        }
        return potentialReferredNode;
    }

    /**
     * Returns the node that matches with the name of the node in path.
     *
     * @param nodeInUnique          node name in path
     * @param potentialReferredNode node under which it has to match
     * @return referred node
     */
    private static YangNode getReferredNodeFromTheUniqueNodes(YangNodeIdentifier nodeInUnique, YangNode
            potentialReferredNode) {
        while (potentialReferredNode != null) {
            // Check if the potential referred node is the actual referred node
            if (potentialReferredNode.getName().equals(nodeInUnique.getName())) {
                return potentialReferredNode;
            }
            potentialReferredNode = potentialReferredNode.getNextSibling();
        }
        return null;
    }

    /**
     * Returns the leaf which unique refers.
     *
     * @param nodeForLeaf  last node where leaf is referred
     * @param leafInUnique leaf in unique path
     * @return YANG leaf
     */
    private static YangLeaf getReferenceLeafFromUnique(YangNode nodeForLeaf, YangAtomicPath leafInUnique) {
        YangLeavesHolder leavesHolder = (YangLeavesHolder) nodeForLeaf;
        List<YangLeaf> leaves = leavesHolder.getListOfLeaf();
        if (leaves != null && !leaves.isEmpty()) {
            for (YangLeaf leaf : leaves) {
                if (leafInUnique.getNodeIdentifier().getName().equals(leaf.getName())) {
                    return leaf;
                }
            }
        }
        return null;
    }

    /**
     * Returns the prefix of the current file.
     *
     * @param node node where it needs to find the root node
     * @return prefix of root node
     */
    public static String getPrefixInFileOfTheCurrentNode(YangNode node) {
        String prefixInFile;
        while (!(node instanceof YangReferenceResolver)) {
            node = node.getParent();
            if (node == null) {
                throw new ParserException("Internal datamodel error: Datamodel tree is not correct");
            }
        }
        if (node instanceof YangModule) {
            YangModule yangModule = (YangModule) node;
            prefixInFile = yangModule.getPrefix();
        } else {
            YangSubModule yangSubModule = (YangSubModule) node;
            prefixInFile = yangSubModule.getPrefix();
        }
        return prefixInFile;
    }

    /**
     * Validates the relative path.
     *
     * @param completePathString the path string of relative path
     * @param yangConstruct      yang construct for creating error message
     * @param ctx                yang construct's context to get the line number and character position
     * @param yangLeafRef        instance of leafref where the path argument has to be set
     */
    private static void validateRelativePath(String completePathString, YangConstructType yangConstruct,
                                             ParserRuleContext ctx, YangLeafRef yangLeafRef) {

        YangRelativePath relativePath = new YangRelativePath();
        int numberOfAncestors = 0;
        while (completePathString.startsWith(ANCESTOR_ACCESSOR_IN_PATH)) {
            completePathString = completePathString.replaceFirst(ANCESTOR_ACCESSOR_IN_PATH, EMPTY_STRING);
            numberOfAncestors = numberOfAncestors + 1;
        }
        if (completePathString == null || completePathString.length() == 0) {
            ParserException parserException = new ParserException("YANG file error : "
                    + YangConstructType.getYangConstructType(yangConstruct) + yangLeafRef.getPath() +
                    " does not follow valid path syntax");
            parserException.setLine(ctx.getStart().getLine());
            parserException.setCharPosition(ctx.getStart().getCharPositionInLine());
            throw parserException;
        }
        relativePath.setAncestorNodeCount(numberOfAncestors);
        List<YangAtomicPath> atomicPath = validateAbsolutePath(SLASH_FOR_STRING + completePathString,
                yangConstruct,
                ctx, yangLeafRef);
        validatePrefixAndYangNode(atomicPath, yangLeafRef);
        relativePath.setAtomicPathList(atomicPath);
        yangLeafRef.setRelativePath(relativePath);
    }

    /**
     * Validates the absolute path.
     *
     * @param completePathString the path string of absolute path
     * @param yangConstruct      yang construct for creating error message
     * @param ctx                yang construct's context to get the line number and character position
     * @param yangLeafRef        instance of leafref where the path argument has to be set
     * @return list of object of node in absolute path
     */
    private static List<YangAtomicPath> validateAbsolutePath(String completePathString,
                                                               YangConstructType yangConstruct, ParserRuleContext
                                                                       ctx, YangLeafRef yangLeafRef) {

        List<YangAtomicPath> absolutePathList = new LinkedList<>();
        YangPathPredicate yangPathPredicate = new YangPathPredicate();
        YangNodeIdentifier yangNodeIdentifier;

        while (completePathString != null) {
            String path = completePathString.replaceFirst(SLASH_FOR_STRING, EMPTY_STRING);
            if (path == null || path.length() == 0) {
                ParserException parserException = new ParserException("YANG file error : "
                        + YangConstructType.getYangConstructType(yangConstruct) + " " + yangLeafRef.getPath() +
                        " does not follow valid path syntax");
                parserException.setLine(ctx.getStart().getLine());
                parserException.setCharPosition(ctx.getStart().getCharPositionInLine());
                throw parserException;
            }
            String matchedPathPredicate;
            String nodeIdentifier;
            String[] differentiate = new String[2];
            int forNodeIdentifier = path.indexOf(CHAR_OF_SLASH);
            int forPathPredicate = path.indexOf(CHAR_OF_OPEN_SQUARE_BRACKET);

            // Checks if path predicate is present for the node.
            if ((forPathPredicate < forNodeIdentifier) && (forPathPredicate != -1)) {
                List<String> pathPredicate = new ArrayList<>();
                matchedPathPredicate = matchForPathPredicate(path);

                if (matchedPathPredicate == null || matchedPathPredicate.length() == 0) {
                    ParserException parserException = new ParserException("YANG file error : "
                            + YangConstructType.getYangConstructType(yangConstruct) + " " + yangLeafRef.getPath() +
                            " does not follow valid path syntax");
                    parserException.setLine(ctx.getStart().getLine());
                    parserException.setCharPosition(ctx.getStart().getCharPositionInLine());
                    throw parserException;
                }
                int indexOfMatchedFirstOpenBrace = path.indexOf(CHAR_OF_OPEN_SQUARE_BRACKET);
                differentiate[0] = path.substring(0, indexOfMatchedFirstOpenBrace);
                differentiate[1] = path.substring(indexOfMatchedFirstOpenBrace);
                pathPredicate.add(matchedPathPredicate);
                nodeIdentifier = differentiate[0];
                // Starts adding all path predicates of a node into the list.
                if (!differentiate[1].isEmpty()) {
                    while (differentiate[1].startsWith(OPEN_SQUARE_BRACKET)) {
                        matchedPathPredicate = matchForPathPredicate(differentiate[1]);
                        if (matchedPathPredicate == null || matchedPathPredicate.length() == 0) {
                            ParserException parserException = new ParserException(
                                    "YANG file error : " + YangConstructType.getYangConstructType(yangConstruct) + " "
                                            + yangLeafRef.getPath() +
                                            " does not follow valid path syntax");
                            parserException.setLine(ctx.getStart().getLine());
                            parserException.setCharPosition(ctx.getStart().getCharPositionInLine());
                            throw parserException;
                        }
                        pathPredicate.add(matchedPathPredicate);
                        differentiate[1] = differentiate[1].substring(matchedPathPredicate.length());
                    }
                }

                List<YangPathPredicate> pathPredicateList = validatePathPredicate(pathPredicate, yangConstruct, ctx,
                        yangPathPredicate, yangLeafRef);
                YangAtomicPath atomicPath = new YangAtomicPath();
                yangNodeIdentifier = getValidNodeIdentifierForLeafref(nodeIdentifier, yangConstruct, ctx, yangLeafRef);
                atomicPath.setNodeIdentifier(yangNodeIdentifier);
                atomicPath.setPathPredicatesList(pathPredicateList);
                absolutePathList.add(atomicPath);
            } else {
                if (path.contains(SLASH_FOR_STRING)) {
                    nodeIdentifier = path.substring(0, path.indexOf(CHAR_OF_SLASH));
                    differentiate[1] = path.substring(path.indexOf(CHAR_OF_SLASH));
                } else {
                    nodeIdentifier = path;
                    differentiate[1] = null;
                }
                yangNodeIdentifier = getValidNodeIdentifierForLeafref(nodeIdentifier, yangConstruct, ctx, yangLeafRef);

                YangAtomicPath atomicPath = new YangAtomicPath();
                atomicPath.setNodeIdentifier(yangNodeIdentifier);
                atomicPath.setPathPredicatesList(null);
                absolutePathList.add(atomicPath);
            }
            if (differentiate[1] == null || differentiate[1].length() == 0) {
                completePathString = null;
            } else {
                completePathString = differentiate[1];
            }
        }
        return absolutePathList;
    }

    /**
     * Validates path predicate in the absolute path's node.
     *
     * @param pathPredicate     list of path predicates in the node of absolute path
     * @param yangConstruct     yang construct for creating error message
     * @param ctx               yang construct's context to get the line number and character position
     * @param yangPathPredicate instance of path predicate where it has to be set
     * @param yangLeafRef       instance of leafref where the path argument has to be set
     * @return list of object of path predicates in absolute path's node
     */
    private static List<YangPathPredicate> validatePathPredicate(List<String> pathPredicate,
                                                                 YangConstructType yangConstruct, ParserRuleContext
                                                                         ctx, YangPathPredicate yangPathPredicate,
                                                                 YangLeafRef yangLeafRef) {

        Iterator<String> pathPredicateString = pathPredicate.iterator();
        List<String> pathEqualityExpression = new ArrayList<>();

        while (pathPredicateString.hasNext()) {
            String pathPredicateForNode = pathPredicateString.next();
            pathPredicateForNode = (pathPredicateForNode.substring(1)).trim();
            pathPredicateForNode = pathPredicateForNode.substring(0,
                    pathPredicateForNode.indexOf(CHAR_OF_CLOSE_SQUARE_BRACKET));
            pathEqualityExpression.add(pathPredicateForNode);
        }
        List<YangPathPredicate> validatedPathPredicateList = validatePathEqualityExpression(pathEqualityExpression,
                yangConstruct, ctx, yangPathPredicate, yangLeafRef);
        return validatedPathPredicateList;
    }

    /**
     * Validates the path equality expression.
     *
     * @param pathEqualityExpression list of path equality expression in the path predicates of the node
     * @param yangConstruct          yang construct for creating error message
     * @param ctx                    yang construct's context to get the line number and character position
     * @param yangPathPredicate      instance of path predicate where it has to be set
     * @param yangLeafRef            instance of leafref where the path argument has to be set
     * @return list of object of path predicates in absolute path's node
     */
    private static List<YangPathPredicate> validatePathEqualityExpression(List<String> pathEqualityExpression,
                                                                          YangConstructType yangConstruct,
                                                                          ParserRuleContext ctx, YangPathPredicate
                                                                                  yangPathPredicate,
                                                                          YangLeafRef yangLeafRef) {

        Iterator<String> pathEqualityExpressionString = pathEqualityExpression.iterator();
        List<YangPathPredicate> yangPathPredicateList = new ArrayList<>();

        while (pathEqualityExpressionString.hasNext()) {
            String pathEqualityExpressionForNode = pathEqualityExpressionString.next();
            String[] pathEqualityExpressionArray = pathEqualityExpressionForNode.split("[=]");

            YangNodeIdentifier yangNodeIdentifierForPredicate;
            YangRelativePath yangRelativePath;
            yangNodeIdentifierForPredicate = getValidNodeIdentifierForLeafref(pathEqualityExpressionArray[0].trim(),
                    yangConstruct, ctx, yangLeafRef);
            yangRelativePath = validatePathKeyExpression(pathEqualityExpressionArray[1].trim(), yangConstruct, ctx,
                    yangLeafRef);
            yangPathPredicate.setNodeIdentifier(yangNodeIdentifierForPredicate);
            yangPathPredicate.setPathOperator(EQUALTO);
            yangPathPredicate.setRightRelativePath(yangRelativePath);
            yangPathPredicateList.add(yangPathPredicate);
        }
        return yangPathPredicateList;
    }

    /**
     * Validate the path key expression.
     *
     * @param rightRelativePath relative path in the path predicate
     * @param yangConstruct     yang construct for creating error message
     * @param ctx               yang construct's context to get the line number and character position
     * @param yangLeafRef       instance of leafref where the path argument has to be set
     * @return object of right relative path in path predicate
     */
    private static YangRelativePath validatePathKeyExpression(String rightRelativePath,
                                                              YangConstructType yangConstruct, ParserRuleContext ctx,
                                                              YangLeafRef yangLeafRef) {

        YangRelativePath yangRelativePath = new YangRelativePath();
        String[] relativePath = rightRelativePath.split(SLASH_FOR_STRING);
        List<String> rightAbsolutePath = new ArrayList<>();
        int accessAncestor = 0;
        for (String path : relativePath) {
            if (path.trim().equals(ANCESTOR_ACCESSOR)) {
                accessAncestor = accessAncestor + 1;
            } else {
                rightAbsolutePath.add(path);
            }
        }
        List<YangAtomicPath> atomicPathInRelativePath = validateRelativePathKeyExpression(rightAbsolutePath,
                yangConstruct, ctx, yangLeafRef);
        yangRelativePath.setAtomicPathList(atomicPathInRelativePath);
        yangRelativePath.setAncestorNodeCount(accessAncestor);
        return yangRelativePath;
    }

    /**
     * Validates the relative path key expression.
     *
     * @param rightAbsolutePath absolute path nodes present in the relative path
     * @param yangConstruct     yang construct for creating error message
     * @param ctx               yang construct's context to get the line number and character position
     * @param yangLeafRef       instance of leafref where the path argument has to be set
     * @return list of object of absolute path nodes present in the relative path
     */
    private static List<YangAtomicPath> validateRelativePathKeyExpression(List<String> rightAbsolutePath,
                                                                            YangConstructType yangConstruct,
                                                                            ParserRuleContext ctx, YangLeafRef
                                                                                    yangLeafRef) {

        List<YangAtomicPath> atomicPathList = new ArrayList<>();
        YangNodeIdentifier yangNodeIdentifier;

        Iterator<String> nodes = rightAbsolutePath.iterator();
        String currentInvocationFunction = nodes.next();
        currentInvocationFunction = currentInvocationFunction.trim();
        String[] currentFunction = currentInvocationFunction.split("[(]");

        if (!(currentFunction[0].trim().equals(CURRENT)) || !(currentFunction[1].trim().equals(CLOSE_PARENTHESIS))) {
            ParserException parserException = new ParserException("YANG file error : "
                    + YangConstructType.getYangConstructType(yangConstruct) + " " + yangLeafRef.getPath() +
                    " does not follow valid path syntax");
            parserException.setLine(ctx.getStart().getLine());
            parserException.setCharPosition(ctx.getStart().getCharPositionInLine());
            throw parserException;
        }

        while (nodes.hasNext()) {
            YangAtomicPath atomicPath = new YangAtomicPath();
            String node = nodes.next();
            yangNodeIdentifier = getValidNodeIdentifierForLeafref(node.trim(), yangConstruct, ctx, yangLeafRef);
            atomicPath.setNodeIdentifier(yangNodeIdentifier);
            atomicPathList.add(atomicPath);
        }
        return atomicPathList;
    }

    /**
     * Validates the match for first path predicate in a given string.
     *
     * @param matchRequiredString string for which match has to be done
     * @return the matched string
     */
    private static String matchForPathPredicate(String matchRequiredString) {

        String matchedString = null;
        java.util.regex.Matcher matcher = PATH_PREDICATE_PATTERN.matcher(matchRequiredString);
        if (matcher.find()) {
            matchedString = matcher.group(0);
        }
        return matchedString;
    }

    /**
     * Checks and return valid absolute schema node id.
     *
     * @param argumentString    string from yang file
     * @param yangConstructType yang construct for creating error message
     * @param ctx               yang construct's context to get the line number and character position
     * @return target nodes list of absolute schema node id
     */
    public static List<YangAtomicPath> getValidAbsoluteSchemaNodeId(String argumentString,
                                                                      YangConstructType yangConstructType,
                                                                      ParserRuleContext ctx) {

        List<YangAtomicPath> targetNodes = new ArrayList<>();
        YangNodeIdentifier yangNodeIdentifier;
        String tmpSchemaNodeId = removeQuotesAndHandleConcat(argumentString);

        // absolute-schema-nodeid = 1*("/" node-identifier)
        if (!tmpSchemaNodeId.startsWith(SLASH)) {
            ParserException parserException = new ParserException("YANG file error : " +
                    YangConstructType.getYangConstructType(yangConstructType) + " name " + argumentString +
                    "is not valid");
            parserException.setLine(ctx.getStart().getLine());
            parserException.setCharPosition(ctx.getStart().getCharPositionInLine());
            throw parserException;
        }
        String[] tmpData = tmpSchemaNodeId.replaceFirst(CARET + SLASH, EMPTY_STRING).split(SLASH);
        for (String nodeIdentifiers : tmpData) {
            yangNodeIdentifier = getValidNodeIdentifier(nodeIdentifiers, yangConstructType, ctx);
            YangAtomicPath yangAbsPath = new YangAtomicPath();
            yangAbsPath.setNodeIdentifier(yangNodeIdentifier);
            targetNodes.add(yangAbsPath);
        }
        return targetNodes;
    }

    /**
     * Throws parser exception for unsupported YANG constructs.
     *
     * @param yangConstructType yang construct for creating error message
     * @param ctx               yang construct's context to get the line number and character position
     * @param errorInfo         error information
     */
    public static void handleUnsupportedYangConstruct(YangConstructType yangConstructType,
                                                      ParserRuleContext ctx, String errorInfo) {
        ParserException parserException = new ParserException(YANG_FILE_ERROR
                + QUOTES + YangConstructType.getYangConstructType(yangConstructType) + QUOTES
                + errorInfo);
        parserException.setLine(ctx.getStart().getLine());
        parserException.setCharPosition(ctx.getStart().getCharPositionInLine());
        throw parserException;
    }

    /**
     * Returns date and makes it in usable format for revision.
     *
     * @param dateInString date argument string from yang file
     * @param ctx          yang construct's context to get the line number and character position
     * @return date format for revision
     */
    public static Date getValidDateFromString(String dateInString, ParserRuleContext ctx) {
        String dateArgument = removeQuotesAndHandleConcat(dateInString);
        if (dateArgument == null || !dateArgument.matches(DATE_PATTERN)) {
            ParserException parserException = new ParserException("YANG file error: Input date is not correct");
            parserException.setLine(ctx.getStart().getLine());
            parserException.setCharPosition(ctx.getStart().getCharPositionInLine());
            throw parserException;
        }

        SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);
        sdf.setLenient(false);

        try {
            //if not valid, it will throw ParseException
            Date date = sdf.parse(dateArgument);
            return date;
        } catch (ParseException e) {
            ParserException parserException = new ParserException("YANG file error: Input date is not correct");
            parserException.setLine(ctx.getStart().getLine());
            parserException.setCharPosition(ctx.getStart().getCharPositionInLine());
            throw parserException;
        }
    }
}
