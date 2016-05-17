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

package org.onosproject.yangutils.parser.impl.parseutils;

import org.junit.Test;
import org.onosproject.yangutils.datamodel.YangNodeIdentifier;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.onosproject.yangutils.parser.impl.parserutils.AugmentListenerUtil.clearOccurrenceCount;
import static org.onosproject.yangutils.parser.impl.parserutils.AugmentListenerUtil.createValidNameForAugment;
import static org.onosproject.yangutils.parser.impl.parserutils.AugmentListenerUtil.getAugmentJavaFileNameList;
import static org.onosproject.yangutils.parser.impl.parserutils.AugmentListenerUtil.updateNameWhenHasMultipleOuccrrence;

/**
 * Unit test case for augment listener utility.
 */
public class AugmentListnerUtilTest {

    private static final String TEST1 = "test1Node";
    private static final String PARENT_PREFIX = "if";
    private static final String NODE_PREFIX = "rf";

    private static final String TEST1_AUGMENTED_NAME_WITHOUT_PREFIX = "AugmentedTest1Node";
    private static final String TEST1_AUGMENTED_NAME_WITHOUT_PREFIX_MULTI1 = "AugmentedTest1Node1";
    private static final String TEST1_AUGMENTED_NAME_WITHOUT_PREFIX_MULTI2 = "AugmentedTest1Node2";
    private static final String TEST1_AUGMENTED_NAME_WITHOUT_PREFIX_MULTI3 = "AugmentedTest1Node3";

    private static final String TEST1_AUGMENTED_NAME_WITH_PREFIX = "AugmentedRfTest1Node";
    private static final String TEST1_AUGMENTED_NAME_WITH_PREFIX_MULTI1 = "AugmentedRfTest1Node1";
    private static final String TEST1_AUGMENTED_NAME_WITH_PREFIX_MULTI2 = "AugmentedRfTest1Node2";
    private static final String TEST1_AUGMENTED_NAME_WITH_PREFIX_MULTI3 = "AugmentedRfTest1Node3";

    private static String testString = "";

    /**
     * Unit test case when parent's prefix is present and one occurrence of augment node to update same target node.
     */
    @Test
    public void testForAugmentNameWhenOneOuccrrenceWithParentPrefix() {
        clearData();
        testString = createValidNameForAugment(getStubNodeIdetifierWithParentPrefix(),
                isPrefixPresent(getStubNodeIdetifierWithParentPrefix()));
        assertThat(true, is(testString.equals(TEST1_AUGMENTED_NAME_WITHOUT_PREFIX)));
    }

    /**
     * Unit test case when no prefix and one occurrence of augment node to update same target node.
     */
    @Test
    public void testForAugmentNameWhenOneOuccrrenceWithNoPrefix() {
        clearData();
        testString = createValidNameForAugment(getStubNodeIdetifierWithNoPrefix(),
                isPrefixPresent(getStubNodeIdetifierWithNoPrefix()));
        assertThat(true, is(testString.equals(TEST1_AUGMENTED_NAME_WITHOUT_PREFIX)));
    }

    /**
     * Unit test case when different prefix then parent is present and
     * one occurrence of augment node to update same target node.
     */
    @Test
    public void testForAugmentNameWhenOneOuccrrenceWithDiffPrefix() {
        clearData();
        testString = createValidNameForAugment(getStubNodeIdetifierWithDiffPrefix(),
                isPrefixPresent(getStubNodeIdetifierWithDiffPrefix()));
        assertThat(true, is(testString.equals(TEST1_AUGMENTED_NAME_WITH_PREFIX)));
    }

    /**
     * Unit test case when parent's prefix is present and two occurrence of augment node to update
     * same target node is present.
     */
    @Test
    public void testForAugmentNameWhenTwoOuccrrenceWithParentPrefix() {
        clearData();

        createValidNameForAugment(getStubNodeIdetifierWithParentPrefix(),
                isPrefixPresent(getStubNodeIdetifierWithParentPrefix()));
        testString = updateNameWhenHasMultipleOuccrrence(getStubNodeIdetifierWithParentPrefix(),
                isPrefixPresent(getStubNodeIdetifierWithParentPrefix()));

        assertThat(true, is(testString.equals(TEST1_AUGMENTED_NAME_WITHOUT_PREFIX_MULTI2)));
    }

    /**
     * Unit test case when no prefix and two occurrence of augment node to update
     * same target node is present.
     */
    @Test
    public void testForAugmentNameWhenTwoOuccrrenceWithNoPrefix() {
        clearData();

        createValidNameForAugment(getStubNodeIdetifierWithNoPrefix(),
                isPrefixPresent(getStubNodeIdetifierWithNoPrefix()));
        testString = updateNameWhenHasMultipleOuccrrence(getStubNodeIdetifierWithNoPrefix(),
                isPrefixPresent(getStubNodeIdetifierWithNoPrefix()));
        assertThat(true, is(testString.equals(TEST1_AUGMENTED_NAME_WITHOUT_PREFIX_MULTI2)));
    }

