/*
 * Copyright 2014-present Open Networking Laboratory
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
package org.onosproject.net.intent;

import java.util.Collection;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.onosproject.net.EdgeLink;
import org.onosproject.net.Link;

/**
 * Matcher to determine if a Collection of Links contains a path between a source
 * and a destination.
 */
public class LinksHaveEntryWithSourceDestinationPairMatcher extends
        TypeSafeMatcher<Collection<Link>> {
    private final String source;
    private final String destination;

    /**
     * Creates a matcher for a given path represented by a source and
     * a destination.
     *
     * @param source string identifier for the source of the path
     * @param destination string identifier for the destination of the path
     */
    LinksHaveEntryWithSourceDestinationPairMatcher(String source,
                                                   String destination) {
        this.source = source;
        this.destination = destination;
    }

    @Override
    public boolean matchesSafely(Collection<Link> links) {
        for (Link link : links) {
            if (source.equals(destination) && link instanceof EdgeLink) {
                EdgeLink edgeLink = (EdgeLink) link;
                if (edgeLink.hostLocation().elementId()
                        .toString().endsWith(source)) {
                    return true;
                }
            }

            if (link.src().elementId().toString().endsWith(source) &&
                    link.dst().elementId().toString().endsWith(destination)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("link lookup for source \"");
        description.appendText(source);
        description.appendText(" and destination ");
        description.appendText(destination);
        description.appendText("\"");
    }

    @Override
    public void describeMismatchSafely(Collection<Link> links,
                                       Description mismatchDescription) {
        mismatchDescription.appendText("was ").
                appendText(links.toString());
    }

    /**
     * Creates a link has path matcher.
     *
     * @param source string identifier for the source of the path
     * @param destination string identifier for the destination of the path
     * @return matcher to match the path
     */
    public static LinksHaveEntryWithSourceDestinationPairMatcher linksHasPath(
            String source,
            String destination) {
        return new LinksHaveEntryWithSourceDestinationPairMatcher(source,
                destination);
    }
}

