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
package org.onosproject.vtnweb.resources;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.google.common.collect.ImmutableList;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onlab.osgi.ServiceDirectory;
import org.onlab.osgi.TestServiceDirectory;
import org.onlab.rest.BaseResource;
import org.onosproject.codec.CodecService;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.vtnrsc.classifier.ClassifierService;
import org.onosproject.vtnweb.web.SfcCodecContext;

import javax.ws.rs.client.WebTarget;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.onosproject.net.NetTestTools.device;
import static org.onosproject.net.NetTestTools.did;

/**
 * Unit tests for classifier REST APIs.
 */
public class ClassifierResourceTest extends VtnResourceTest {

    final ClassifierService classifierService = createMock(ClassifierService.class);

    /**
     * Sets up the global values for all the tests.
     */
    @Before
    public void setUpTest() {

        SfcCodecContext context = new SfcCodecContext();
        ServiceDirectory testDirectory = new TestServiceDirectory().add(ClassifierService.class, classifierService)
                .add(CodecService.class, context.codecManager());
        BaseResource.setServiceDirectory(testDirectory);

    }

    /**
     * Cleans up.
     */
    @After
    public void tearDownTest() {
    }

    /**
     * Tests the result of the rest api GET when there are no classifiers.
     */
    @Test
    public void testClassifiersEmpty() {

        expect(classifierService.getClassifiers()).andReturn(null).anyTimes();
        replay(classifierService);
        final WebTarget wt = target();
        final String response = wt.path("classifiers").request().get(String.class);
        assertThat(response, is("{\"classifiers\":[]}"));
    }

    /**
     * Tests the result of a rest api GET for classifiers.
     */
    @Test
    public void testClassifiers() {

        DeviceId devId1 = did("dev1");
        Device device1 = device("dev1");

        expect(classifierService.getClassifiers()).andReturn(ImmutableList.of(devId1)).anyTimes();
        replay(classifierService);

        final WebTarget wt = target();
        final String response = wt.path("classifiers").request().get(String.class);
        final JsonObject result = Json.parse(response).asObject();
        assertThat(result, notNullValue());
    }
}
