package org.onlab.graph;

/**
 * Representation of a graph search algorithm and its outcome.
 *
 * @param <V>    vertex type
 * @param <E>    edge type
 */
public interface GraphSearch<V extends Vertex, E extends Edge<V>> {

    /**
     * Notion of a graph search result.
     */
    public interface Result<V extends Vertex, E extends Edge<V>> {
    }

    /**
     * Searches the specified graph.
     *
     * @param graph  graph to be searched
     * @param weight optional edge-weight; if null cost of each edge will be
     *               assumed to be 1.0
     *
     * @return search results
     */
    Result search(Graph<V, E> graph, EdgeWeight<V, E> weight);

}
