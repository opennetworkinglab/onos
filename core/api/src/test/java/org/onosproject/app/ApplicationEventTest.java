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
package org.onosproject.app;

import org.junit.Test;
import org.onosproject.core.Application;
import org.onosproject.core.DefaultApplication;
import org.onosproject.event.AbstractEventTest;

import java.util.Optional;

import static org.onosproject.app.ApplicationEvent.Type.APP_ACTIVATED;
import static org.onosproject.app.DefaultApplicationDescriptionTest.*;
import static org.onosproject.core.DefaultApplicationTest.APP_ID;

/**
 * Test of the application event.
 */
public class ApplicationEventTest extends AbstractEventTest {

    private Application createApp() {
        return new DefaultApplication(APP_ID, VER, TITLE, DESC, ORIGIN, CATEGORY,
                                      URL, README, ICON, ROLE, PERMS,
                                      Optional.of(FURL), FEATURES, APPS);
    }

    @Test
    public void withoutTime() {
        Application app = createApp();
        ApplicationEvent event = new ApplicationEvent(APP_ACTIVATED, app, 123L);
        validateEvent(event, APP_ACTIVATED, app, 123L);
    }

    @Test
    public void withTime() {
        Application app = createApp();
        long before = System.currentTimeMillis();
        ApplicationEvent event = new ApplicationEvent(APP_ACTIVATED, app);
        long after = System.currentTimeMillis();
        validateEvent(event, APP_ACTIVATED, app, before, after);
    }

}