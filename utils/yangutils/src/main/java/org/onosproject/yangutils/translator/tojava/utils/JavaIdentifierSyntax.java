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

package org.onosproject.yangutils.translator.tojava.utils;

import java.util.ArrayList;

import org.onosproject.yangutils.translator.exception.TranslatorException;
import org.onosproject.yangutils.utils.UtilConstants;

/**
 * Utility Class for translating the name from YANG to java convention.
 */
public final class JavaIdentifierSyntax {

    private static final int MAX_MONTHS = 12;
    private static final int MAX_DAYS = 31;
    private static final int INDEX_ZERO = 0;
    private static final int INDEX_ONE = 1;
    private static final int INDEX_TWO = 2;

    /**
     * Default constructor.
     */
    private JavaIdentifierSyntax() {
    }

    /**
     * Get the root package string.
     *
     * @param version YANG version
     * @param nameSpace name space of the module
     * @param revision revision of the module defined
     * @return returns the root package string
     */
    public static String getRootPackage(byte version, String nameSpace, String revision) {

        String pkg;
        pkg = UtilConstants.DEFAULT_BASE_PKG;
        pkg = pkg + UtilConstants.PERIOD;
        pkg = pkg + getYangVersion(version);
        pkg = pkg + UtilConstants.PERIOD;
        pkg = pkg + getPkgFromNameSpace(nameSpace);
        pkg = pkg + UtilConstants.PERIOD;
        pkg = pkg + getYangRevisionStr(revision);

        return pkg.toLowerCase();
    }

    /**
     * Returns version.
     *
     * @param ver YANG version
     * @return version
     */
    private static String getYangVersion(byte ver) {
        return "v" + ver;
    }

    /**
     * Get package name from name space.
     *
     * @param nameSpace name space of YANG module
     * @return java package name as per java rules
     */
    public static String getPkgFromNameSpace(String nameSpace) {
        ArrayList<String> pkgArr = new ArrayList<String>();
        nameSpace = nameSpace.replace(UtilConstants.QUOTES, UtilConstants.EMPTY_STRING);
        String properNameSpace = nameSpace.replaceAll(UtilConstants.REGEX_WITH_SPECIAL_CHAR, UtilConstants.COLAN);
        String[] nameSpaceArr = properNameSpace.split(UtilConstants.COLAN);

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
    public static String getYangRevisionStr(String date) throws TranslatorException {
        String[] revisionArr = date.split(UtilConstants.HYPHEN);

        String rev = "rev";
        rev = rev + revisionArr[INDEX_ZERO];

        if ((Integer.parseInt(revisionArr[INDEX_ONE]) <= MAX_MONTHS)
                && Integer.parseInt(revisionArr[INDEX_TWO]) <= MAX_DAYS) {
            for (int i = INDEX_ONE; i < revisionArr.length; i++) {

                Integer val = Integer.parseInt(revisionArr[i]);
                if (val < 10) {
                    rev = rev + "0";
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
    public static String getPkgFrmArr(ArrayList<String> pkgArr) {

        String pkg = UtilConstants.EMPTY_STRING;
        int size = pkgArr.size();
        int i = 0;
        for (String member : pkgArr) {
            boolean presenceOfKeyword = UtilConstants.JAVA_KEY_WORDS.contains(member);
            if (presenceOfKeyword || (member.matches(UtilConstants.REGEX_FOR_FIRST_DIGIT))) {
                member = UtilConstants.UNDER_SCORE + member;
            }
            pkg = pkg + member;
            if (i != size - 1) {
                pkg = pkg + UtilConstants.PERIOD;
            }
            i++;
        }
        return pkg;
    }

    /**
     * Get the package from parent's package and string.
     *
     * @param parentPkg parent's package
     * @param parentName parent's name
     * @return package string
     */
    public static String getPackageFromParent(String parentPkg, String parentName) {
        return (parentPkg + UtilConstants.PERIOD + getSubPkgFromName(parentName)).toLowerCase();
    }

    /**
     * Get package sub name from YANG identifier name.
     *
     * @param name YANG identifier name
     * @return java package sub name as per java rules
     */
    public static String getSubPkgFromName(String name) {
        ArrayList<String> pkgArr = new ArrayList<String>();
        String[] nameArr = name.split(UtilConstants.COLAN);

        for (String nameString : nameArr) {
            pkgArr.add(nameString);
        }
        return getPkgFrmArr(pkgArr);
    }

    /**
     * Translate the YANG identifier name to java identifier.
     *
     * @param yangIdentifier identifier in YANG file
     * @return corresponding java identifier
     */
    public static String getCamelCase(String yangIdentifier) {
        String[] strArray = yangIdentifier.split(UtilConstants.HYPHEN);
        String camelCase = strArray[0];
        for (int i = 1; i < strArray.length; i++) {
            camelCase = camelCase + strArray[i].substring(0, 1).toUpperCase() + strArray[i].substring(1);
        }
        return camelCase;
    }

    /**
     * Translate the YANG identifier name to java identifier with first letter
     * in caps.
     *
     * @param yangIdentifier identifier in YANG file
     * @return corresponding java identifier
     */
    public static String getCaptialCase(String yangIdentifier) {
        return yangIdentifier.substring(0, 1).toUpperCase() + yangIdentifier.substring(1);
    }

    /**
     * Translate the YANG identifier name to java identifier with first letter in small.
     *
     * @param yangIdentifier identifier in YANG file.
     * @return corresponding java identifier
     */
    public static String getLowerCase(String yangIdentifier) {
        return yangIdentifier.substring(0, 1).toLowerCase() + yangIdentifier.substring(1);
    }
}
