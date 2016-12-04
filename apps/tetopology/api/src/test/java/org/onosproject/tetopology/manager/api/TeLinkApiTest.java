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
import org.onosproject.tetopology.management.api.link.ElementType;
import org.onosproject.tetopology.management.api.link.Label;
import org.onosproject.tetopology.management.api.link.PathElement;
import org.onosproject.tetopology.management.api.link.TeLinkTpGlobalKey;
import org.onosproject.tetopology.management.api.link.UnderlayBackupPath;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


/**
 * Unit tests for TE link APIs.
 */
public class TeLinkApiTest {
    private static final long DEFAULT_PROVIDER_ID = 123;
    private static final long DEFAULT_CLIENT_ID = 456;
    private static final long DEFAULT_TOPOLOGY_ID = 789;
    private static final long DEFAULT_TE_NODE_ID = 1234;
    private static final long DEFAULT_TE_LINK_TP_ID = 5678;
    private static final String DEFAULT_TOPOLOGY_ID_STRING =
            "default-topology-123";
    private static final long DEFAULT_PATH_ELEMENT_ID = 234;
    private static final long DEFAULT_UNDERLAY_BACKUP_PATH_IDX = 10;

    private long providerId;
    private long clientId;
    private long topologyId;
    private long teNodeId;
    private long teLinkTpId;
    private long pathElementId;
    private long underlayBackupPathIndex;

    private String topologyIdString;

    @Before
    public void setUp() {
        providerId = DEFAULT_PROVIDER_ID;
        clientId = DEFAULT_CLIENT_ID;
        topologyId = DEFAULT_TOPOLOGY_ID;
        teNodeId = DEFAULT_TE_NODE_ID;
        teLinkTpId = DEFAULT_TE_LINK_TP_ID;
        topologyIdString = DEFAULT_TOPOLOGY_ID_STRING;
        pathElementId = DEFAULT_PATH_ELEMENT_ID;
        underlayBackupPathIndex = DEFAULT_UNDERLAY_BACKUP_PATH_IDX;
    }

    @Test
    public void teLinkTpGlobalKeyEqualOperatorTest() {
        TeLinkTpGlobalKey key1 = new TeLinkTpGlobalKey(providerId, clientId,
                                                       topologyId, teNodeId,
                                                       teLinkTpId);
        TeLinkTpGlobalKey key2 = new TeLinkTpGlobalKey(providerId, clientId,
                                                       topologyId, teNodeId,
                                                       teLinkTpId);
        TeLinkTpGlobalKey key3 = new TeLinkTpGlobalKey(providerId + 1, clientId,
                                                       topologyId, teNodeId,
                                                       teLinkTpId);
        TeLinkTpGlobalKey key4 = new TeLinkTpGlobalKey(providerId, clientId + 1,
                                                       topologyId, teNodeId,
                                                       teLinkTpId);
        TeLinkTpGlobalKey key5 = new TeLinkTpGlobalKey(providerId, clientId,
                                                       topologyId + 1,
                                                       teNodeId, teLinkTpId);
        TeLinkTpGlobalKey key6 = new TeLinkTpGlobalKey(providerId, clientId,
                                                       topologyId,
                                                       teNodeId + 1, teLinkTpId);
        TeLinkTpGlobalKey key7 = new TeLinkTpGlobalKey(providerId, clientId,
                                                       topologyId,
                                                       teNodeId, teLinkTpId + 1);

        assertTrue("Two topology Ids must be equal", key1.equals(key2));

        assertFalse("Two topology Ids must be unequal", key1.equals(key3));
        assertFalse("Two topology Ids must be unequal", key3.equals(key1));

        assertFalse("Two topology Ids must be unequal", key1.equals(key4));
        assertFalse("Two topology Ids must be unequal", key4.equals(key1));

        assertFalse("Two topology Ids must be unequal", key1.equals(key5));
        assertFalse("Two topology Ids must be unequal", key5.equals(key1));

        assertFalse("Two topology Ids must be unequal", key1.equals(key6));
        assertFalse("Two topology Ids must be unequal", key6.equals(key1));

        assertFalse("Two topology Ids must be unequal", key1.equals(key7));
        assertFalse("Two topology Ids must be unequal", key7.equals(key1));
    }

    @Test
    public void underLayBackupPathEqualOperatorTest() {
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
        pathElementList1.add(pathElement1);
        pathElementList1.add(pathElement2);
        pathElementList1.add(pathElement4);

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
}
