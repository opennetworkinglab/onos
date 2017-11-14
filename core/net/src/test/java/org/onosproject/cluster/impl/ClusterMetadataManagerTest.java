/*
 * Copyright 2017-present Open Networking Foundation
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
package org.onosproject.cluster.impl;

import com.google.common.collect.ImmutableSet;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onlab.junit.HttpResourceUrlInterceptor;
import org.onlab.junit.LoggerAdapter;
import org.onlab.junit.TestUtils;
import org.onosproject.cluster.ClusterMetadata;
import org.onosproject.cluster.ControllerNode;
import org.onosproject.cluster.Partition;
import org.onosproject.common.event.impl.TestEventDispatcher;
import org.onosproject.core.VersionServiceAdapter;
import org.slf4j.helpers.MessageFormatter;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;

/**
 * Unit tests for the cluster metadata manager.
 */

public class ClusterMetadataManagerTest {

    private static final String CLUSTER_NAME = "MyCluster";
    private static final String NODE_ID = "MyId";
    private static final String NODE_IP = "11.22.33.44";
    private static final int NODE_PORT = 4523;

    private static final String CLUSTER_METADATA_FORMAT =
            "{\"nodes\": [" +
            "   {\"ip\": \"{}\", \"id\": \"{}\", \"port\": {}}]," +
                "\"name\": \"{}\"," +
                "\"partitions\": [{\"id\": 1, \"members\": [\"{}\"]}]}";

    private static final String CLUSTER_METADATA =
            format(CLUSTER_METADATA_FORMAT, NODE_IP, NODE_ID, NODE_PORT, CLUSTER_NAME, NODE_ID);

    private ClusterMetadataManager mgr;
    private ConfigFileBasedClusterMetadataProvider fileProvider;
    private DefaultClusterMetadataProvider defaultProvider;

    private static String format(String format, Object... params) {
        return MessageFormatter.arrayFormat(format, params).getMessage();
    }

    private class TrackingLogger extends LoggerAdapter {
        int errors = 0;
        StringBuilder messages = new StringBuilder();

        public final void error(String msg, Throwable t) {
            messages.append(msg);
            messages.append(t);
            errors++;
        }
    }

    @Before
    public void setUp() {
        System.clearProperty("onos.cluster.metadata.uri");
        mgr = new ClusterMetadataManager();
        TestUtils.setField(mgr, "eventDispatcher", new TestEventDispatcher());
        fileProvider = new ConfigFileBasedClusterMetadataProvider();
        fileProvider.providerRegistry = mgr;

        defaultProvider = new DefaultClusterMetadataProvider();
        defaultProvider.providerRegistry = mgr;
        defaultProvider.versionService = new VersionServiceAdapter();

        mgr.activate();
        defaultProvider.activate();

        pause(200);
    }

    @After
    public void tearDown() {
        defaultProvider.deactivate();
        fileProvider.deactivate();
        mgr.deactivate();
    }

    private void pause(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ie) {
            throw new IllegalStateException(ie);
        }
    }

    /**
     * Tests that the file provider functions correctly when no metadata is present.
     */
    @Test
    public void testNoMetadata() {
        fileProvider.activate();
        TrackingLogger log = new TrackingLogger();
        TestUtils.setField(fileProvider, "log", log);

        assertThat(fileProvider.isAvailable(), is(false));

        boolean exceptionSeen;
        try {
            fileProvider.getClusterMetadata();
            exceptionSeen = false;
        } catch (Throwable t) {
            exceptionSeen = true;
        }
        assertThat(exceptionSeen, is(true));

        assertThat("Logger errors detected: " + log.messages.toString(), log.errors, is(0));
    }

    /**
     * Tests the default provider.
     */
    @Test
    public void testDefaultProvider() {
        fileProvider.activate();
        ClusterMetadata metadata = mgr.getClusterMetadata();

        assertThat(metadata, notNullValue());
        assertThat(metadata.getName(), is(defaultProvider.id().scheme()));
        assertThat(metadata.getNodes(), hasSize(1));
        assertThat(metadata.getPartitions(), hasSize(1));

        String localAddress = mgr.getLocalNode().ip().toString();

        ControllerNode node = metadata.getNodes().iterator().next();
        assertThat(node.ip().toString(), is(localAddress));
        assertThat(node.id().id(), is(localAddress));

        Partition partition = metadata.getPartitions().iterator().next();
        assertThat(partition.getId().asInt(), is(1));
        assertThat(partition.getMembers(), hasSize(1));
        assertThat(partition.getMembers().iterator().next().id(), is(localAddress));
    }

    /**
     * Tests the file based cluster metadata provider.
     */
    @Test
    public void testFileBasedProvider() {
        File jsonFile;

        try {
            jsonFile = File.createTempFile("cluster", "json");
            FileUtils.writeStringToFile(jsonFile, CLUSTER_METADATA);
        } catch (IOException ioe) {
            throw new IllegalStateException(ioe);
        }

        System.setProperty("onos.cluster.metadata.uri", "file://" + jsonFile.getAbsolutePath());
        fileProvider.activate();

        ClusterMetadata metadata = fileProvider.getClusterMetadata().value();
        assertThat(metadata, notNullValue());

        assertThat(metadata.getName(), is(CLUSTER_NAME));

        ControllerNode node = metadata.getNodes().iterator().next();
        assertThat(node.ip().toString(), is(NODE_IP));
        assertThat(node.id().id(), is(NODE_ID));

        Partition partition = metadata.getPartitions().iterator().next();
        assertThat(partition.getId().asInt(), is(1));
        assertThat(partition.getMembers(), hasSize(1));
        assertThat(partition.getMembers().iterator().next().id(), is(NODE_ID));

        ClusterMetadata newMetadata = new ClusterMetadata(metadata.providerId(),
                "NewMetadata",
                ImmutableSet.of(node),
                ImmutableSet.of(partition));
        mgr.setClusterMetadata(newMetadata);

        assertThat(fileProvider.getClusterMetadata().value(), equalTo(newMetadata));

        assertThat(jsonFile.delete(), is(true));
    }

    /**
     * Tests fetching metadata from an HTTP source.
     */
    @Test
    public void testUrlFetch() {
        URL.setURLStreamHandlerFactory(
                new HttpResourceUrlInterceptor.HttpResourceUrlInterceptorFactory("cluster-info.json"));
        System.setProperty("onos.cluster.metadata.uri",  "http://opennetworking.org");

        fileProvider.activate();
        pause(400);


        ClusterMetadata metadata = fileProvider.getClusterMetadata().value();
        assertThat(metadata, notNullValue());

        assertThat(metadata.getName(), is(CLUSTER_NAME));
        assertThat(metadata.getNodes(), hasSize(1));
        assertThat(metadata.getPartitions(), hasSize(1));
    }
}