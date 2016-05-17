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

package org.onosproject.yangutils.translator.tojava.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.onosproject.yangutils.datamodel.YangNode;
import org.onosproject.yangutils.translator.exception.TranslatorException;
import org.onosproject.yangutils.translator.tojava.JavaFileInfoContainer;
import org.onosproject.yangutils.translator.tojava.JavaFileInfo;

import static org.onosproject.yangutils.utils.UtilConstants.COLAN;
import static org.onosproject.yangutils.utils.UtilConstants.DEFAULT_BASE_PKG;
import static org.onosproject.yangutils.utils.UtilConstants.EMPTY_STRING;
import static org.onosproject.yangutils.utils.UtilConstants.HYPHEN;
import static org.onosproject.yangutils.utils.UtilConstants.JAVA_KEY_WORDS;
import static org.onosproject.yangutils.utils.UtilConstants.PERIOD;
import static org.onosproject.yangutils.utils.UtilConstants.QUOTES;
import static org.onosproject.yangutils.utils.UtilConstants.REGEX_FOR_DIGITS_WITH_SINGLE_LETTER;
import static org.onosproject.yangutils.utils.UtilConstants.REGEX_FOR_FIRST_DIGIT;
import static org.onosproject.yangutils.utils.UtilConstants.REGEX_FOR_HYPHEN;
import static org.onosproject.yangutils.utils.UtilConstants.REGEX_FOR_IDENTIFIER_SPECIAL_CHAR;
import static org.onosproject.yangutils.utils.UtilConstants.REGEX_FOR_PERIOD;
import static org.onosproject.yangutils.utils.UtilConstants.REGEX_FOR_SINGLE_LETTER;
import static org.onosproject.yangutils.utils.UtilConstants.REGEX_FOR_UNDERSCORE;
import static org.onosproject.yangutils.utils.UtilConstants.REGEX_WITH_ALL_SPECIAL_CHAR;
import static org.onosproject.yangutils.utils.UtilConstants.REGEX_WITH_DIGITS;
import static org.onosproject.yangutils.utils.UtilConstants.REGEX_WITH_SINGLE_CAPITAL_CASE;
import static org.onosproject.yangutils.utils.UtilConstants.REGEX_WITH_SINGLE_CAPITAL_CASE_AND_DIGITS_SMALL_CASES;
import static org.onosproject.yangutils.utils.UtilConstants.REGEX_WITH_UPPERCASE;
import static org.onosproject.yangutils.utils.UtilConstants.REVISION_PREFIX;
import static org.onosproject.yangutils.utils.UtilConstants.SLASH;
import static org.onosproject.yangutils.utils.UtilConstants.UNDER_SCORE;
import static org.onosproject.yangutils.utils.UtilConstants.VERSION_PREFIX;
import static org.onosproject.yangutils.utils.UtilConstants.YANG_AUTO_PREFIX;

/**
 * Represents an utility Class for translating the name from YANG to java convention.
 */
public final class JavaIdentifierSyntax {

    private static final int MAX_MONTHS = 12;
    private static final int MAX_DAYS = 31;
    private static final int INDEX_ZERO = 0;
    private static final int INDEX_ONE = 1;
    private static final int INDEX_TWO = 2;
    private static final int VALUE_CHECK = 10;
    private static final String ZERO = "0";

    /**
     * Create instance of java identifier syntax.
     */
    private JavaIdentifierSyntax() {
    }

    /**
     * Returns the root package string.
     *
     * @param version YANG version
     * @param nameSpace name space of the module
     * @param revision revision of the module defined
     * @return returns the root package string
     */
    public static String getRootPackage(byte version, String nameSpace, String revision) {

        String pkg;
        pkg = DEFAULT_BASE_PKG;
        pkg = pkg + PERIOD;
        pkg = pkg + getYangVersion(version);
        pkg = pkg + PERIOD;
        pkg = pkg + getPkgFromNameSpace(nameSpace);
        pkg = pkg + PERIOD;
        pkg = pkg + getYangRevisionStr(revision);

        return pkg.toLowerCase();
    }

    /**
     * Returns the contained data model parent node.
     *
     * @param currentNode current node which parent contained node is required
     * @return parent node in which the current node is an attribute
     */
    public static YangNode getParentNodeInGenCode(YangNode currentNode) {

        /*
         * TODO: recursive parent lookup to support choice/augment/uses. TODO:
         * need to check if this needs to be updated for
         * choice/case/augment/grouping
         */
        return currentNode.getParent();
    }

