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
package org.onosproject.core;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
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
    private final String title;
    private final String description;
    private final String category;
    private final String url;
    private final String readme;
    private final byte[] icon;
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
     * @param title        application title
     * @param description  application description
     * @param origin       origin company
     * @param category     application category
     * @param url          application URL
     * @param readme       application readme
     * @param icon         application icon
     * @param role         application role
     * @param permissions  requested permissions
     * @param featuresRepo optional features repo URI
     * @param features     application features
     * @param requiredApps list of required application names
     */
    public DefaultApplication(ApplicationId appId, Version version, String title,
                              String description, String origin, String category,
                              String url, String readme, byte[] icon,
                              ApplicationRole role, Set<Permission> permissions,
                              Optional<URI> featuresRepo, List<String> features,
                              List<String> requiredApps) {
        this.appId = checkNotNull(appId, "ID cannot be null");
        this.version = checkNotNull(version, "Version cannot be null");
        this.title = checkNotNull(title, "Title cannot be null");
        this.description = checkNotNull(description, "Description cannot be null");
        this.origin = checkNotNull(origin, "Origin cannot be null");
        this.category = checkNotNull(category, "Category cannot be null");
        this.url = url;
        this.readme = checkNotNull(readme, "Readme cannot be null");
        this.icon = icon == null ? new byte[0] : icon.clone();
        this.role = checkNotNull(role, "Role cannot be null");
        this.permissions = ImmutableSet.copyOf(
                checkNotNull(permissions, "Permissions cannot be null")
        );
        this.featuresRepo = checkNotNull(featuresRepo, "Features repo cannot be null");
        this.features = ImmutableList.copyOf(
                checkNotNull(features, "Features cannot be null")
        );
        this.requiredApps = ImmutableList.copyOf(
                checkNotNull(requiredApps, "Required apps cannot be null")
        );
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
    public String title() {
        return title;
    }

    @Override
    public String description() {
        return description;
    }

    @Override
    public String category() {
        return category;
    }

    @Override
    public String url() {
        return url;
    }

    @Override
    public String readme() {
        return readme;
    }

    @Override
    public byte[] icon() {
        return icon.clone();
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
        return Objects.hash(appId, version, title, description, origin, category, url,
                            readme, role, permissions, featuresRepo, features, requiredApps);
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
        // TODO: review -- do ALL the fields need to be included?
        // It is debatable whether fields like description, url, and readme,
        //   need to be included in the notion of equivalence.
        return Objects.equals(this.appId, other.appId) &&
                Objects.equals(this.version, other.version) &&
                Objects.equals(this.title, other.title) &&
                Objects.equals(this.description, other.description) &&
                Objects.equals(this.origin, other.origin) &&
                Objects.equals(this.category, other.category) &&
                Objects.equals(this.url, other.url) &&
                Objects.equals(this.readme, other.readme) &&
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
                .add("title", title)
                .add("description", description)
                .add("origin", origin)
                .add("category", category)
                .add("url", url)
                .add("readme", readme)
                .add("role", role)
                .add("permissions", permissions)
                .add("featuresRepo", featuresRepo)
                .add("features", features)
                .add("requiredApps", requiredApps)
                .toString();
    }
}
