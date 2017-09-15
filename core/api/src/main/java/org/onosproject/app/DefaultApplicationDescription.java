/*
 * Copyright 2015-present Open Networking Foundation
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

import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.onosproject.core.ApplicationRole;
import org.onosproject.core.Version;
import org.onosproject.security.Permission;

import com.google.common.collect.ImmutableList;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Default implementation of network control/management application descriptor.
 */
public final class DefaultApplicationDescription implements ApplicationDescription {

    private final String name;
    private final Version version;
    private final String title;
    private final String description;
    private final String origin;
    private final String category;
    private final String url;
    private final String readme;
    private final byte[] icon;
    private final ApplicationRole role;
    private final Set<Permission> permissions;
    private final Optional<URI> featuresRepo;
    private final List<String> features;
    private final List<String> requiredApps;

    /**
     * Default constructor is hidden to prevent calls to new.
     */
    private DefaultApplicationDescription() {
        //  Should not happen
        throw new UnsupportedOperationException();
    }

    /**
     * Creates a new application descriptor using the supplied data.
     *
     * @param name         application name
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
    private DefaultApplicationDescription(String name, Version version, String title,
                                          String description, String origin, String category,
                                          String url, String readme, byte[] icon,
                                          ApplicationRole role, Set<Permission> permissions,
                                          URI featuresRepo, List<String> features,
                                          List<String> requiredApps) {
        this.name = name;
        this.version = version;
        this.title = title;
        this.description = description;
        this.origin = origin;
        this.category = category;
        this.url = url;
        this.readme = readme;
        this.icon = icon;
        this.role = role;
        this.permissions = permissions;
        this.featuresRepo = Optional.ofNullable(featuresRepo);
        this.features = ImmutableList.copyOf(features);
        this.requiredApps = ImmutableList.copyOf(requiredApps);
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
        return icon;
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
                .add("title", title)
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

    /**
     * Returns a default application description builder.
     *
     * @return builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Default application description builder.
     */
    public static final class Builder {

        private String name;
        private Version version;
        private String title;
        private String description;
        private String category;
        private String url;
        private String readme;
        private byte[] icon;
        private String origin;
        private ApplicationRole role;
        private Set<Permission> permissions;
        private URI featuresRepo;
        private List<String> features;
        private List<String> requiredApps;

        /**
         * Default constructor for the builder.
         */
        public Builder() {}

        /**
         * Adds an application id.
         *
         * @param name application name
         * @return builder
         */
        public Builder withName(String name) {
            this.name = name;
            return this;
        }

        /**
         * Adds a version string.
         *
         * @param version version string
         * @return builder
         */
        public Builder withVersion(Version version) {
            this.version = version;
            return this;
        }

        /**
         * Adds a title string.
         *
         * @param title title string
         * @return builder
         */
        public Builder withTitle(String title) {
            this.title = title;
            return this;
        }

        /**
         * Adds a description string.
         *
         * @param description description string
         * @return builder
         */
        public Builder withDescription(String description) {
            this.description = description;
            return this;
        }

        /**
         * Adds a category string.
         *
         * @param category category string
         * @return builder
         */
        public Builder withCategory(String category) {
            this.category = category;
            return this;
        }

        /**
         * Adds a URL string.
         *
         * @param url url string
         * @return builder
         */
        public Builder withUrl(String url) {
            this.url = url;
            return this;
        }

        /**
         * Adds a readme string.
         *
         * @param readme readme string
         * @return builder
         */
        public Builder withReadme(String readme) {
            this.readme = readme;
            return this;
        }

        /**
         * Adds an icon.
         *
         * @param icon icon data
         * @return builder
         */
        public Builder withIcon(byte[] icon) {
            this.icon = icon;
            return this;
        }

        /**
         * Adds an origin string.
         *
         * @param origin origin string
         * @return builder
         */
        public Builder withOrigin(String origin) {
            this.origin = origin;
            return this;
        }

        /**
         * Adds an application role.
         *
         * @param role application role
         * @return builder
         */
        public Builder withRole(ApplicationRole role) {
            this.role = role;
            return this;
        }

        /**
         * Adds a permissions set.
         *
         * @param permissions permissions set
         * @return builder
         */
        public Builder withPermissions(Set<Permission> permissions) {
            this.permissions = permissions;
            return this;
        }

        /**
         * Adds a URI for a features repository.
         *
         * @param featuresRepo Optional URI for a features repository
         * @return builder
         */
        public Builder withFeaturesRepo(URI featuresRepo) {
            this.featuresRepo = featuresRepo;
            return this;
        }

        /**
         * Adds a features list.
         *
         * @param features features list
         * @return builder
         */
        public Builder withFeatures(List<String> features) {
            this.features = features;
            return this;
        }

        /**
         * Adds a list of required applications.
         *
         * @param requiredApps List of name strings of required applications
         * @return builder
         */
        public Builder withRequiredApps(List<String> requiredApps) {
            this.requiredApps = requiredApps;
            return this;
        }

        /**
         * Builds a default application object from the gathered parameters.
         *
         * @return new default application
         */
        public DefaultApplicationDescription build() {
            checkNotNull(name, "Name cannot be null");
            checkNotNull(version, "Version cannot be null");
            checkNotNull(title, "Title cannot be null");
            checkNotNull(description, "Description cannot be null");
            checkNotNull(origin, "Origin cannot be null");
            checkNotNull(category, "Category cannot be null");
            checkNotNull(readme, "Readme cannot be null");
            checkNotNull(role, "Role cannot be null");
            checkNotNull(permissions, "Permissions cannot be null");
            checkNotNull(features, "Features cannot be null");
            checkNotNull(requiredApps, "Required apps cannot be null");
            checkArgument(!features.isEmpty(), "There must be at least one feature");

            return new DefaultApplicationDescription(name, version, title,
                                          description, origin, category,
                                          url, readme, icon,
                                          role, permissions,
                                          featuresRepo, features,
                                          requiredApps);
        }
    }
}