    /**
     * Returns the node package string.
     *
     * @param curNode current java node whose package string needs to be set
     * @return returns the root package string
     */
    public static String getCurNodePackage(YangNode curNode) {

        String pkg;
        if (!(curNode instanceof JavaFileInfoContainer)
                || curNode.getParent() == null) {
            throw new TranslatorException("missing parent node to get current node's package");
        }

        YangNode parentNode = getParentNodeInGenCode(curNode);
        if (!(parentNode instanceof JavaFileInfoContainer)) {
            throw new TranslatorException("missing parent java node to get current node's package");
        }
        JavaFileInfo parentJavaFileHandle = ((JavaFileInfoContainer) parentNode).getJavaFileInfo();
        pkg = parentJavaFileHandle.getPackage() + PERIOD + parentJavaFileHandle.getJavaName();
        return pkg.toLowerCase();
    }

    /**
     * Returns version.
     *
     * @param ver YANG version
     * @return version
     */
    private static String getYangVersion(byte ver) {
        return VERSION_PREFIX + ver;
    }

    /**
     * Returns package name from name space.
     *
     * @param nameSpace name space of YANG module
     * @return java package name as per java rules
     */
    private static String getPkgFromNameSpace(String nameSpace) {

        ArrayList<String> pkgArr = new ArrayList<String>();
        nameSpace = nameSpace.replace(QUOTES, EMPTY_STRING);
        String properNameSpace = nameSpace.replaceAll(REGEX_WITH_ALL_SPECIAL_CHAR, COLAN);
        String[] nameSpaceArr = properNameSpace.split(COLAN);

        for (String nameSpaceString : nameSpaceArr) {
            pkgArr.add(nameSpaceString);
        }
        return getPkgFrmArr(pkgArr);
    }

    /**
     * Returns revision string array.
     *
     * @param date YANG module revision
     * @return revision string
     * @throws TranslatorException when date is invalid.
     */
    private static String getYangRevisionStr(String date) throws TranslatorException {

        String[] revisionArr = date.split(HYPHEN);

        String rev = REVISION_PREFIX;
        rev = rev + revisionArr[INDEX_ZERO];

        if (Integer.parseInt(revisionArr[INDEX_ONE]) <= MAX_MONTHS
                && Integer.parseInt(revisionArr[INDEX_TWO]) <= MAX_DAYS) {
            for (int i = INDEX_ONE; i < revisionArr.length; i++) {

                Integer val = Integer.parseInt(revisionArr[i]);
                if (val < VALUE_CHECK) {
                    rev = rev + ZERO;
                }
                rev = rev + val;
            }

            return rev;
        } else {
            throw new TranslatorException("Date in revision is not proper: " + date);
        }
    }

    /**
     * Returns the package string.
     *
     * @param pkgArr package array
     * @return package string
     */
    private static String getPkgFrmArr(ArrayList<String> pkgArr) {

        String pkg = EMPTY_STRING;
        int size = pkgArr.size();
        int i = 0;
        for (String member : pkgArr) {
            boolean presenceOfKeyword = JAVA_KEY_WORDS.contains(member);
            if (presenceOfKeyword || member.matches(REGEX_FOR_FIRST_DIGIT)) {
                member = YANG_AUTO_PREFIX + member;
            }
            pkg = pkg + member;
            if (i != size - 1) {
                pkg = pkg + PERIOD;
            }
            i++;
        }
        return pkg;
    }

    /**
     * Returns package sub name from YANG identifier name.
     *
     * @param name YANG identifier name
     * @return java package sub name as per java rules
     */
    public static String getSubPkgFromName(String name) {

        ArrayList<String> pkgArr = new ArrayList<String>();
        String[] nameArr = name.split(COLAN);

        for (String nameString : nameArr) {
            pkgArr.add(nameString);
        }
        return getPkgFrmArr(pkgArr);
    }

