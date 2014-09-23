package org.onlab.graph;

import java.util.Objects;

/**
 * Test vertex.
 */
public class TestVertex implements Vertex {

    private final String name;

    public TestVertex(String name) {
        this.name = name;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof TestVertex) {
            final TestVertex other = (TestVertex) obj;
            return Objects.equals(this.name, other.name);
        }
        return false;
    }

    @Override
    public String toString() {
        return name;
    }

}
