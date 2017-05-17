/*
 * Copyright 2015-present Open Networking Laboratory
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
package org.onosproject.store.intent.impl;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onosproject.cfg.ComponentConfigService;
import org.onosproject.cfg.ConfigProperty;
import org.onosproject.cluster.ClusterServiceAdapter;
import org.onosproject.cluster.NodeId;
import org.onosproject.net.intent.AbstractIntentTest;
import org.onosproject.net.intent.HostToHostIntent;
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.IntentData;
import org.onosproject.net.intent.IntentState;
import org.onosproject.net.intent.IntentTestsMocks;
import org.onosproject.net.intent.WorkPartitionServiceAdapter;
import org.onosproject.store.service.TestStorageService;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.onosproject.net.NetTestTools.APP_ID;
import static org.onosproject.net.NetTestTools.hid;

/**
 * Gossip Intent Store test using database adapter.
 */
public class GossipIntentStoreTest extends AbstractIntentTest {

    private GossipIntentStore intentStore;
    private HostToHostIntent.Builder builder1;

    @Override
    @Before
    public void setUp() {
        intentStore = new GossipIntentStore();
        intentStore.storageService = new TestStorageService();
        intentStore.partitionService = new WorkPartitionServiceAdapter();
        intentStore.clusterService = new ClusterServiceAdapter();
        super.setUp();
        builder1 = HostToHostIntent
                        .builder()
                        .one(hid("12:34:56:78:91:ab/1"))
                        .two(hid("12:34:56:78:91:ac/1"))
                        .appId(APP_ID);
        intentStore.configService = new MockComponentConfigService();
        intentStore.activate(null);
    }

    @Override
    @After
    public void tearDown() {
        intentStore.deactivate();
        super.tearDown();
    }

    /**
     * Generates a list of test intent data.
     *
     * @param count how many intent data objects are needed
     * @return list of intent data
     */
    private List<IntentData> generateIntentList(int count) {
        LinkedList<IntentData> intents = new LinkedList<>();
        IntStream.rangeClosed(1, count)
                .forEach(i ->
                        intents.add(
                                new IntentData(
                                        builder1
                                                .priority(i)
                                                .build(),
                                        IntentState.INSTALLED,
                                        new IntentTestsMocks.MockTimestamp(12))));
        return intents;
    }

    /**
     * Tests the intent count APIs.
     */
    @Test
    public void testGetIntentCount() {
        assertThat(intentStore.getIntentCount(), is(0L));

        generateIntentList(5).forEach(intentStore::write);

        assertThat(intentStore.getIntentCount(), is(5L));
    }

    /**
     * Tests the batch add API.
     */
    @Test
    public void testBatchAdd() {
        assertThat(intentStore.getIntentCount(), is(0L));

        List<IntentData> intents = generateIntentList(5);

        intentStore.batchWrite(intents);
        assertThat(intentStore.getIntentCount(), is(5L));
    }


