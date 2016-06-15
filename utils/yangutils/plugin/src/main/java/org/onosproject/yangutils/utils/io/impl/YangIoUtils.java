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

package org.onosproject.yangutils.utils.io.impl;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Stack;
import java.util.regex.Pattern;
import org.apache.commons.io.FileUtils;
import org.onosproject.yangutils.translator.exception.TranslatorException;

import static org.onosproject.yangutils.utils.UtilConstants.COLAN;
import static org.onosproject.yangutils.utils.UtilConstants.COMMA;
import static org.onosproject.yangutils.utils.UtilConstants.EMPTY_STRING;
import static org.onosproject.yangutils.utils.UtilConstants.HASH;
import static org.onosproject.yangutils.utils.UtilConstants.HYPHEN;
import static org.onosproject.yangutils.utils.UtilConstants.JAVA_KEY_WORDS;
import static org.onosproject.yangutils.utils.UtilConstants.NEW_LINE;
import static org.onosproject.yangutils.utils.UtilConstants.OPEN_PARENTHESIS;
import static org.onosproject.yangutils.utils.UtilConstants.ORG;
import static org.onosproject.yangutils.utils.UtilConstants.PACKAGE;
import static org.onosproject.yangutils.utils.UtilConstants.PERIOD;
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
import static org.onosproject.yangutils.utils.UtilConstants.SEMI_COLAN;
import static org.onosproject.yangutils.utils.UtilConstants.SLASH;
import static org.onosproject.yangutils.utils.UtilConstants.SPACE;
import static org.onosproject.yangutils.utils.UtilConstants.TWELVE_SPACE_INDENTATION;
import static org.onosproject.yangutils.utils.UtilConstants.UNDER_SCORE;
import static org.onosproject.yangutils.utils.UtilConstants.YANG_AUTO_PREFIX;
import static org.onosproject.yangutils.utils.io.impl.FileSystemUtil.appendFileContents;
import static org.onosproject.yangutils.utils.io.impl.FileSystemUtil.updateFileHandle;
import static org.onosproject.yangutils.utils.io.impl.JavaDocGen.JavaDocType.PACKAGE_INFO;
import static org.onosproject.yangutils.utils.io.impl.JavaDocGen.getJavaDoc;

/**
 * Represents common utility functionalities for code generation.
 */
public final class YangIoUtils {

    private static final int LINE_SIZE = 118;
    private static final int SUB_LINE_SIZE = 112;
    private static final int ZERO = 0;

    /**
     * Creates an instance of YANG io utils.
     */
    private YangIoUtils() {
    }

    /**
     * Creates the directory structure.
     *
     * @param path directory path
     * @return directory structure
     */
    public static File createDirectories(String path) {
        File generatedDir = new File(path);
        generatedDir.mkdirs();
        return generatedDir;
    }

