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
package org.onosproject.tetopology.manager.api;

import org.junit.Before;
import org.junit.Test;
import org.onosproject.tetopology.management.api.link.ConnectivityMatrixId;
import org.onosproject.tetopology.management.api.link.ElementType;
import org.onosproject.tetopology.management.api.link.Label;
import org.onosproject.tetopology.management.api.link.PathElement;
import org.onosproject.tetopology.management.api.link.TePathAttributes;
import org.onosproject.tetopology.management.api.link.UnderlayAbstractPath;
import org.onosproject.tetopology.management.api.link.UnderlayBackupPath;
import org.onosproject.tetopology.management.api.node.ConnectivityMatrix;
import org.onosproject.tetopology.management.api.node.ConnectivityMatrixKey;
import org.onosproject.tetopology.management.api.node.TeNodeKey;
import org.onosproject.tetopology.management.api.node.TtpKey;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


/**
 * Unit tests for TE node APIs.
 */
public class TeNodeApiTest {
    private static final long DEFAULT_PROVIDER_ID = 123;
    private static final long DEFAULT_CLIENT_ID = 456;
    private static final long DEFAULT_TOPOLOGY_ID = 789;
    private static final long DEFAULT_TE_NODE_ID = 1234;
    private static final long DEFAULT_CONNECTIVITY_ENTRY_ID = 5678;
    private static final long DEFAULT_TTP_ID = 897;
    private static final String DEFAULT_TOPOLOGY_ID_STRING =
            "default-topology-123";
    private static final long DEFAULT_PATH_ELEMENT_ID = 234;
    private static final long DEFAULT_UNDERLAY_BACKUP_PATH_IDX = 10;

    private long providerId;
    private long clientId;
    private long topologyId;
    private long teNodeId;
    private long connectivityMatrixEntryId;
    private long pathElementId;
    private long underlayBackupPathIndex;
    private long ttpId;

    private String topologyIdString;

    @Before
    public void setUp() {
        providerId = DEFAULT_PROVIDER_ID;
        clientId = DEFAULT_CLIENT_ID;
        topologyId = DEFAULT_TOPOLOGY_ID;
        teNodeId = DEFAULT_TE_NODE_ID;
        connectivityMatrixEntryId = DEFAULT_CONNECTIVITY_ENTRY_ID;
        topologyIdString = DEFAULT_TOPOLOGY_ID_STRING;
        pathElementId = DEFAULT_PATH_ELEMENT_ID;
        underlayBackupPathIndex = DEFAULT_UNDERLAY_BACKUP_PATH_IDX;
        ttpId = DEFAULT_TTP_ID;
    }

    @Test
    public void connectivityMatrixKeyEqualOperatorTest() {
        ConnectivityMatrixKey key1 = new ConnectivityMatrixKey(providerId,
                                                               clientId,
                                                               topologyId,
                                                               teNodeId,
                                                               connectivityMatrixEntryId);
        ConnectivityMatrixKey key2 = new ConnectivityMatrixKey(providerId,
                                                               clientId,
                                                               topologyId,
                                                               teNodeId,
                                                               connectivityMatrixEntryId);
        ConnectivityMatrixKey key3 = new ConnectivityMatrixKey(providerId + 1,
                                                               clientId,
                                                               topologyId,
                                                               teNodeId,
                                                               connectivityMatrixEntryId);
        ConnectivityMatrixKey key4 = new ConnectivityMatrixKey(providerId,
                                                               clientId + 1,
                                                               topologyId,
                                                               teNodeId,
                                                               connectivityMatrixEntryId);
        ConnectivityMatrixKey key5 = new ConnectivityMatrixKey(providerId,
                                                               clientId,
                                                               topologyId + 1,
                                                               teNodeId,
                                                               connectivityMatrixEntryId);
        ConnectivityMatrixKey key6 = new ConnectivityMatrixKey(providerId,
                                                               clientId,
                                                               topologyId,
                                                               teNodeId + 1,
                                                               connectivityMatrixEntryId);
        ConnectivityMatrixKey key7 = new ConnectivityMatrixKey(providerId,
                                                               clientId,
                                                               topologyId,
                                                               teNodeId,
                                                               connectivityMatrixEntryId + 1);

        assertTrue("Two matrix keys must be equal", key1.equals(key2));

        assertFalse("Two matrix keys must be unequal", key1.equals(key3));
        assertFalse("Two matrix keys must be unequal", key3.equals(key1));

        assertFalse("Two matrix keys must be unequal", key1.equals(key4));
        assertFalse("Two matrix keys must be unequal", key4.equals(key1));

        assertFalse("Two matrix keys must be unequal", key1.equals(key5));
        assertFalse("Two matrix keys must be unequal", key5.equals(key1));

        assertFalse("Two matrix keys must be unequal", key1.equals(key6));
        assertFalse("Two matrix keys must be unequal", key6.equals(key1));

        assertFalse("Two matrix keys must be unequal", key1.equals(key7));
        assertFalse("Two matrix keys must be unequal", key7.equals(key1));
    }