    /**
     * Unit test case when different prefix then parent is present and
     * two occurrence of augment node to update same target node is present.
     */
    @Test
    public void testForAugmentNameWhenTwoOuccrrenceWithDiffPrefix() {
        clearData();

        createValidNameForAugment(getStubNodeIdetifierWithDiffPrefix(),
                isPrefixPresent(getStubNodeIdetifierWithDiffPrefix()));
        testString = updateNameWhenHasMultipleOuccrrence(getStubNodeIdetifierWithDiffPrefix(),
                isPrefixPresent(getStubNodeIdetifierWithDiffPrefix()));
        assertThat(true, is(testString.equals(TEST1_AUGMENTED_NAME_WITH_PREFIX_MULTI2)));
    }

    /**
     * Unit test case when parent prefix and three occurrence of augment node to update
     * same target node is present.
     */
    @Test
    public void testForAugmentNameWhenThreeOuccrrenceWithParentPrefix() {
        clearData();

        createValidNameForAugment(getStubNodeIdetifierWithParentPrefix(),
                isPrefixPresent(getStubNodeIdetifierWithParentPrefix()));
        updateNameWhenHasMultipleOuccrrence(getStubNodeIdetifierWithParentPrefix(),
                isPrefixPresent(getStubNodeIdetifierWithParentPrefix()));

        testString = updateNameWhenHasMultipleOuccrrence(getStubNodeIdetifierWithParentPrefix(),
                isPrefixPresent(getStubNodeIdetifierWithParentPrefix()));
        assertThat(true, is(testString.equals(TEST1_AUGMENTED_NAME_WITHOUT_PREFIX_MULTI3)));
    }

    /**
     * Unit test case when no prefix and three occurrence of augment node to update
     * same target node is present.
     */
    @Test
    public void testForAugmentNameWhenThreeOuccrrenceNoPrefix() {
        clearData();

        createValidNameForAugment(getStubNodeIdetifierWithNoPrefix(),
                isPrefixPresent(getStubNodeIdetifierWithNoPrefix()));
        updateNameWhenHasMultipleOuccrrence(getStubNodeIdetifierWithNoPrefix(),
                isPrefixPresent(getStubNodeIdetifierWithNoPrefix()));

        testString = updateNameWhenHasMultipleOuccrrence(getStubNodeIdetifierWithNoPrefix(),
                isPrefixPresent(getStubNodeIdetifierWithNoPrefix()));
        assertThat(true, is(testString.equals(TEST1_AUGMENTED_NAME_WITHOUT_PREFIX_MULTI3)));
    }

    /**
     * Unit test case when different prefix and three occurrence of augment node to update
     * same target node is present.
     */
    @Test
    public void testForAugmentNameWhenThreeOuccrrenceWithDiffPrefix() {
        clearData();

        createValidNameForAugment(getStubNodeIdetifierWithDiffPrefix(),
                isPrefixPresent(getStubNodeIdetifierWithDiffPrefix()));
        updateNameWhenHasMultipleOuccrrence(getStubNodeIdetifierWithDiffPrefix(),
                isPrefixPresent(getStubNodeIdetifierWithDiffPrefix()));

        testString = updateNameWhenHasMultipleOuccrrence(getStubNodeIdetifierWithDiffPrefix(),
                isPrefixPresent(getStubNodeIdetifierWithDiffPrefix()));
        assertThat(true, is(testString.equals(TEST1_AUGMENTED_NAME_WITH_PREFIX_MULTI3)));
    }

    /**
     * Unit test case for when three occurrence is there and parent prefix is present,
     * all the names need to be updated in list.
     */
    @Test
    public void testForPreviousNamesGotUpdatedWhenParentPrefix() {
        clearData();

        createValidNameForAugment(getStubNodeIdetifierWithParentPrefix(),
                isPrefixPresent(getStubNodeIdetifierWithParentPrefix()));
        updateNameWhenHasMultipleOuccrrence(getStubNodeIdetifierWithParentPrefix(),
                isPrefixPresent(getStubNodeIdetifierWithParentPrefix()));
        updateNameWhenHasMultipleOuccrrence(getStubNodeIdetifierWithParentPrefix(),
                isPrefixPresent(getStubNodeIdetifierWithParentPrefix()));

        testString = getAugmentJavaFileNameList().get(0);
        assertThat(true, is(testString.equals(TEST1_AUGMENTED_NAME_WITHOUT_PREFIX_MULTI1)));

        testString = getAugmentJavaFileNameList().get(1);
        assertThat(true, is(testString.equals(TEST1_AUGMENTED_NAME_WITHOUT_PREFIX_MULTI2)));

        testString = getAugmentJavaFileNameList().get(2);
        assertThat(true, is(testString.equals(TEST1_AUGMENTED_NAME_WITHOUT_PREFIX_MULTI3)));
    }

