/*
 * Copyright 2018-present Open Networking Foundation
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
package org.onosproject.alarm;

import com.google.common.collect.ImmutableSet;
import org.onlab.util.Identifier;

import java.net.URI;
import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Immutable representation of a alarm source. It is meaningful within the
 * context of a device.
 */
public final class AlarmEntityId extends Identifier<URI> {

    public static final AlarmEntityId NONE = new AlarmEntityId(URI.create("none:none"));
    public static final Set<String> SCHEMES = ImmutableSet.of("none", "port", "och", "other");

    private AlarmEntityId(final URI uri) {
        super(uri);
    }

    protected AlarmEntityId() {
        super(NONE.identifier);
    }

    public static AlarmEntityId alarmEntityId(final String string) {
        return alarmEntityId(URI.create(string));
    }

    public static AlarmEntityId alarmEntityId(final URI uri) {
        checkArgument(SCHEMES.contains(uri.getScheme()), "Unexpected scheme");
        return new AlarmEntityId(uri);
    }
}
