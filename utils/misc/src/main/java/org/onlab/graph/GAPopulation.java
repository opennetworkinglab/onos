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

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Represents a population of GAOrganisms. This class can be used
 * to run a genetic algorithm on the population and return the fittest solutions.
 */
class GAPopulation<Organism extends GAOrganism> extends ArrayList<Organism> {
    Random r = new SecureRandom();

    /**
     * Steps the population through one generation. The 75% least fit
     * organisms are killed off and replaced with the children of the
     * 25% (as well as some "random" newcomers).
     */
    void step() {
        Collections.sort(this, (org1, org2) ->
                org1.fitness().compareTo(org2.fitness()));
        int maxSize = size();
        for (int i = size() - 1; i > maxSize / 4; i--) {
            remove(i);
        }
        for (Organism org: this) {
            if (r.nextBoolean()) {
                org.mutate();
            }
        }
        while (size() < maxSize * 4 / 5) {
            Organism org1 = get(r.nextInt(size()));
            Organism org2 = get(r.nextInt(size()));
            add((Organism) org1.crossWith(org2));
        }

        while (size() < maxSize) {
            Organism org1 = get(r.nextInt(size()));
            add((Organism) org1.random());
        }
    }

    /**
     * Runs GA for the specified number of iterations, and returns
     * a sample of the resulting population of solutions.
     *
     * @param generations   Number of generations to run GA for
     * @param populationSize    Population size of GA
     * @param sample        Number of solutions to ask for
     * @param template      Template GAOrganism to seed the population with
     * @return  ArrayList containing sample number of organisms
     */
    List<Organism> runGA(int generations, int populationSize, int sample, Organism template) {
        for (int i = 0; i < populationSize; i++) {
            add((Organism) template.random());
        }

        for (int i = 0; i < generations; i++) {
            step();
        }
        for (int i = size() - 1; i >= sample; i--) {
            remove(i);
        }
        return new ArrayList<>(this);
    }
}

