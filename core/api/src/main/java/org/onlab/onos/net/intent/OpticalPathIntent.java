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

import java.util.Collection;

import org.onlab.onos.core.ApplicationId;
import org.onlab.onos.net.ConnectPoint;
import org.onlab.onos.net.Link;
import org.onlab.onos.net.NetworkResource;
import org.onlab.onos.net.Path;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableSet;

public class OpticalPathIntent extends Intent {

    private final ConnectPoint src;
    private final ConnectPoint dst;
    private final Path path;


    public OpticalPathIntent(ApplicationId appId,
            ConnectPoint src,
            ConnectPoint dst,
            Path path) {
        super(id(OpticalPathIntent.class, src, dst, path),
              appId,
              ImmutableSet.<NetworkResource>copyOf(path.links()));
        this.src = src;
        this.dst = dst;
        this.path = path;
    }

    protected OpticalPathIntent() {
        this.src = null;
        this.dst = null;
        this.path = null;
    }

    public ConnectPoint src() {
        return src;
    }

    public ConnectPoint dst() {
        return dst;
    }

    public Path path() {
        return path;
    }

    @Override
    public boolean isInstallable() {
        return true;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("id", id())
                .add("ingressPort", src)
                .add("egressPort", dst)
                .add("path", path)
                .toString();
    }


    public Collection<Link> requiredLinks() {
        return path.links();
    }
}