    /**
     * Returns the YANG identifier name as java identifier.
     *
     * @param yangIdentifier identifier in YANG file
     * @param conflictResolver object of YANG to java naming conflict util
     * @return corresponding java identifier
     */
    public static String getCamelCase(String yangIdentifier, YangToJavaNamingConflictUtil conflictResolver) {

        if (conflictResolver != null) {
            String replacementForHyphen = conflictResolver.getReplacementForHyphen();
            String replacementForPeriod = conflictResolver.getReplacementForPeriod();
            String replacementForUnderscore = conflictResolver.getReplacementForUnderscore();
            if (replacementForPeriod != null) {
                yangIdentifier = yangIdentifier.replaceAll(REGEX_FOR_PERIOD,
                        PERIOD + replacementForPeriod.toLowerCase() + PERIOD);
            }
            if (replacementForUnderscore != null) {
                yangIdentifier = yangIdentifier.replaceAll(REGEX_FOR_UNDERSCORE,
                        UNDER_SCORE + replacementForUnderscore.toLowerCase() + UNDER_SCORE);
            }
            if (replacementForHyphen != null) {
                yangIdentifier = yangIdentifier.replaceAll(REGEX_FOR_HYPHEN,
                        HYPHEN + replacementForHyphen.toLowerCase() + HYPHEN);
            }
        }
        yangIdentifier = yangIdentifier.replaceAll(REGEX_FOR_IDENTIFIER_SPECIAL_CHAR, COLAN);
        String[] strArray = yangIdentifier.split(COLAN);
        if (strArray[0].isEmpty()) {
            List<String> stringArrangement = new ArrayList<String>();
            for (int i = 1; i < strArray.length; i++) {
                stringArrangement.add(strArray[i]);
            }
            strArray = stringArrangement.toArray(new String[stringArrangement.size()]);
        }
        return upperCaseConflictResolver(strArray);
    }

    /**
     * Resolves the conflict when input has uppercase.
     *
     * @param stringArray containing strings for uppercase conflict resolver
     * @return camel cased string
     */
    private static String upperCaseConflictResolver(String[] stringArray) {

        for (int l = 0; l < stringArray.length; l++) {
            String[] upperCaseSplitArray = stringArray[l].split(REGEX_WITH_UPPERCASE);
            for (int m = 0; m < upperCaseSplitArray.length; m++) {
                if (upperCaseSplitArray[m].matches(REGEX_WITH_SINGLE_CAPITAL_CASE)) {
                    int check = m;
                    while (check + 1 < upperCaseSplitArray.length) {
                        if (upperCaseSplitArray[check + 1].matches(REGEX_WITH_SINGLE_CAPITAL_CASE)) {
                            upperCaseSplitArray[check + 1] = upperCaseSplitArray[check + 1].toLowerCase();
                            check = check + 1;
                        } else if (upperCaseSplitArray[check + 1]
                                .matches(REGEX_WITH_SINGLE_CAPITAL_CASE_AND_DIGITS_SMALL_CASES)) {
                            upperCaseSplitArray[check + 1] = upperCaseSplitArray[check + 1].toLowerCase();
                            break;
                        } else {
                            break;
                        }
                    }
                }
            }
            StringBuilder strBuilder = new StringBuilder();
            for (String element : upperCaseSplitArray) {
                strBuilder.append(element);
            }
            stringArray[l] = strBuilder.toString();
        }
        List<String> result = new ArrayList<String>();
        for (String element : stringArray) {
            String[] capitalCaseSplitArray = element.split(REGEX_WITH_UPPERCASE);
            for (String letter : capitalCaseSplitArray) {
                String[] arrayForAddition = letter.split(REGEX_WITH_DIGITS);
                List<String> list = Arrays.asList(arrayForAddition);
                for (String str : list) {
                    if (str != null && !str.isEmpty()) {
                        result.add(str);
                    }
                }
            }
        }
        stringArray = result.toArray(new String[result.size()]);
        return applyCamelCaseRule(stringArray);
    }

