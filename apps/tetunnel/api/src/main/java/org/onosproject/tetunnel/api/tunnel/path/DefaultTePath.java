/*
 * Copyright 2016-present Open Networking Laboratory
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

package org.onosproject.tetunnel.api.tunnel.path;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.onosproject.tetunnel.api.lsp.TeLspKey;

import java.util.List;

/**
 * Default implementation of TE path.
 */
public class DefaultTePath implements TePath {

    private final Type type;
    private final List<TeLspKey> lsps;
    private final List<TeRouteSubobject> explicitRoute;
    private final List<TePath> secondaryPaths;

    /**
     * Creates a default implementation of TE path with supplied information.
     *
     * @param type type of TE Path
     * @param lsps LSPs of the TE Path
     * @param explicitRoute explicit route of the (Explicit) TE path
     * @param secondaryPaths secondary paths of the TE path
     */
    public DefaultTePath(Type type, List<TeLspKey> lsps,
                         List<TeRouteSubobject> explicitRoute,
                         List<TePath> secondaryPaths) {
        this.type = type;
        if (lsps == null) {
            this.lsps = Lists.newArrayList();
        } else {
            this.lsps = Lists.newArrayList(lsps);
        }
        if (explicitRoute == null) {
            this.explicitRoute = Lists.newArrayList();
        } else {
            this.explicitRoute = Lists.newArrayList(explicitRoute);
        }
        if (secondaryPaths == null) {
            this.secondaryPaths = Lists.newArrayList();
        } else {
            this.secondaryPaths = Lists.newArrayList(secondaryPaths);
        }
    }

    @Override
    public Type type() {
        return type;
    }

    @Override
    public List<TeLspKey> lsps() {
        return ImmutableList.copyOf(lsps);
    }

    @Override
    public List<TeRouteSubobject> explicitRoute() {
        return ImmutableList.copyOf(explicitRoute);
    }

    @Override
    public List<TePath> secondaryPaths() {
        return ImmutableList.copyOf(secondaryPaths);
    }
}