    /**
     * Tests adding and withdrawing an Intent.
     */
    @Test
    public void testAddAndWithdrawIntent() {
        // build and install one intent
        Intent intent = builder1.build();
        IntentData installed = new IntentData(
                intent,
                IntentState.INSTALLED,
                new IntentTestsMocks.MockTimestamp(12));
        intentStore.write(installed);

        // check that the intent count includes the new one
        assertThat(intentStore.getIntentCount(), is(1L));

        // check that the getIntents() API returns the new intent
        intentStore.getIntents()
                .forEach(item -> assertThat(item, is(intent)));

        // check that the getInstallableIntents() API returns the new intent
        intentStore.getInstallableIntents(intent.key())
                .forEach(item -> assertThat(item, is(intent)));

        // check that the getIntent() API can find the new intent
        Intent queried = intentStore.getIntent(intent.key());
        assertThat(queried, is(intent));

        // check that the state of the new intent is correct
        IntentState state = intentStore.getIntentState(intent.key());
        assertThat(state, is(IntentState.INSTALLED));

        // check that the getIntentData() API returns the proper value for the
        // new intent
        IntentData dataByQuery = intentStore.getIntentData(intent.key());
        assertThat(dataByQuery, is(installed));

        // check that the getIntentData() API returns the new intent when given
        // a time stamp to look for
        Iterable<IntentData> dataIteratorByTime = intentStore.getIntentData(true, 10L);
        assertThat(dataIteratorByTime.iterator().hasNext(), is(true));
        dataIteratorByTime.forEach(
                data -> assertThat(data, is(installed))
        );

        // check that the getIntentData() API returns the new intent when asked to
        // find all intents
        Iterable<IntentData> dataIteratorAll = intentStore.getIntentData(false, 0L);
        assertThat(dataIteratorAll.iterator().hasNext(), is(true));
        dataIteratorAll.forEach(
                data -> assertThat(data, is(installed))
        );

        // now purge the intent that was created
        IntentData purgeAssigned =
                IntentData.assign(IntentData.purge(intent),
                                  new IntentTestsMocks.MockTimestamp(12),
                                  new NodeId("node-id"));
        intentStore.write(purgeAssigned);

        // check that no intents are left
        assertThat(intentStore.getIntentCount(), is(0L));

        // check that a getIntent() operation on the key of the purged intent
        // returns null
        Intent queriedAfterWithdrawal = intentStore.getIntent(intent.key());
        assertThat(queriedAfterWithdrawal, nullValue());
    }

    /**
     * Tests the operation of the APIs for the pending map.
     */
    @Test
    public void testPending() {
        // crete a new intent and add it as pending
        Intent intent = builder1.build();
        IntentData installed = new IntentData(
                intent,
                IntentState.INSTALLED,
                new IntentTestsMocks.MockTimestamp(11));
        intentStore.addPending(installed);

        // check that the getPending() API returns the new pending intent
        Iterable<Intent> pendingIntentIteratorAll = intentStore.getPending();
        assertThat(pendingIntentIteratorAll.iterator().hasNext(), is(true));
        pendingIntentIteratorAll.forEach(
                data -> assertThat(data, is(intent))
        );

        // check that the getPendingData() API returns the IntentData for the
        // new pending intent
        Iterable<IntentData> pendingDataIteratorAll = intentStore.getPendingData();
        assertThat(pendingDataIteratorAll.iterator().hasNext(), is(true));
        pendingDataIteratorAll.forEach(
                data -> assertThat(data, is(installed))
        );

        // check that the new pending intent is returned by the getPendingData()
        // API when a time stamp is provided
        Iterable<IntentData> pendingDataIteratorSelected =
                intentStore.getPendingData(true, 10L);
        assertThat(pendingDataIteratorSelected.iterator().hasNext(), is(true));
        pendingDataIteratorSelected.forEach(
                data -> assertThat(data, is(installed))
        );

        // check that the new pending intent is returned by the getPendingData()
        // API when a time stamp is provided
        Iterable<IntentData> pendingDataIteratorAllFromTimestamp =
                intentStore.getPendingData(false, 0L);
        assertThat(pendingDataIteratorAllFromTimestamp.iterator().hasNext(), is(true));
        pendingDataIteratorSelected.forEach(
                data -> assertThat(data, is(installed))
        );
    }

    private class MockComponentConfigService implements ComponentConfigService {

        public MockComponentConfigService() {

        }

        @Override
        public Set<String> getComponentNames() {
            return null;
        }

        @Override
        public void registerProperties(Class<?> componentClass) {

        }

        @Override
        public void unregisterProperties(Class<?> componentClass, boolean clear) {

        }

        @Override
        public Set<ConfigProperty> getProperties(String componentName) {
            return null;
        }

        @Override
        public void setProperty(String componentName, String name, String value) {

        }

        @Override
        public void preSetProperty(String componentName, String name, String value) {

        }

        @Override
        public void unsetProperty(String componentName, String name) {

        }
    }
}
