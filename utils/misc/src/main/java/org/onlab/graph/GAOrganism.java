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
    Comparable fitness();

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
