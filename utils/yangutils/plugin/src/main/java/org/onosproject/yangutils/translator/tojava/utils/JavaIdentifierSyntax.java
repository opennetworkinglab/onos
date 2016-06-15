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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.onosproject.yangutils.datamodel.YangNode;
import org.onosproject.yangutils.translator.exception.TranslatorException;
import org.onosproject.yangutils.translator.tojava.JavaFileInfo;
import org.onosproject.yangutils.translator.tojava.JavaFileInfoContainer;
import org.onosproject.yangutils.utils.io.impl.YangIoUtils;
import org.onosproject.yangutils.utils.io.impl.YangToJavaNamingConflictUtil;

import static org.onosproject.yangutils.datamodel.utils.DataModelUtils.getParentNodeInGenCode;
import static org.onosproject.yangutils.utils.UtilConstants.COLAN;
import static org.onosproject.yangutils.utils.UtilConstants.DEFAULT_BASE_PKG;
import static org.onosproject.yangutils.utils.UtilConstants.EMPTY_STRING;
import static org.onosproject.yangutils.utils.UtilConstants.HYPHEN;
import static org.onosproject.yangutils.utils.UtilConstants.JAVA_KEY_WORDS;
import static org.onosproject.yangutils.utils.UtilConstants.PERIOD;
import static org.onosproject.yangutils.utils.UtilConstants.QUOTES;
import static org.onosproject.yangutils.utils.UtilConstants.REGEX_FOR_FIRST_DIGIT;
import static org.onosproject.yangutils.utils.UtilConstants.REGEX_WITH_ALL_SPECIAL_CHAR;
import static org.onosproject.yangutils.utils.UtilConstants.REVISION_PREFIX;
import static org.onosproject.yangutils.utils.UtilConstants.SLASH;
import static org.onosproject.yangutils.utils.UtilConstants.UNDER_SCORE;
import static org.onosproject.yangutils.utils.UtilConstants.VERSION_PREFIX;
import static org.onosproject.yangutils.utils.io.impl.YangIoUtils.addPackageInfo;
import static org.onosproject.yangutils.utils.io.impl.YangIoUtils.createDirectories;
import static org.onosproject.yangutils.utils.io.impl.YangIoUtils.getAbsolutePackagePath;
import static org.onosproject.yangutils.utils.io.impl.YangIoUtils.getJavaPackageFromPackagePath;
import static org.onosproject.yangutils.utils.io.impl.YangIoUtils.getPackageDirPathFromJavaJPackage;

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
     * @param version          YANG version
     * @param nameSpace        name space of the module
     * @param revision         revision of the module defined
     * @param conflictResolver object of YANG to java naming conflict util
     * @return the root package string
     */
    public static String getRootPackage(byte version, String nameSpace, String revision,
                                        YangToJavaNamingConflictUtil conflictResolver) {

        String pkg;
        pkg = DEFAULT_BASE_PKG;
        pkg = pkg + PERIOD;
        pkg = pkg + getYangVersion(version);
        pkg = pkg + PERIOD;
        pkg = pkg + getPkgFromNameSpace(nameSpace, conflictResolver);
        pkg = pkg + PERIOD;
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
        return VERSION_PREFIX + ver;
    }

    /**
     * Returns package name from name space.
     *
     * @param nameSpace        name space of YANG module
     * @param conflictResolver object of YANG to java naming conflict util
     * @return java package name as per java rules
     */
    private static String getPkgFromNameSpace(String nameSpace, YangToJavaNamingConflictUtil conflictResolver) {

        ArrayList<String> pkgArr = new ArrayList<String>();
        nameSpace = nameSpace.replace(QUOTES, EMPTY_STRING);
        String properNameSpace = nameSpace.replaceAll(REGEX_WITH_ALL_SPECIAL_CHAR, COLAN);
        String[] nameSpaceArr = properNameSpace.split(COLAN);

        for (String nameSpaceString : nameSpaceArr) {
            pkgArr.add(nameSpaceString);
        }
        return getPkgFrmArr(pkgArr, conflictResolver);
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
     * @param pkgArr           package array
     * @param conflictResolver object of YANG to java naming conflict util
     * @return package string
     */
    private static String getPkgFrmArr(ArrayList<String> pkgArr, YangToJavaNamingConflictUtil conflictResolver) {

        String pkg = EMPTY_STRING;
        int size = pkgArr.size();
        int i = 0;
        for (String member : pkgArr) {
            boolean presenceOfKeyword = JAVA_KEY_WORDS.contains(member.toLowerCase());
            if (presenceOfKeyword || member.matches(REGEX_FOR_FIRST_DIGIT)) {
                String prefix = YangIoUtils.getPrefixForIdentifier(conflictResolver);
                member = prefix + member;
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
     * Returns enum's java name.
     *
     * @param name enum's name
     * @return enum's java name
     */
    public static String getEnumJavaAttribute(String name) {

        name = name.replaceAll(REGEX_WITH_ALL_SPECIAL_CHAR, COLAN);
        String[] strArray = name.split(COLAN);
        String output = EMPTY_STRING;
        if (strArray[0].isEmpty()) {
            List<String> stringArrangement = new ArrayList<String>();
            for (int i = 1; i < strArray.length; i++) {
                stringArrangement.add(strArray[i]);
            }
            strArray = stringArrangement.toArray(new String[stringArrangement.size()]);
        }
        for (int i = 0; i < strArray.length; i++) {
            if (i > 0 && i < strArray.length) {
                output = output + UNDER_SCORE;
            }
            output = output + strArray[i];
        }
        return output;
    }

    /**
     * Creates a package structure with package info java file if not present.
     *
     * @param yangNode YANG node for which code is being generated
     * @throws IOException any IO exception
     */
    public static void createPackage(YangNode yangNode) throws IOException {
        if (!(yangNode instanceof JavaFileInfoContainer)) {
            throw new TranslatorException("current node must have java file info");
        }
        String pkgInfo;
        JavaFileInfo javaFileInfo = ((JavaFileInfoContainer) yangNode).getJavaFileInfo();
        String pkg = getAbsolutePackagePath(javaFileInfo.getBaseCodeGenPath(), javaFileInfo.getPackageFilePath());
        if (!doesPackageExist(pkg)) {
            try {
                File pack = createDirectories(pkg);
                YangNode parent = getParentNodeInGenCode(yangNode);
                if (parent != null) {
                    pkgInfo = ((JavaFileInfoContainer) parent).getJavaFileInfo().getJavaName();
                    addPackageInfo(pack, pkgInfo, getJavaPackageFromPackagePath(pkg), true,
                            ((JavaFileInfoContainer) parent).getJavaFileInfo().getPluginConfig());
                } else {
                    pkgInfo = ((JavaFileInfoContainer) yangNode).getJavaFileInfo().getJavaName();
                    addPackageInfo(pack, pkgInfo, getJavaPackageFromPackagePath(pkg), false,
                            ((JavaFileInfoContainer) yangNode).getJavaFileInfo().getPluginConfig());
                }
            } catch (IOException e) {
                throw new IOException("failed to create package-info file");
            }
        }
    }

    /**
     * Checks if the package directory structure created.
     *
     * @param pkg Package to check if it is created
     * @return existence status of package
     */
    public static boolean doesPackageExist(String pkg) {
        File pkgDir = new File(getPackageDirPathFromJavaJPackage(pkg));
        File pkgWithFile = new File(pkgDir + SLASH + "package-info.java");
        return pkgDir.exists() && pkgWithFile.isFile();
    }
}