    /**
     * Applies the rule that a string does not end with a capitalized letter and capitalizes
     * the letter next to a number in an array.
     *
     * @param stringArray containing strings for camel case separation
     * @return camel case rule checked string
     */
    private static String applyCamelCaseRule(String[] stringArray) {

        String ruleChecker = stringArray[0].toLowerCase();
        int i;
        if (ruleChecker.matches(REGEX_FOR_FIRST_DIGIT)) {
            i = 0;
            ruleChecker = EMPTY_STRING;
        } else {
            i = 1;
        }
        for (; i < stringArray.length; i++) {
            if ((i + 1) == stringArray.length) {
                if (stringArray[i].matches(REGEX_FOR_SINGLE_LETTER)
                        || stringArray[i].matches(REGEX_FOR_DIGITS_WITH_SINGLE_LETTER)) {
                    ruleChecker = ruleChecker + stringArray[i].toLowerCase();
                    break;
                }
            }
            if (stringArray[i].matches(REGEX_FOR_FIRST_DIGIT)) {
                for (int j = 0; j < stringArray[i].length(); j++) {
                    char letterCheck = stringArray[i].charAt(j);
                    if (Character.isLetter(letterCheck)) {
                        stringArray[i] = stringArray[i].substring(0, j)
                                + stringArray[i].substring(j, j + 1).toUpperCase() + stringArray[i].substring(j + 1);
                        break;
                    }
                }
                ruleChecker = ruleChecker + stringArray[i];
            } else {
                ruleChecker = ruleChecker + stringArray[i].substring(0, 1).toUpperCase() + stringArray[i].substring(1);
            }
        }
        String ruleCheckerWithPrefix = addPrefix(ruleChecker);
        return restrictConsecutiveCapitalCase(ruleCheckerWithPrefix);
    }

    /**
     * Adds prefix YANG auto prefix if the string begins with digit or is a java key word.
     *
     * @param camelCasePrefix string for adding prefix
     * @return prefixed camel case string
     */
    private static String addPrefix(String camelCasePrefix) {

        if (camelCasePrefix.matches(REGEX_FOR_FIRST_DIGIT)) {
            camelCasePrefix = YANG_AUTO_PREFIX + camelCasePrefix;
        }
        if (JAVA_KEY_WORDS.contains(camelCasePrefix.toLowerCase())) {
            camelCasePrefix = YANG_AUTO_PREFIX + camelCasePrefix.substring(0, 1).toUpperCase()
                    + camelCasePrefix.substring(1);
        }
        return camelCasePrefix;
    }

    /**
     * Restricts consecutive capital cased string as a rule in camel case.
     *
     * @param consecCapitalCaseRemover which requires the restriction of consecutive capital case
     * @return string without consecutive capital case
     */
    private static String restrictConsecutiveCapitalCase(String consecCapitalCaseRemover) {

        for (int k = 0; k < consecCapitalCaseRemover.length(); k++) {
            if (k + 1 < consecCapitalCaseRemover.length()) {
                if (Character.isUpperCase(consecCapitalCaseRemover.charAt(k))) {
                    if (Character.isUpperCase(consecCapitalCaseRemover.charAt(k + 1))) {
                        consecCapitalCaseRemover = consecCapitalCaseRemover.substring(0, k + 1)
                                + consecCapitalCaseRemover.substring(k + 1, k + 2).toLowerCase()
                                + consecCapitalCaseRemover.substring(k + 2);
                    }
                }
            }
        }
        return consecCapitalCaseRemover;
    }

    /**
     * Returns the YANG identifier name as java identifier with first letter
     * in capital.
     *
     * @param yangIdentifier identifier in YANG file
     * @return corresponding java identifier
     */
    public static String getCapitalCase(String yangIdentifier) {
        yangIdentifier = yangIdentifier.substring(0, 1).toUpperCase() + yangIdentifier.substring(1);
        return restrictConsecutiveCapitalCase(yangIdentifier);
    }

    /**
     * Returns the YANG identifier name as java identifier with first letter
     * in small.
     *
     * @param yangIdentifier identifier in YANG file.
     * @return corresponding java identifier
     */
    public static String getSmallCase(String yangIdentifier) {
        return yangIdentifier.substring(0, 1).toLowerCase() + yangIdentifier.substring(1);
    }

    /**
     * Returns the java Package from package path.
     *
     * @param packagePath package path
     * @return java package
     */
    public static String getJavaPackageFromPackagePath(String packagePath) {
        return packagePath.replace(SLASH, PERIOD);
    }

    /**
     * Returns enum's java name.
     *
     * @param name enum's name
     * @return enum's java name
     */
    public static String getEnumJavaAttribute(String name) {

        String[] strArray = name.split(HYPHEN);
        String output = EMPTY_STRING;
        for (int i = 0; i < strArray.length; i++) {
            output = output + strArray[i];
            if (i > 0 && i < strArray.length - 1) {
                output = output + UNDER_SCORE;
            }
        }
        return output;
    }

    /**
     * Returns the directory path corresponding to java package.
     *
     * @param packagePath package path
     * @return java package
     */
    public static String getPackageDirPathFromJavaJPackage(String packagePath) {
        return packagePath.replace(PERIOD, SLASH);
    }
}
