package org.onlab.graph;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Represents a population of GAOrganisms. This class can be used
 * to run a genetic algorithm on the population and return the fittest solutions.
 */
class GAPopulation<Organism extends GAOrganism> extends ArrayList<Organism> {
    Random r = new Random();

    /**
     * Steps the population through one generation. The 75% least fit
     * organisms are killed off and replaced with the children of the
     * 25% (as well as some "random" newcomers).
     */
    void step() {
        Collections.sort(this, (org1, org2) -> {
            double d = org1.fitness() - org2.fitness();
            if (d < 0) {
                return -1;
            } else if (d == 0) {
                return 0;
            }
            return 1;
        });
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

