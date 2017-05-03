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
package org.onosproject.mapping.cli;

import com.google.common.collect.ImmutableList;
import org.apache.commons.lang3.StringUtils;
import org.onosproject.cli.AbstractChoicesCompleter;
import org.onosproject.mapping.MappingStore.Type;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapping store type completer.
 */
public class MappingStoreTypeCompleter extends AbstractChoicesCompleter {

    private static final List<Type> STORE_TYPES =
            ImmutableList.of(Type.MAP_CACHE, Type.MAP_DATABASE);
    private static final String MAP_PREFIX = "map_";

    @Override
    protected List<String> choices() {
        return STORE_TYPES.stream().map(type ->
                removeMapPrefix(type.toString().toLowerCase()))
                .collect(Collectors.toList());
    }

    private String removeMapPrefix(String type) {
        return StringUtils.replaceAll(type, MAP_PREFIX, "");
    }
}