    /**
     * Unit test case for when three occurrence is there and no prefix is present,
     * all the names need to be updated in list.
     */
    @Test
    public void testForPreviousNamesGotUpdatedWhenNoPrefix() {
        clearData();

        createValidNameForAugment(getStubNodeIdetifierWithNoPrefix(),
                isPrefixPresent(getStubNodeIdetifierWithNoPrefix()));
        updateNameWhenHasMultipleOuccrrence(getStubNodeIdetifierWithNoPrefix(),
                isPrefixPresent(getStubNodeIdetifierWithNoPrefix()));
        updateNameWhenHasMultipleOuccrrence(getStubNodeIdetifierWithNoPrefix(),
                isPrefixPresent(getStubNodeIdetifierWithNoPrefix()));

        testString = getAugmentJavaFileNameList().get(0);
        assertThat(true, is(testString.equals(TEST1_AUGMENTED_NAME_WITHOUT_PREFIX_MULTI1)));

        testString = getAugmentJavaFileNameList().get(1);
        assertThat(true, is(testString.equals(TEST1_AUGMENTED_NAME_WITHOUT_PREFIX_MULTI2)));

        testString = getAugmentJavaFileNameList().get(2);
        assertThat(true, is(testString.equals(TEST1_AUGMENTED_NAME_WITHOUT_PREFIX_MULTI3)));
    }

    /**
     * Unit test case for when three occurrence is there and different prefix is present,
     * all the names need to be updated in list.
     */
    @Test
    public void testForPreviousNamesGotUpdatedWhenDifferentPrefix() {
        clearData();

        createValidNameForAugment(getStubNodeIdetifierWithDiffPrefix(),
                isPrefixPresent(getStubNodeIdetifierWithDiffPrefix()));
        updateNameWhenHasMultipleOuccrrence(getStubNodeIdetifierWithDiffPrefix(),
                isPrefixPresent(getStubNodeIdetifierWithDiffPrefix()));
        updateNameWhenHasMultipleOuccrrence(getStubNodeIdetifierWithDiffPrefix(),
                isPrefixPresent(getStubNodeIdetifierWithDiffPrefix()));

        testString = getAugmentJavaFileNameList().get(0);
        assertThat(true, is(testString.equals(TEST1_AUGMENTED_NAME_WITH_PREFIX_MULTI1)));

        testString = getAugmentJavaFileNameList().get(1);
        assertThat(true, is(testString.equals(TEST1_AUGMENTED_NAME_WITH_PREFIX_MULTI2)));

        testString = getAugmentJavaFileNameList().get(2);
        assertThat(true, is(testString.equals(TEST1_AUGMENTED_NAME_WITH_PREFIX_MULTI3)));
    }

    /**
     * Returns stub node identifier when parent prefix is used.
     *
     * @param name name of node
     * @param prefix prefix of node
     * @return node identifier for node
     */
    private YangNodeIdentifier getStubNodeIdetifierWithParentPrefix() {
        YangNodeIdentifier nodeId = new YangNodeIdentifier();
        nodeId.setName(TEST1);
        nodeId.setPrefix(PARENT_PREFIX);
        return nodeId;
    }

    /**
     * Returns stub node identifier when no prefix is used.
     *
     * @param name name of node
     * @param prefix prefix of node
     * @return node identifier for node
     */
    private YangNodeIdentifier getStubNodeIdetifierWithNoPrefix() {
        YangNodeIdentifier nodeId = new YangNodeIdentifier();
        nodeId.setName(TEST1);
        nodeId.setPrefix(null);
        return nodeId;
    }

    /**
     * Returns stub node identifier when different prefix is used.
     *
     * @param name name of node
     * @param prefix prefix of node
     * @return node identifier for node
     */
    private YangNodeIdentifier getStubNodeIdetifierWithDiffPrefix() {
        YangNodeIdentifier nodeId = new YangNodeIdentifier();
        nodeId.setName(TEST1);
        nodeId.setPrefix(NODE_PREFIX);
        return nodeId;
    }

    /**
     * Returns true if a prefix is present and it is not equals to parents prefix.
     *
     * @param nodeId YANG node identifier
     * @param parentsPrefix parent's prefix
     * @return true if a prefix is present and it is not equals to parents prefix
     */
    private static boolean isPrefixPresent(YangNodeIdentifier nodeId) {
        return nodeId.getPrefix() != null && nodeId.getPrefix() != PARENT_PREFIX;
    }

    /**
     * Clears list of names and occurrence count after each test case.
     */
    private void clearData() {
        getAugmentJavaFileNameList().clear();
        clearOccurrenceCount();
    }

}
