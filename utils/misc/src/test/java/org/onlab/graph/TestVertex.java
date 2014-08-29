package org.onlab.graph;

import java.util.Objects;

import static com.google.common.base.Objects.toStringHelper;

/**
 * Test vertex.
 */
public class TestVertex implements Vertex {

    private final String name;

    public TestVertex(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return toStringHelper(this).add("name", name).toString();
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof TestVertex) {
            final TestVertex other = (TestVertex) obj;
            return Objects.equals(this.name, other.name);
        }
        return false;
    }

}
