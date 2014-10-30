/*
 * Copyright 2014 Open Networking Laboratory
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
package org.onlab.onos.net.intent;

import org.junit.Test;
import org.onlab.onos.net.NetTestTools;
import org.onlab.onos.net.Path;
import org.onlab.onos.net.resource.LinkResourceRequest;

import static org.junit.Assert.assertEquals;

public class PathIntentTest extends ConnectivityIntentTest {
    // 111:11 --> 222:22
    private static final Path PATH1 = NetTestTools.createPath("111", "222");

    // 111:11 --> 333:33
    private static final Path PATH2 = NetTestTools.createPath("222", "333");

    @Test
    public void basics() {
        PathIntent intent = createOne();
        assertEquals("incorrect id", APPID, intent.appId());
        assertEquals("incorrect match", MATCH, intent.selector());
        assertEquals("incorrect action", NOP, intent.treatment());
        assertEquals("incorrect path", PATH1, intent.path());
    }

    @Override
    protected PathIntent createOne() {
        return new PathIntent(APPID, MATCH, NOP, PATH1, new LinkResourceRequest[0]);
    }

    @Override
    protected PathIntent createAnother() {
        return new PathIntent(APPID, MATCH, NOP, PATH2, new LinkResourceRequest[0]);
    }
}
