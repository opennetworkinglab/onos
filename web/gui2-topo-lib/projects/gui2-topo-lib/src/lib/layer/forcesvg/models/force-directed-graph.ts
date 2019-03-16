/*
 * Copyright 2018-present Open Networking Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the 'License');
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an 'AS IS' BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import { EventEmitter } from '@angular/core';
import { Link } from './link';
import { Node } from './node';
import * as d3 from 'd3-force';
import {LogService} from 'gui2-fw-lib';

const FORCES = {
    LINKS: 1 / 50,
    COLLISION: 1,
    GRAVITY: 0.4,
    FRICTION: 0.7
};

const CHARGES = {
    device: -80,
    host: -200,
    region: -80,
    _def_: -120
};

const LINK_DISTANCE = {
    // note: key is link.type
    direct: 100,
    optical: 120,
    UiEdgeLink: 100,
    _def_: 50,
};

const LINK_STRENGTH = {
    // note: key is link.type
    // range: {0.0 ... 1.0}
    _def_: 0.1
};

export interface Options {
    width: number;
    height: number;
}

/**
 * The inspiration for this approach comes from
 * https://medium.com/netscape/visualizing-data-with-angular-and-d3-209dde784aeb
 */
export class ForceDirectedGraph {
    public ticker: EventEmitter<d3.Simulation<Node, Link>> = new EventEmitter();
    public simulation: d3.Simulation<any, any>;

    public nodes: Node[] = [];
    public links: Link[] = [];

    constructor(options: Options, public log: LogService) {
        this.initSimulation(options);
    }

    initNodes() {
        if (!this.simulation) {
            throw new Error('simulation was not initialized yet');
        }

        this.simulation.nodes(this.nodes);
    }

    initLinks() {
        if (!this.simulation) {
            throw new Error('simulation was not initialized yet');
        }

        // Initializing the links force simulation
        this.simulation.force('links',
            d3.forceLink(this.links)
                .strength(this.strength.bind(this))
                .distance(this.distance.bind(this))
        );
    }

    charges(node) {
        const nodeType = node.nodeType;
        return CHARGES[nodeType] || CHARGES._def_;
    }

    distance(node) {
        const nodeType = node.nodeType;
        return LINK_DISTANCE[nodeType] || LINK_DISTANCE._def_;
    }

    strength(node) {
        const nodeType = node.nodeType;
        return LINK_STRENGTH[nodeType] || LINK_STRENGTH._def_;
    }

    initSimulation(options: Options) {
        if (!options || !options.width || !options.height) {
            throw new Error('missing options when initializing simulation');
        }

        /** Creating the simulation */
        if (!this.simulation) {
            const ticker = this.ticker;

            // Creating the force simulation and defining the charges
            this.simulation = d3.forceSimulation()
                .force('charge',
                    d3.forceManyBody().strength(this.charges.bind(this)))
                        // .distanceMin(100).distanceMax(500))
                .force('gravity',
                    d3.forceManyBody().strength(FORCES.GRAVITY))
                .force('friction',
                    d3.forceManyBody().strength(FORCES.FRICTION));

            // Connecting the d3 ticker to an angular event emitter
            this.simulation.on('tick', function () {
                ticker.emit(this);
            });

            this.initNodes();
            // this.initLinks();
        }

        /** Updating the central force of the simulation */
        this.simulation.force('centers', d3.forceCenter(options.width / 2, options.height / 2));

        /** Restarting the simulation internal timer */
        this.simulation.restart();
    }

    stopSimulation() {
        this.simulation.stop();
        this.log.debug('Simulation stopped');
    }

    restartSimulation() {
        this.simulation.restart();
        this.log.debug('Simulation restarted');
    }
}
