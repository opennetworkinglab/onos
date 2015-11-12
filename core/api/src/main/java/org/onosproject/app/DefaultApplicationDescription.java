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
package org.onosproject.app;

import org.onosproject.core.ApplicationRole;
import org.onosproject.core.Version;
import org.onosproject.security.Permission;

import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Default implementation of network control/management application descriptor.
 */
public class DefaultApplicationDescription implements ApplicationDescription {

    private final String name;
    private final Version version;
    private final String description;
    private final String origin;
    private final ApplicationRole role;
    private final Set<Permission> permissions;
    private final Optional<URI> featuresRepo;
    private final List<String> features;
    private final List<String> requiredApps;

    /**
     * Creates a new application descriptor using the supplied data.
     *
     * @param name         application name
     * @param version      application version
     * @param description  application description
     * @param origin       origin company
     * @param role         application role
     * @param permissions  requested permissions
     * @param featuresRepo optional features repo URI
     * @param features     application features
     * @param requiredApps list of required application names
     */
    public DefaultApplicationDescription(String name, Version version,
                                         String description, String origin,
                                         ApplicationRole role, Set<Permission> permissions,
                                         URI featuresRepo, List<String> features,
                                         List<String> requiredApps) {
        this.name = checkNotNull(name, "Name cannot be null");
        this.version = checkNotNull(version, "Version cannot be null");
        this.description = checkNotNull(description, "Description cannot be null");
        this.origin = checkNotNull(origin, "Origin cannot be null");
        this.role = checkNotNull(role, "Role cannot be null");
        this.permissions = checkNotNull(permissions, "Permissions cannot be null");
        this.featuresRepo = Optional.ofNullable(featuresRepo);
        this.features = checkNotNull(features, "Features cannot be null");
        this.requiredApps = checkNotNull(requiredApps, "Required apps cannot be null");
        checkArgument(!features.isEmpty(), "There must be at least one feature");
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public Version version() {
        return version;
    }

    @Override
    public String description() {
        return description;
    }

    @Override
    public String origin() {
        return origin;
    }

    @Override
    public ApplicationRole role() {
        return role;
    }

    @Override
    public Set<Permission> permissions() {
        return permissions;
    }

    @Override
    public Optional<URI> featuresRepo() {
        return featuresRepo;
    }

    @Override
    public List<String> features() {
        return features;
    }

    @Override
    public List<String> requiredApps() {
        return requiredApps;
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("name", name)
                .add("version", version)
                .add("description", description)
                .add("origin", origin)
                .add("role", role)
                .add("permissions", permissions)
                .add("featuresRepo", featuresRepo)
                .add("features", features)
                .add("requiredApps", requiredApps)
                .toString();
    }
}
