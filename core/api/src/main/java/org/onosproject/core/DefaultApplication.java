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
package org.onosproject.core;

import org.onosproject.security.Permission;

import java.net.URI;
import java.util.Set;
import java.util.Optional;
import java.util.List;
import java.util.Objects;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Default implementation of network control/management application descriptor.
 */
public class DefaultApplication implements Application {

    private final ApplicationId appId;
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
     * @param appId        application identifier
     * @param version      application version
     * @param description  application description
     * @param origin       origin company
     * @param role         application role
     * @param permissions  requested permissions
     * @param featuresRepo optional features repo URI
     * @param features     application features
     * @param requiredApps list of required application names
     */
    public DefaultApplication(ApplicationId appId, Version version,
                              String description, String origin,
                              ApplicationRole role, Set<Permission> permissions,
                              Optional<URI> featuresRepo, List<String> features,
                              List<String> requiredApps) {
        this.appId = checkNotNull(appId, "ID cannot be null");
        this.version = checkNotNull(version, "Version cannot be null");
        this.description = checkNotNull(description, "Description cannot be null");
        this.origin = checkNotNull(origin, "Origin cannot be null");
        this.role = checkNotNull(role, "Role cannot be null");
        this.permissions = checkNotNull(permissions, "Permissions cannot be null");
        this.featuresRepo = checkNotNull(featuresRepo, "Features repo cannot be null");
        this.features = checkNotNull(features, "Features cannot be null");
        this.requiredApps = checkNotNull(requiredApps, "Required apps cannot be null");
        checkArgument(!features.isEmpty(), "There must be at least one feature");
    }

    @Override
    public ApplicationId id() {
        return appId;
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
    public int hashCode() {
        return Objects.hash(appId, version, description, origin, role, permissions,
                            featuresRepo, features, requiredApps);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final DefaultApplication other = (DefaultApplication) obj;
        return Objects.equals(this.appId, other.appId) &&
                Objects.equals(this.version, other.version) &&
                Objects.equals(this.description, other.description) &&
                Objects.equals(this.origin, other.origin) &&
                Objects.equals(this.role, other.role) &&
                Objects.equals(this.permissions, other.permissions) &&
                Objects.equals(this.featuresRepo, other.featuresRepo) &&
                Objects.equals(this.features, other.features) &&
                Objects.equals(this.requiredApps, other.requiredApps);
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("appId", appId)
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