    @Test
    public void underlayBackupPathEqualOperatorTest() {
        ElementType pathElementType1 = new Label(pathElementId + 1);
        ElementType pathElementType2 = new Label(pathElementId + 2);
        ElementType pathElementType3 = new Label(pathElementId + 3);
        ElementType pathElementType4 = new Label(pathElementId + 4);

        PathElement pathElement1 = new PathElement(pathElementId, teNodeId,
                                                   pathElementType1, true);
        PathElement pathElement2 = new PathElement(pathElementId + 1,
                                                   teNodeId + 1,
                                                   pathElementType2, true);
        PathElement pathElement3 = new PathElement(pathElementId + 2,
                                                   teNodeId + 2,
                                                   pathElementType3, true);
        PathElement pathElement4 = new PathElement(pathElementId + 3,
                                                   teNodeId + 3,
                                                   pathElementType4, true);

        List<PathElement> pathElementList1 = new ArrayList<>();
        pathElementList1.add(pathElement1);
        pathElementList1.add(pathElement2);
        pathElementList1.add(pathElement3);

        List<PathElement> pathElementList2 = new ArrayList<>();
        pathElementList2.add(pathElement1);
        pathElementList2.add(pathElement2);
        pathElementList2.add(pathElement4);

        // bp1 and bp2 are the same. bp3, bp4, and bp5 differ by one
        // attribute comparing to bp1.
        UnderlayBackupPath bp1 = new UnderlayBackupPath(
                underlayBackupPathIndex, pathElementList1, true);
        UnderlayBackupPath bp2 = new UnderlayBackupPath(
                underlayBackupPathIndex, pathElementList1, true);

        UnderlayBackupPath bp3 = new UnderlayBackupPath(
                underlayBackupPathIndex + 1, pathElementList1, true);
        UnderlayBackupPath bp4 = new UnderlayBackupPath(
                underlayBackupPathIndex, pathElementList2, true);
        UnderlayBackupPath bp5 = new UnderlayBackupPath(
                underlayBackupPathIndex, pathElementList1, false);


        assertTrue("Two backup paths must be equal", bp1.equals(bp2));

        assertFalse("Two backup paths must be unequal", bp1.equals(bp3));
        assertFalse("Two backup paths must be unequal", bp3.equals(bp1));

        assertFalse("Two backup paths must be unequal", bp1.equals(bp4));
        assertFalse("Two backup paths must be unequal", bp4.equals(bp1));

        assertFalse("Two backup paths must be unequal", bp1.equals(bp5));
        assertFalse("Two backup paths must be unequal", bp5.equals(bp1));
    }


    @Test
    public void connectivityMatrixEqualOperatorTest() {
        long key1 = connectivityMatrixEntryId;
        long key2 = connectivityMatrixEntryId + 1;

        ElementType pathElementType1 = new Label(pathElementId + 1);
        ElementType pathElementType2 = new Label(pathElementId + 2);
        ElementType pathElementType3 = new Label(pathElementId + 3);
        ElementType pathElementType4 = new Label(pathElementId + 4);

        PathElement pathElement1 = new PathElement(pathElementId, teNodeId,
                                                   pathElementType1, true);
        PathElement pathElement2 = new PathElement(pathElementId + 1,
                                                   teNodeId + 1,
                                                   pathElementType2, true);
        PathElement pathElement3 = new PathElement(pathElementId + 2,
                                                   teNodeId + 2,
                                                   pathElementType3, true);
        PathElement pathElement4 = new PathElement(pathElementId + 3,
                                                   teNodeId + 3,
                                                   pathElementType4, true);

        List<PathElement> pathElementList1 = new ArrayList<>();
        pathElementList1.add(pathElement1);
        pathElementList1.add(pathElement2);
        pathElementList1.add(pathElement3);

        List<PathElement> pathElementList2 = new ArrayList<>();
        pathElementList2.add(pathElement1);
        pathElementList2.add(pathElement2);
        pathElementList2.add(pathElement4);

        UnderlayAbstractPath abstractPath1 = new UnderlayAbstractPath(
                pathElementList1, true);
        UnderlayAbstractPath abstractPath2 = new UnderlayAbstractPath(
                pathElementList2, true);

        ElementType from = new ConnectivityMatrixId(connectivityMatrixEntryId);
        List<ElementType> mergingList = new ArrayList<>();
        mergingList.add(new ConnectivityMatrixId(connectivityMatrixEntryId + 1));
        mergingList.add(new ConnectivityMatrixId(connectivityMatrixEntryId + 2));

        List<ElementType> constrainList = new ArrayList<>();
        constrainList.add(new ConnectivityMatrixId(connectivityMatrixEntryId + 3));
        constrainList.add(new ConnectivityMatrixId(connectivityMatrixEntryId + 4));

        BitSet flags = new BitSet(1);

        List<Long> srlgs = new ArrayList<>();
        srlgs.add(new Long(10));
        TePathAttributes tePathAttributes = new TePathAttributes(new Long(10),
                                                                 new Long(10),
                                                                 srlgs);

        ConnectivityMatrix matrix1 = new ConnectivityMatrix(key1,
                                                            from,
                                                            mergingList,
                                                            constrainList,
                                                            flags,
                                                            tePathAttributes,
                                                            abstractPath1);
        ConnectivityMatrix matrix2 = new ConnectivityMatrix(key1,
                                                            from,
                                                            mergingList,
                                                            constrainList,
                                                            flags,
                                                            tePathAttributes,
                                                            abstractPath1);
        ConnectivityMatrix matrix3 = new ConnectivityMatrix(key1,
                                                            from,
                                                            mergingList,
                                                            constrainList,
                                                            flags,
                                                            tePathAttributes,
                                                            abstractPath2);
        ConnectivityMatrix matrix4 = new ConnectivityMatrix(key2,
                                                            from,
                                                            mergingList,
                                                            constrainList,
                                                            flags,
                                                            tePathAttributes,
                                                            abstractPath1);

        assertTrue("Two conn matrices must be equal", matrix1.equals(matrix2));

        assertFalse("Two conn matrices must be unequal", matrix1.equals(matrix3));
        assertFalse("Two conn matrices must be unequal", matrix3.equals(matrix1));

        assertFalse("Two conn matrices must be unequal", matrix1.equals(matrix4));
        assertFalse("Two conn matrices must be unequal", matrix4.equals(matrix1));
    }