    /**
     * Adds package info file for the created directory.
     *
     * @param path         directory path
     * @param classInfo    class info for the package
     * @param pack         package of the directory
     * @param isChildNode  is it a child node
     * @param pluginConfig plugin configurations
     * @throws IOException when fails to create package info file
     */
    public static void addPackageInfo(File path, String classInfo, String pack, boolean isChildNode,
            YangPluginConfig pluginConfig)
            throws IOException {

        pack = parsePkg(pack);

        try {

            File packageInfo = new File(path + SLASH + "package-info.java");
            packageInfo.createNewFile();

            FileWriter fileWriter = new FileWriter(packageInfo);
            BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);

            bufferedWriter.write(CopyrightHeader.getCopyrightHeader());
            bufferedWriter.write(getJavaDoc(PACKAGE_INFO, classInfo, isChildNode, pluginConfig));
            String pkg = PACKAGE + SPACE + pack + SEMI_COLAN;
            if (pkg.length() > LINE_SIZE) {
                pkg = whenDelimiterIsPersent(pkg, LINE_SIZE);
            }
            bufferedWriter.write(pkg);
            bufferedWriter.close();
            fileWriter.close();
        } catch (IOException e) {
            throw new IOException("Exception occured while creating package info file.");
        }
    }

    /**
     * Parses package and returns updated package.
     *
     * @param pack package needs to be updated
     * @return updated package
     */
    public static String parsePkg(String pack) {

        if (pack.contains(ORG)) {
            String[] strArray = pack.split(ORG);
            if (strArray.length >= 3) {
                for (int i = 1; i < strArray.length; i++) {
                    if (i == 1) {
                        pack = ORG + strArray[1];
                    } else {
                        pack = pack + ORG + strArray[i];
                    }
                }
            } else {
                pack = ORG + strArray[1];
            }
        }

        return pack;
    }

    /**
     * Cleans the generated directory if already exist in source folder.
     *
     * @param dir generated directory in previous build
     * @throws IOException when failed to delete directory
     */
    public static void deleteDirectory(String dir)
            throws IOException {
        File generatedDirectory = new File(dir);
        if (generatedDirectory.exists()) {
            try {
                FileUtils.deleteDirectory(generatedDirectory);
            } catch (IOException e) {
                throw new IOException(
                        "Failed to delete the generated files in " + generatedDirectory + " directory");
            }
        }
    }

    /**
     * Searches and deletes generated temporary directories.
     *
     * @param root root directory
     * @throws IOException when fails to do IO operations.
     */
    public static void searchAndDeleteTempDir(String root)
            throws IOException {
        List<File> store = new LinkedList<>();
        Stack<String> stack = new Stack<>();
        stack.push(root);

        while (!stack.empty()) {
            root = stack.pop();
            File file = new File(root);
            File[] filelist = file.listFiles();
            if (filelist == null || filelist.length == 0) {
                continue;
            }
            for (File current : filelist) {
                if (current.isDirectory()) {
                    stack.push(current.toString());
                    if (current.getName().endsWith("-Temp")) {
                        store.add(current);
                    }
                }
            }
        }

        for (File dir : store) {
            FileUtils.deleteDirectory(dir);
        }
    }

    /**
     * Removes extra char from the string.
     *
     * @param valueString    string to be trimmed
     * @param removealStirng extra chars
     * @return new string
     */
    public static String trimAtLast(String valueString, String removealStirng) {
        StringBuilder stringBuilder = new StringBuilder(valueString);
        int index = valueString.lastIndexOf(removealStirng);
        stringBuilder.deleteCharAt(index);
        return stringBuilder.toString();
    }

    /**
     * Returns new parted string.
     *
     * @param partString string to be parted
     * @return parted string
     */
    public static String partString(String partString) {
        String[] strArray = partString.split(COMMA);
        String newString = EMPTY_STRING;
        for (int i = 0; i < strArray.length; i++) {
            if (i % 4 != 0 || i == 0) {
                newString = newString + strArray[i] + COMMA;
            } else {
                newString = newString + NEW_LINE + TWELVE_SPACE_INDENTATION
                        + strArray[i] + COMMA;
            }
        }
        return trimAtLast(newString, COMMA);
    }

    /**
     * Returns the directory path of the package in canonical form.
     *
     * @param baseCodeGenPath base path where the generated files needs to be
     *                        put
     * @param pathOfJavaPkg   java package of the file being generated
     * @return absolute path of the package in canonical form
     */
    public static String getDirectory(String baseCodeGenPath, String pathOfJavaPkg) {

        if (pathOfJavaPkg.charAt(pathOfJavaPkg.length() - 1) == File.separatorChar) {
            pathOfJavaPkg = trimAtLast(pathOfJavaPkg, SLASH);
        }
        String[] strArray = pathOfJavaPkg.split(SLASH);
        if (strArray[0].equals(EMPTY_STRING)) {
            return pathOfJavaPkg;
        } else {
            return baseCodeGenPath + SLASH + pathOfJavaPkg;
        }
    }

    /**
     * Returns the absolute path of the package in canonical form.
     *
     * @param baseCodeGenPath base path where the generated files needs to be
     *                        put
     * @param pathOfJavaPkg   java package of the file being generated
     * @return absolute path of the package in canonical form
     */
    public static String getAbsolutePackagePath(String baseCodeGenPath, String pathOfJavaPkg) {
        return baseCodeGenPath + pathOfJavaPkg;
    }

    /**
     * Merges the temp java files to main java files.
     *
     * @param appendFile temp file
     * @param srcFile    main file
     * @throws IOException when fails to append contents
     */
    public static void mergeJavaFiles(File appendFile, File srcFile)
            throws IOException {
        try {
            appendFileContents(appendFile, srcFile);
        } catch (IOException e) {
            throw new IOException("Failed to append " + appendFile + " in " + srcFile);
        }
    }

    /**
     * Inserts data in the generated file.
     *
     * @param file file in which need to be inserted
     * @param data data which need to be inserted
     * @throws IOException when fails to insert into file
     */
    public static void insertDataIntoJavaFile(File file, String data)
            throws IOException {
        try {
            updateFileHandle(file, data, false);
        } catch (IOException e) {
            throw new IOException("Failed to insert in " + file + "file");
        }
    }

    /**
     * Validates a line size in given file whether it is having more then 120 characters.
     * If yes it will update and give a new file.
     *
     * @param dataFile file in which need to verify all lines.
     * @return updated file
     * @throws IOException when fails to do IO operations.
     */
    public static File validateLineLength(File dataFile)
            throws IOException {
        File tempFile = dataFile;
        FileReader fileReader = new FileReader(dataFile);
        BufferedReader bufferReader = new BufferedReader(fileReader);
        try {
            StringBuilder stringBuilder = new StringBuilder();
            String line = bufferReader.readLine();

            while (line != null) {
                if (line.length() > LINE_SIZE) {
                    if (line.contains(PERIOD)) {
                        line = whenDelimiterIsPersent(line, LINE_SIZE);
                    } else if (line.contains(SPACE)) {
                        line = whenSpaceIsPresent(line, LINE_SIZE);
                    }
                    stringBuilder.append(line);
                } else {
                    stringBuilder.append(line + NEW_LINE);
                }
                line = bufferReader.readLine();
            }
            FileWriter writer = new FileWriter(tempFile);
            writer.write(stringBuilder.toString());
            writer.close();
            return tempFile;
        } finally {
            fileReader.close();
            bufferReader.close();
        }
    }

    /* When delimiters are present in the given line. */
    private static String whenDelimiterIsPersent(String line, int lineSize) {
        StringBuilder stringBuilder = new StringBuilder();

        if (line.length() > lineSize) {
            String[] strArray = line.split(Pattern.quote(PERIOD));
            stringBuilder = updateString(strArray, stringBuilder, PERIOD, lineSize);
        } else {
            stringBuilder.append(line + NEW_LINE);
        }
        String[] strArray = stringBuilder.toString().split(NEW_LINE);
        StringBuilder tempBuilder = new StringBuilder();
        for (String str : strArray) {
            if (str.length() > SUB_LINE_SIZE) {
                if (line.contains(PERIOD) && !line.contains(PERIOD + HASH + OPEN_PARENTHESIS)) {
                    String[] strArr = str.split(Pattern.quote(PERIOD));
                    tempBuilder = updateString(strArr, tempBuilder, PERIOD, SUB_LINE_SIZE);
                } else if (str.contains(SPACE)) {
                    tempBuilder.append(whenSpaceIsPresent(str, SUB_LINE_SIZE));
                }
            } else {
                tempBuilder.append(str + NEW_LINE);
            }
        }
        return tempBuilder.toString();

    }

    /* When spaces are present in the given line. */
    private static String whenSpaceIsPresent(String line, int lineSize) {
        StringBuilder stringBuilder = new StringBuilder();
        if (line.length() > lineSize) {
            String[] strArray = line.split(SPACE);
            stringBuilder = updateString(strArray, stringBuilder, SPACE, lineSize);
        } else {
            stringBuilder.append(line + NEW_LINE);
        }

        String[] strArray = stringBuilder.toString().split(NEW_LINE);
        StringBuilder tempBuilder = new StringBuilder();
        for (String str : strArray) {
            if (str.length() > SUB_LINE_SIZE) {
                if (str.contains(SPACE)) {
                    String[] strArr = str.split(SPACE);
                    tempBuilder = updateString(strArr, tempBuilder, SPACE, SUB_LINE_SIZE);
                }
            } else {
                tempBuilder.append(str + NEW_LINE);
            }
        }
        return tempBuilder.toString();
    }

    /* Updates the given line with the given size conditions. */
    private static StringBuilder updateString(String[] strArray, StringBuilder stringBuilder, String string,
            int lineSize) {

        StringBuilder tempBuilder = new StringBuilder();
        for (String str : strArray) {
            tempBuilder.append(str + string);
            if (tempBuilder.length() > lineSize) {
                String tempString = stringBuilder.toString();
                stringBuilder.delete(ZERO, stringBuilder.length());
                tempString = trimAtLast(tempString, string);
                stringBuilder.append(tempString);
                if (string.equals(PERIOD)) {
                    stringBuilder.append(NEW_LINE + TWELVE_SPACE_INDENTATION + PERIOD + str + string);
                } else {
                    stringBuilder.append(NEW_LINE + TWELVE_SPACE_INDENTATION + str + string);
                }
                tempBuilder.delete(ZERO, tempBuilder.length());
                tempBuilder.append(TWELVE_SPACE_INDENTATION);
            } else {
                stringBuilder.append(str + string);
            }
        }
        String tempString = stringBuilder.toString();
        tempString = trimAtLast(tempString, string);
        stringBuilder.delete(ZERO, stringBuilder.length());
        stringBuilder.append(tempString + NEW_LINE);
        return stringBuilder;
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
     * Returns the directory path corresponding to java package.
     *
     * @param packagePath package path
     * @return java package
     */
    public static String getPackageDirPathFromJavaJPackage(String packagePath) {
        return packagePath.replace(PERIOD, SLASH);
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
     * Restricts consecutive capital cased string as a rule in camel case.
     *
     * @param consecCapitalCaseRemover which requires the restriction of consecutive capital case
     * @return string without consecutive capital case
     */
    public static String restrictConsecutiveCapitalCase(String consecCapitalCaseRemover) {

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
     * Adds prefix, if the string begins with digit or is a java key word.
     *
     * @param camelCasePrefix string for adding prefix
     * @param conflictResolver object of YANG to java naming conflict util
     * @return prefixed camel case string
     */
    public static String addPrefix(String camelCasePrefix, YangToJavaNamingConflictUtil conflictResolver) {

        String prefix = getPrefixForIdentifier(conflictResolver);
        if (camelCasePrefix.matches(REGEX_FOR_FIRST_DIGIT)) {
            camelCasePrefix = prefix + camelCasePrefix;
        }
        if (JAVA_KEY_WORDS.contains(camelCasePrefix)) {
            camelCasePrefix = prefix + camelCasePrefix.substring(0, 1).toUpperCase()
                    + camelCasePrefix.substring(1);
        }
        return camelCasePrefix;
    }

    /**
     * Applies the rule that a string does not end with a capitalized letter and capitalizes
     * the letter next to a number in an array.
     *
     * @param stringArray containing strings for camel case separation
     * @param conflictResolver object of YANG to java naming conflict util
     * @return camel case rule checked string
     */
    public static String applyCamelCaseRule(String[] stringArray, YangToJavaNamingConflictUtil conflictResolver) {

        String ruleChecker = stringArray[0].toLowerCase();
        int i;
        if (ruleChecker.matches(REGEX_FOR_FIRST_DIGIT)) {
            i = 0;
            ruleChecker = EMPTY_STRING;
        } else {
            i = 1;
        }
        for (; i < stringArray.length; i++) {
            if (i + 1 == stringArray.length) {
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
        String ruleCheckerWithPrefix = addPrefix(ruleChecker, conflictResolver);
        return restrictConsecutiveCapitalCase(ruleCheckerWithPrefix);
    }

    /**
     * Resolves the conflict when input has upper case.
     *
     * @param stringArray containing strings for upper case conflict resolver
     * @param conflictResolver object of YANG to java naming conflict util
     * @return camel cased string
     */
    public static String upperCaseConflictResolver(String[] stringArray,
                                                   YangToJavaNamingConflictUtil conflictResolver) {

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
        return applyCamelCaseRule(stringArray, conflictResolver);
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
        return upperCaseConflictResolver(strArray, conflictResolver);
    }

    /**
     * Prefix for adding with identifier and namespace, when it is a java keyword or starting with digits.
     *
     * @param conflictResolver object of YANG to java naming conflict util
     * @return prefix which needs to be added
     */
    public static String getPrefixForIdentifier(YangToJavaNamingConflictUtil conflictResolver) {

        String prefixForIdentifier = null;
        if (conflictResolver != null) {
            prefixForIdentifier = conflictResolver.getPrefixForIdentifier();
        }
        if (prefixForIdentifier != null) {
            prefixForIdentifier = prefixForIdentifier.replaceAll(REGEX_WITH_ALL_SPECIAL_CHAR, COLAN);
            String[] strArray = prefixForIdentifier.split(COLAN);
            try {
                if (strArray[0].isEmpty()) {
                    List<String> stringArrangement = new ArrayList<String>();
                    for (int i = 1; i < strArray.length; i++) {
                        stringArrangement.add(strArray[i]);
                    }
                    strArray = stringArrangement.toArray(new String[stringArrangement.size()]);
                }
                prefixForIdentifier = strArray[0];
                for (int j = 1; j < strArray.length; j++) {
                    prefixForIdentifier = prefixForIdentifier + strArray[j].substring(0, 1).toUpperCase() +
                            strArray[j].substring(1);
                }
            } catch (ArrayIndexOutOfBoundsException outOfBoundsException) {
                throw new TranslatorException("The given prefix in pom.xml is invalid.");
            }
        } else {
            prefixForIdentifier = YANG_AUTO_PREFIX;
        }
        return prefixForIdentifier;
    }
}
