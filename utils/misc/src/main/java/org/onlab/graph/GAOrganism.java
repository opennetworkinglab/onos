package org.onlab.graph;

/**
 * Interface representing an "organism": a specific solution
 * to a problem where solutions can be evaluated in terms
 * of fitness. These organisms can be used to represent any
 * class of problem that genetic algorithms can be run on.
 */
interface GAOrganism {
    /**
     * A fitness function that determines how
     * optimal a given organism is.
     *
     * @return fitness of organism
     */
    double fitness();

    /**
     * A method that slightly mutates an organism.
     */
    void mutate();

    /**
     * Creates a new random organism.
     *
     * @return  random GAOrganism
     */
    GAOrganism random();

    /**
     * Returns a child organism that is the result
     * of "crossing" this organism with another.
     *
     * @param other Other organism to cross with
     * @return child organism
     */
    GAOrganism crossWith(GAOrganism other);
}