    @Test
    public void teNodeKeyEqualOperatorTest() {
        TeNodeKey key1 = new TeNodeKey(providerId, clientId,
                                       topologyId, teNodeId);
        TeNodeKey key2 = new TeNodeKey(providerId, clientId,
                                       topologyId, teNodeId);
        TeNodeKey key3 = new TeNodeKey(providerId + 1, clientId,
                                       topologyId, teNodeId);
        TeNodeKey key4 = new TeNodeKey(providerId, clientId + 1,
                                       topologyId, teNodeId);
        TeNodeKey key5 = new TeNodeKey(providerId, clientId,
                                       topologyId + 1, teNodeId);
        TeNodeKey key6 = new TeNodeKey(providerId, clientId,
                                       topologyId, teNodeId + 1);

        assertTrue("Two matrix keys must be equal", key1.equals(key2));

        assertFalse("Two matrix keys must be unequal", key1.equals(key3));
        assertFalse("Two matrix keys must be unequal", key3.equals(key1));

        assertFalse("Two matrix keys must be unequal", key1.equals(key4));
        assertFalse("Two matrix keys must be unequal", key4.equals(key1));

        assertFalse("Two matrix keys must be unequal", key1.equals(key5));
        assertFalse("Two matrix keys must be unequal", key5.equals(key1));

        assertFalse("Two matrix keys must be unequal", key1.equals(key6));
        assertFalse("Two matrix keys must be unequal", key6.equals(key1));
    }

    @Test
    public void ttpMatrixKeyEqualOperatorTest() {
        TtpKey key1 = new TtpKey(providerId, clientId, topologyId,
                                 teNodeId, ttpId);
        TtpKey key2 = new TtpKey(providerId, clientId, topologyId,
                                 teNodeId, ttpId);
        TtpKey key3 = new TtpKey(providerId + 1, clientId, topologyId,
                                 teNodeId, ttpId);
        TtpKey key4 = new TtpKey(providerId, clientId + 1, topologyId,
                                 teNodeId, ttpId);
        TtpKey key5 = new TtpKey(providerId, clientId, topologyId + 1,
                                 teNodeId, ttpId);
        TtpKey key6 = new TtpKey(providerId, clientId, topologyId,
                                 teNodeId + 1, ttpId);
        TtpKey key7 = new TtpKey(providerId, clientId, topologyId,
                                 teNodeId, ttpId + 1);

        assertTrue("Two TTP keys must be equal", key1.equals(key2));

        assertFalse("Two TTP keys must be unequal", key1.equals(key3));
        assertFalse("Two TTP keys must be unequal", key3.equals(key1));

        assertFalse("Two TTP keys must be unequal", key1.equals(key4));
        assertFalse("Two TTP keys must be unequal", key4.equals(key1));

        assertFalse("Two TTP keys must be unequal", key1.equals(key5));
        assertFalse("Two TTP keys must be unequal", key5.equals(key1));

        assertFalse("Two TTP keys must be unequal", key1.equals(key6));
        assertFalse("Two TTP keys must be unequal", key6.equals(key1));

        assertFalse("Two TTP keys must be unequal", key1.equals(key7));
        assertFalse("Two TTP keys must be unequal", key7.equals(key1));
    }

}
