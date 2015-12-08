/*
 * Copyright 2015 Open Networking Laboratory
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
package org.onosproject.faultmanagement.web;

import com.sun.jersey.api.client.WebResource;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import org.junit.Ignore;
import org.onlab.osgi.ServiceDirectory;
import org.onlab.osgi.TestServiceDirectory;
import org.onlab.rest.BaseResource;
import org.onosproject.codec.CodecService;
import org.onosproject.codec.impl.CodecManager;
import org.onosproject.rest.ResourceTest;

/**
 * Test of the Fault Management Web REST API for Alarms.
 */
public class AlarmsWebResourceTest extends ResourceTest {

    @Before
    public void setUp() {

        final CodecManager codecService = new CodecManager();
        codecService.activate();

        final ServiceDirectory testDirectory = new TestServiceDirectory()
                // Currently no alarms-service implemented
                // .add(AlarmsService.class, alarmsService)
                .add(CodecService.class, codecService);
        BaseResource.setServiceDirectory(testDirectory);
    }

    @Test
    @Ignore
    public void getAllAlarms() {
        final WebResource rs = resource();
        final String response = rs.path("/alarms").get(String.class);
        // Ensure hard-coded alarms returned okay
        assertThat(response, containsString("\"NE is not reachable\","));
        assertThat(response, containsString("\"Equipment Missing\","));
    }

    @Test
    @Ignore
    public void getAlarm() {
        final WebResource rs = resource();
        final String response = rs.path("/alarms/1").get(String.class);
        // Ensure hard-coded alarms returned okay
        assertThat(response, containsString("\"NE is not reachable\","));
        assertThat(response, not(containsString("\"Equipment Missing\",")));
    }

}
