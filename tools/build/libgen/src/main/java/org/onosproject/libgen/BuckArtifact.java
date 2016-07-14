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
package org.onosproject.libgen;

import org.eclipse.aether.artifact.Artifact;

/**
 * Representation of a remote artifact for Buck.
 */
public abstract class BuckArtifact {

    private final String name;
    private final String sha;
    private final boolean osgiReady;

    public static BuckArtifact getArtifact(String name, Artifact artifact, String sha, String repo, boolean osgiReady) {
        return new MavenArtifact(name, artifact, sha, repo, osgiReady);
    }
    public static BuckArtifact getArtifact(String name, String url, String sha, String mavenCoords, boolean osgiReady) {
        return new HTTPArtifact(name, url, sha, mavenCoords, osgiReady);
    }
    public static BuckArtifact getArtifact(String name, String url, String sha) {
        return new HTTPArtifact(name, url, sha, null, true);
    }

    public BuckArtifact(String name, String sha, boolean osgiReady) {
        this.name = name;
        this.sha = sha;
        this.osgiReady = osgiReady;
    }

    public String name() {
        return name;
    }

    abstract String fileName();

    abstract String url();

    private String jarTarget() {
        return name != null ? name : fileName();
    }

    private boolean isPublic() {
        return name != null;
    }

    boolean isOsgiReady() {
        return osgiReady;
    }

    String mavenCoords() {
        return null;
    }

    public String getBuckFragment() {
        String visibility = isPublic() ? "[ 'PUBLIC' ]" : "[]";

        boolean isJar = fileName().endsWith(".jar");
        String output = (isJar ? "remote_jar" : "remote_file") + " (\n" +
                "  name = '%s',\n" + // jar target
                "  out = '%s',\n" + // jar file name
                "  url = '%s',\n" + // maven url
                "  sha1 = '%s',\n" + // artifact sha
                ( isJar && mavenCoords() != null ?
                "  maven_coords = '"+ mavenCoords()+"',\n" : "" ) +
                "  visibility = %s,\n" +
                ")\n\n";

        return String.format(output, jarTarget(), fileName(), url(), sha, visibility);
    }

    private static class HTTPArtifact extends BuckArtifact {
        private final String url;
        private final String mavenCoords;

        public HTTPArtifact(String name, String url, String sha, String mavenCoords, boolean osgiReady) {
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
    }

    private static class MavenArtifact extends BuckArtifact {
        private final Artifact artifact;
        private final String repo;

        private MavenArtifact(String name, Artifact artifact, String sha, String repo, boolean osgiReady) {
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

        //FIXME get sources jars

        @Override
        String mavenCoords() {
            String classifer = artifact.getClassifier();
            if (!isOsgiReady()) {
                classifer = "NON-OSGI" + classifer;
            }

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
