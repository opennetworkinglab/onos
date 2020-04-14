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
import {ForceDirectedGraph, Options} from './force-directed-graph';
import {Node} from './node';
import {Link} from './link';
import {LogService} from 'org_onosproject_onos/web/gui2-fw-lib/public_api';
import {TestBed} from '@angular/core/testing';

export class TestNode extends Node {
    constructor(id: string) {
        super(id);
    }
}

export class TestLink extends Link {
    constructor(source: Node, target: Node) {
        super(source, target);
    }
}

/**
 * ONOS GUI -- ForceDirectedGraph - Unit Tests
 */
describe('ForceDirectedGraph', () => {
    let logServiceSpy: jasmine.SpyObj<LogService>;
    let fdg: ForceDirectedGraph;
    const options: Options = {width: 1000, height: 1000};

    beforeEach(() => {
        const logSpy = jasmine.createSpyObj('LogService', ['info', 'debug', 'warn', 'error']);
        const nodes: Node[] = [];
        const links: Link[] = [];
        fdg = new ForceDirectedGraph(options, logSpy);

        for (let i = 0; i < 10; i++) {
            const newNode: TestNode = new TestNode('id' + i);
            nodes.push(newNode);
        }
        for (let j = 1; j < 10; j++) {
            const newLink = new TestLink(nodes[0], nodes[j]);
            links.push(newLink);
        }
        fdg.nodes = nodes;
        fdg.links = links;
        fdg.reinitSimulation();
        logServiceSpy = TestBed.get(LogService);
    });

    afterEach(() => {
        fdg.stopSimulation();
        fdg.nodes = [];
        fdg.links = [];
        fdg.reinitSimulation();
    });

    it('should be created', () => {
        expect(fdg).toBeTruthy();
    });

    it('should have simulation', () => {
        expect(fdg.simulation).toBeTruthy();
    });

    it('should have 10 nodes', () => {
        expect(fdg.nodes.length).toEqual(10);
    });

    it('should have 10 links', () => {
        expect(fdg.links.length).toEqual(9);
    });

    // TODO fix these up to listen for tick
    // it('nodes should not be at zero', () => {
    //     expect(nodes[0].x).toBeGreaterThan(0);
    // });
    // it('ticker should emit', () => {
    //     let tickMe = jasmine.createSpy("tickMe() spy");
    //     fdg.ticker.subscribe((simulation) => tickMe());
    //     expect(tickMe).toHaveBeenCalled();
    // });

    // it('init links chould be called ', () => {
    //     spyOn(fdg, 'initLinks');
    //     // expect(fdg).toBeTruthy();
    //     fdg.reinitSimulation(options);
    //     expect(fdg.initLinks).toHaveBeenCalled();
    // });
});
