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

import org.onosproject.yangutils.utils.UtilConstants;

/**
 * Utility Class for translating the name from YANG to java convention.
 */
public final class JavaIdentifierSyntax {

    /**
     * Util class, with static functions only.
     */
    private JavaIdentifierSyntax() {
    }

    /**
     * Get the root package string.
     *
     * @param version YANG version.
     * @param nameSpace name space of the module.
     * @param revision revision of the module defined
     * @return returns the root package string.
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

        return pkg;
    }

    /**
     * Returns version.
     *
     * @param ver YANG version.
     * @return version
     */
    private static String getYangVersion(byte ver) {
        return "v" + ver;
    }

    /**
     * Get package name from name space.
     *
     * @param nameSpace name space of YANG module
     *  @return java package name as per java rules.
     */
    public static String getPkgFromNameSpace(String nameSpace) {
        ArrayList<String> pkgArr = new ArrayList<String>();
        nameSpace = nameSpace.replace("\"", "");

        String[] nameSpaceArr = nameSpace.split(UtilConstants.COLAN);

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
     */
    public static String getYangRevisionStr(String date) {
        String[] revisionArr = date.split(UtilConstants.HYPHEN);

        String rev = "rev";
        for (String element : revisionArr) {
            Integer val = Integer.parseInt(element);
            if (val < 10) {
                rev = rev + "0";
            }
            rev = rev + val;
        }
        return rev;
    }

    /**
     * Returns the package string.
     *
     * @param pkgArr package array
     * @return package string
     */
    public static String getPkgFrmArr(ArrayList<String> pkgArr) {

        String pkg = "";
        int size = pkgArr.size();
        int i = 0;
        for (String member : pkgArr) {
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
     * @param parentPkg parent's package.
     * @param childName child's name.
     * @return package string.
     */
    public static String getPackageFromParent(String parentPkg, String childName) {
        return parentPkg + UtilConstants.PERIOD + getSubPkgFromName(childName);
    }

    /**
     * Get package sub name from YANG identifier name.
     *
     * @param name YANG identifier name.
     * @return java package sub name as per java rules.
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
     * @param yangIdentifier identifier in YANG file.
     * @return corresponding java identifier
     */
    public static String getCamelCase(String yangIdentifier) {
        String[] strArray = yangIdentifier.split(UtilConstants.HYPHEN);
        String camelCase = strArray[0];
        for (int i = 1; i < strArray.length; i++) {
            camelCase = camelCase + (strArray[i].substring(0, 1).toUpperCase() + strArray[i].substring(1));
        }
        return camelCase;
    }
}
