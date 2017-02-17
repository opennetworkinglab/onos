/*
 * Copyright 2017-present Open Networking Laboratory
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
package org.onosproject.cli.net.completer;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;

import org.onosproject.cli.AbstractChoicesCompleter;
import org.onosproject.net.Link;

/**
 * Completer for {@link Link.Type}.
 */
public class LinkTypeCompleter extends AbstractChoicesCompleter {

    @Override
    protected List<String> choices() {
        return EnumSet.allOf(Link.Type.class).stream()
                .sorted(Comparator.comparingInt(Enum::ordinal))
                .map(Enum::name)
                .collect(Collectors.toList());
    }

}
