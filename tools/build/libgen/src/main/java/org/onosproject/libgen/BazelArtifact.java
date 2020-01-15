/*
 * Copyright 2016-present Open Networking Foundation
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
package org.onosproject.libgen;

import org.eclipse.aether.artifact.Artifact;

/**
 * Representation of a remote artifact for Bazel.
 */
public abstract class BazelArtifact {

    private final String name;
    private final String sha;
    private final boolean osgiReady;

    public static BazelArtifact getArtifact(String name, Artifact artifact, String sha, String repo,
                                            boolean osgiReady) {
        return new MavenArtifact(name, artifact, sha, repo, osgiReady);
    }

    public static BazelArtifact getArtifact(String name, String url, String sha, String mavenCoords,
                                            boolean osgiReady) {
        return new HTTPArtifact(name, url, sha, mavenCoords, osgiReady);
    }

    public static org.onosproject.libgen.BazelArtifact getArtifact(String name, String url, String sha) {
        return new HTTPArtifact(name, url, sha, null, true);
    }

    public BazelArtifact(String name, String sha, boolean osgiReady) {
        this.name = name;
        this.sha = sha;
        this.osgiReady = osgiReady;
    }

    public String name() {
        return name.replaceAll("[.-]", "_");
    }

    abstract String fileName();

    abstract String url();

    abstract String url(boolean withClassifier);

    private String jarTarget() {
        return name != null ? name() : fileName();
    }

    private boolean isPublic() {
        return name != null;
    }

    boolean isOsgiReady() {
        return osgiReady;
    }

    String httpUrl() {
        return "";
    }

    String mavenCoords() {
        return null;
    }

    String bazelExport() {
        return "@" + jarTarget() + "//:" + jarTarget();
    }

    private boolean isJar() {
        return fileName().endsWith(".jar");
    }

    String getMavenJarFragment() {
        System.out.println(name + " == " + httpUrl());
        String sha256 = BazelLibGenerator.getHttpSha256(name, httpUrl());
        String format = "\n" +
                "    if \"%s\" not in native.existing_rules():\n" +
                "        java_import_external(\n" +
                "            name = \"%s\",\n" +
                "            jar_sha256 = \"%s\",\n" +
                "            licenses = [\"notice\"],\n" +
                "            jar_urls = [\"%s\"]," +
                "        )";

        return String.format(format, jarTarget(), jarTarget(), sha256, httpUrl());

    }

    String getFragment() {
        String visibility = isPublic() ? "[ 'PUBLIC' ]" : "[]";

        String output = (isJar() ? "remote_jar" : "remote_file") + " (\n" +
                "  name = '%s',\n" + // jar target
                "  out = '%s',\n" + // jar file name
                "  url = '%s',\n" + // maven url
                "  sha1 = '%s',\n" + // artifact sha
                (isJar() && mavenCoords() != null ?
                        "  maven_coords = '" + mavenCoords() + "',\n" : "") +
                "  visibility = %s,\n" +
                ")\n\n";

        return String.format(output, jarTarget(), fileName(), url(), sha, visibility);
    }

    private static class HTTPArtifact extends BazelArtifact {
        private final String url;
        private final String mavenCoords;

        public HTTPArtifact(String name, String url, String sha,
                            String mavenCoords, boolean osgiReady) {
            super(name, sha, osgiReady);
            this.url = url;
            this.mavenCoords = mavenCoords;
        }

        @Override
        String fileName() {
            String[] parts = url.split("/");
            return parts[parts.length - 1];
        }

        @Override
        String mavenCoords() {
            return mavenCoords;
        }

        @Override
        String url() {
            return url;
        }

        @Override
        String url(boolean withClassifier) {
            return url;
        }

        @Override
        String httpUrl() {
            return url;
        }
    }

    private static class MavenArtifact extends BazelArtifact {
        private final Artifact artifact;
        private final String repo;

        private MavenArtifact(String name, Artifact artifact, String sha,
                              String repo, boolean osgiReady) {
            super(name, sha, osgiReady);
            this.artifact = artifact;
            this.repo = repo;
        }

        @Override
        String url() {
            //mvn:[repo:]groupId:artifactId:[extension:[classifier:]]:version
            StringBuilder mvnUrl = new StringBuilder("mvn:");
            if (repo != null && repo.length() > 0) {
                mvnUrl.append(repo).append(':');
            }
            mvnUrl.append(artifact.getGroupId()).append(':')
                    .append(artifact.getArtifactId()).append(':')
                    .append(artifact.getExtension()).append(':');
            if (artifact.getClassifier() != null && artifact.getClassifier().length() > 0) {
                mvnUrl.append(artifact.getClassifier()).append(':');
            }
            mvnUrl.append(artifact.getVersion());
            return mvnUrl.toString();
        }

        @Override
        String httpUrl() {
            return "https://repo1.maven.org/maven2/" +
                    artifact.getGroupId().replace(".", "/") +
                    "/" + artifact.getArtifactId() +
                    "/" + artifact.getVersion() +
                    "/" + artifact.getFile().getName();
        }

        @Override
        String url(boolean withClassifier) {
            String url = url();
            if (withClassifier && !isOsgiReady()) {
                int i = url.lastIndexOf(':');
                if (i > 0) {
                    url = url.substring(0, i) + ":NON-OSGI" + url.substring(i);
                }
            }
            return url;
        }

        //FIXME get sources jars

        @Override
        String mavenCoords() {
            String classifer = artifact.getClassifier();
            if ("jar".equals(artifact.getExtension().toLowerCase()) &&
                    classifer.length() == 0) {
                // shorter form
                return String.format("%s:%s:%s",
                                     artifact.getGroupId(),
                                     artifact.getArtifactId(),
                                     artifact.getVersion());
            }
            return String.format("%s:%s:%s:%s:%s",
                                 artifact.getGroupId(),
                                 artifact.getArtifactId(),
                                 artifact.getExtension(),
                                 classifer,
                                 artifact.getVersion());
        }

        @Override
        String fileName() {
            return String.format("%s-%s.%s",
                                 artifact.getArtifactId(),
                                 artifact.getVersion(),
                                 artifact.getExtension());
        }
    }
}
