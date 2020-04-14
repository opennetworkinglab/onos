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
import {LogService} from 'org_onosproject_onos/web/gui2-fw-lib/public_api';

const FORCES = {
    COLLISION: 1,
    GRAVITY: 0.4,
    FRICTION: 0.7
};

const CHARGES = {
    device: -800,
    host: -2000,
    region: -800,
    _def_: -1200
};

const LINK_DISTANCE = {
    // note: key is link.type
    direct: 100,
    optical: 120,
    UiEdgeLink: 3,
    UiDeviceLink: 100,
    _def_: 50,
};

/**
 * note: key is link.type
 * range: {0.0 ... 1.0}
 */
const LINK_STRENGTH = {
    _def_: 0.5
};

export interface Options {
    width: number;
    height: number;
}

/**
 * The inspiration for this approach comes from
 * https://medium.com/netscape/visualizing-data-with-angular-and-d3-209dde784aeb
 *
 * Do yourself a favour and read https://d3indepth.com/force-layout/
 */
export class ForceDirectedGraph {
    public ticker: EventEmitter<d3.Simulation<Node, Link>> = new EventEmitter();
    public simulation: d3.Simulation<any, any>;
    public canvasOptions: Options;
    public nodes: Node[] = [];
    public links: Link[] = [];

    constructor(options: Options, public log: LogService) {
        this.canvasOptions = options;
        const ticker = this.ticker;

        // Creating the force simulation and defining the charges
        this.simulation = d3.forceSimulation()
            .force('charge',
                d3.forceManyBody().strength(this.charges.bind(this)))
            // .distanceMin(100).distanceMax(500))
            .force('gravity',
                d3.forceManyBody().strength(FORCES.GRAVITY))
            .force('friction',
                d3.forceManyBody().strength(FORCES.FRICTION))
            .force('center',
                d3.forceCenter(this.canvasOptions.width / 2, this.canvasOptions.height / 2))
            .force('x', d3.forceX())
            .force('y', d3.forceY())
            .on('tick', () => {
                ticker.emit(this.simulation); // ForceSvgComponent.ngOnInit listens
            });

    }

    /**
     * Assigning updated node and restarting the simulation
     * Setting alpha to 0.3 and it will count down to alphaTarget=0
     */
    public reinitSimulation() {
        this.simulation.nodes(this.nodes);
        this.simulation.force('link',
            d3.forceLink(this.links)
                .strength(LINK_STRENGTH._def_)
                .distance(this.distance.bind(this))
        );
        this.simulation.alpha(0.3).restart();
    }

    charges(node: Node) {
        const nodeType = node.nodeType;
        return CHARGES[nodeType] || CHARGES._def_;
    }

    distance(link: Link) {
        const linkType = link.type;
        return LINK_DISTANCE[linkType] || LINK_DISTANCE._def_;
    }

    stopSimulation() {
        this.simulation.stop();
        this.log.debug('Simulation stopped');
    }

    public restartSimulation(alpha: number = 0.3) {
        this.simulation.alpha(alpha).restart();
        this.log.debug('Simulation restarted. Alpha:', alpha);
    }
}
