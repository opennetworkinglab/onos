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

import java.util.ArrayList;
import java.util.List;

import org.onosproject.yangutils.datamodel.YangNodeIdentifier;

import static org.onosproject.yangutils.translator.tojava.utils.JavaIdentifierSyntax.getCaptialCase;

/**
 * Represents a utility which provides valid name for generated java file for augment node.
 */
public final class AugmentJavaFileNameGenUtil {

    /**
     * Prefix to be added to generated java file for augment node.
     */
    private static final String AUGMENTED = "Augmented";

    /**
     * The number of time augment has updated the same target node in same module/submodule.
     */
    private static int occurrenceCount = 1;

    /**
     * List of names for generated augment java file.
     */
    private static List<String> augmentJavaFileNameList = new ArrayList<>();

    private static final int ONE = 1;
    private static final int TWO = 2;
    private static final int ZERO = 0;

    /**
     * Creates an instance of augment java file name generator utility.
     */
    private AugmentJavaFileNameGenUtil() {
    }

    /**
     * Sets the augment java file name list.
     *
     * @param nameList name list
     */
    private static void setAugmentJavaFileNameList(List<String> nameList) {
        augmentJavaFileNameList = nameList;
    }

    /**
     * Returns augment java file name list.
     *
     * @return augment java file name list
     */
    public static List<String> getAugmentJavaFileNameList() {
        return augmentJavaFileNameList;
    }

    /**
     * Sets occurrence count.
     *
     * @param occurrence occurrence count
     */
    private static void setOccurrenceCount(int occurrence) {
        occurrenceCount = occurrence;
    }

    /**
     * Returns occurrence count.
     *
     * @return occurrence count
     */
    private static int getOccurrenceCount() {
        return occurrenceCount;
    }

    /**
     * Creates a name identifier for augment.
     *
     * @param nodeId node identifier
     * @param isPrefix if prefix is present or it is not equals to parent's prefix
     * @return valid name for augment
     */
    public static String createValidNameForAugment(YangNodeIdentifier nodeId, boolean isPrefix) {
        getAugmentJavaFileNameList().add(createName(nodeId, isPrefix));
        setAugmentJavaFileNameList(getAugmentJavaFileNameList());
        return getAugmentJavaFileNameList().get(getAugmentJavaFileNameList().size() - 1);
    }

    /**
     * Creates name for the current augment file.
     *
     * @param nodeId node identifier
     * @param isPrefix if prefix is present or it is not equals to parent's prefix
     */
    private static String createName(YangNodeIdentifier nodeId, boolean isPrefix) {
        if (isPrefix) {
            return AUGMENTED + getCaptialCase(nodeId.getPrefix()) + getCaptialCase(nodeId.getName());
        } else {
            return AUGMENTED + getCaptialCase(nodeId.getName());
        }
    }

    /**
     * Updates occurrence count of augment.
     */
    public static void updateOccurenceCount() {
        int count = getOccurrenceCount();
        count++;
        setOccurrenceCount(count);
    }

    /**
     * Updates the list of name when augment has occurred multiple times to update the same target node
     * and returns a valid name for augment node's generated java file.
     *
     * @param nodeId YANG node identifier
     * @param isPrefix true if a prefix is present and it is not equals to parents prefix
     * @return valid name for augment node
     */
    public static String updateNameWhenHasMultipleOuccrrence(YangNodeIdentifier nodeId, boolean isPrefix) {
        String name = "";
        updateOccurenceCount();

        if (getOccurrenceCount() == TWO) {
            String previousAugmentsName = getAugmentJavaFileNameList().get(getAugmentJavaFileNameList().size() - ONE);
            getAugmentJavaFileNameList().remove(ZERO);
            getAugmentJavaFileNameList().add(previousAugmentsName + ONE);
            //TODO: update when already contains the name.
            name = createName(nodeId, isPrefix) + TWO;
        } else {
            name = createName(nodeId, isPrefix) + getOccurrenceCount();
        }
        getAugmentJavaFileNameList().add(name);
        return name;
    }

    /**
     * Resets occurrence count to one.
     */
    public static void clearOccurrenceCount() {
        setOccurrenceCount(ONE);
    }

}
