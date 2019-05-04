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
import {async, ComponentFixture, TestBed} from '@angular/core/testing';

import {ForceSvgComponent} from './forcesvg.component';
import {
    FnService, IconService,
    LionService,
    LogService, SvgUtilService,
    UrlFnService,
    WebSocketService
} from 'gui2-fw-lib';
import {DraggableDirective} from './draggable/draggable.directive';
import {ActivatedRoute, Params} from '@angular/router';
import {of} from 'rxjs';
import {DeviceNodeSvgComponent} from './visuals/devicenodesvg/devicenodesvg.component';
import {SubRegionNodeSvgComponent} from './visuals/subregionnodesvg/subregionnodesvg.component';
import {HostNodeSvgComponent} from './visuals/hostnodesvg/hostnodesvg.component';
import {LinkSvgComponent} from './visuals/linksvg/linksvg.component';
import {Device, Host, Link, LinkType, Region} from './models';
import {ChangeDetectorRef, SimpleChange} from '@angular/core';
import {TopologyService} from '../../topology.service';

class MockActivatedRoute extends ActivatedRoute {
    constructor(params: Params) {
        super();
        this.queryParams = of(params);
    }
}

class MockIconService {
    loadIconDef() { }
}

class MockSvgUtilService {

    cat7() {
        const tcid = 'd3utilTestCard';

        function getColor(id, muted, theme) {
            // NOTE: since we are lazily assigning domain ids, we need to
            //       get the color from all 4 scales, to keep the domains
            //       in sync.
            const ln = '#5b99d2';
            const lm = '#9ebedf';
            const dn = '#5b99d2';
            const dm = '#9ebedf';
            if (theme === 'dark') {
                return muted ? dm : dn;
            } else {
                return muted ? lm : ln;
            }
        }

        return {
            // testCard: testCard,
            getColor: getColor,
        };
    }
}

class MockUrlFnService { }

class MockWebSocketService {
    createWebSocket() { }
    isConnected() { return false; }
    unbindHandlers() { }
    bindHandlers() { }
}

class MockTopologyService {
    public instancesIndex: Map<string, number>;
    constructor() {
        this.instancesIndex = new Map();
    }
}

describe('ForceSvgComponent', () => {
    let fs: FnService;
    let ar: MockActivatedRoute;
    let windowMock: Window;
    let logServiceSpy: jasmine.SpyObj<LogService>;
    let component: ForceSvgComponent;
    let fixture: ComponentFixture<ForceSvgComponent>;
    const openflowSampleData = require('./tests/test-module-topo2CurrentRegion.json');
    const openflowRegionData: Region = <Region><unknown>(openflowSampleData.payload);

    const odtnSampleData = require('./tests/test-OdtnConfig-topo2CurrentRegion.json');
    const odtnRegionData: Region = <Region><unknown>(odtnSampleData.payload);

    const emptyRegion: Region = <Region>{devices: [ [], [], [] ], hosts: [ [], [], [] ], links: []};

    beforeEach(() => {
        const logSpy = jasmine.createSpyObj('LogService', ['info', 'debug', 'warn', 'error']);
        ar = new MockActivatedRoute({ 'debug': 'txrx' });

        windowMock = <any>{
            location: <any>{
                hostname: 'foo',
                host: 'foo',
                port: '80',
                protocol: 'http',
                search: { debug: 'true' },
                href: 'ws://foo:123/onos/ui/websock/path',
                absUrl: 'ws://foo:123/onos/ui/websock/path'
            }
        };

        const bundleObj = {
            'core.view.Topo': {
                test: 'test1'
            }
        };
        const mockLion = (key) => {
            return bundleObj[key] || '%' + key + '%';
        };

        fs = new FnService(ar, logSpy, windowMock);

        TestBed.configureTestingModule({
            declarations: [
                ForceSvgComponent,
                DeviceNodeSvgComponent,
                HostNodeSvgComponent,
                SubRegionNodeSvgComponent,
                LinkSvgComponent,
                DraggableDirective
            ],
            providers: [
                { provide: LogService, useValue: logSpy },
                { provide: ActivatedRoute, useValue: ar },
                { provide: FnService, useValue: fs },
                { provide: ChangeDetectorRef, useClass: ChangeDetectorRef },
                { provide: UrlFnService, useClass: MockUrlFnService },
                { provide: WebSocketService, useClass: MockWebSocketService },
                { provide: LionService, useFactory: (() => {
                        return {
                            bundle: ((bundleId) => mockLion),
                            ubercache: new Array(),
                            loadCbs: new Map<string, () => void>([])
                        };
                    })
                },
                { provide: IconService, useClass: MockIconService },
                { provide: SvgUtilService, useClass: MockSvgUtilService },
                { provide: TopologyService, useClass: MockTopologyService },
                { provide: 'Window', useValue: windowMock },
            ]
        })
        .compileComponents();
        logServiceSpy = TestBed.get(LogService);

        fixture = TestBed.createComponent(ForceSvgComponent);
        component = fixture.debugElement.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });

    it('load sample files', () => {
        expect(openflowSampleData).toBeTruthy();
        expect(openflowSampleData.payload).toBeTruthy();
        expect(openflowSampleData.payload.id).toBe('(root)');

        expect(odtnSampleData).toBeTruthy();
        expect(odtnSampleData.payload).toBeTruthy();
        expect(odtnSampleData.payload.id).toBe('(root)');
    });

    it('should read sample data payload as Region', () => {
        expect(openflowRegionData).toBeTruthy();
        // console.log(regionData);
        expect(openflowRegionData.id).toBe('(root)');
        expect(openflowRegionData.devices).toBeTruthy();
        expect(openflowRegionData.devices.length).toBe(3);
        expect(openflowRegionData.devices[2].length).toBe(10);
        expect(openflowRegionData.hosts.length).toBe(3);
        expect(openflowRegionData.hosts[2].length).toBe(20);
        expect(openflowRegionData.links.length).toBe(44);
    });

    it('should read device246 correctly', () => {
        const device246: Device = openflowRegionData.devices[2][0];
        expect(device246.id).toBe('of:0000000000000246');
        expect(device246.nodeType).toBe('device');
        expect(device246.type).toBe('switch');
        expect(device246.online).toBe(true);
        expect(device246.master).toBe('10.192.19.68');
        expect(device246.layer).toBe('def');

        expect(device246.props.managementAddress).toBe('10.192.19.69');
        expect(device246.props.protocol).toBe('OF_13');
        expect(device246.props.driver).toBe('ofdpa-ovs');
        expect(device246.props.latitude).toBe('40.15');
        expect(device246.props.name).toBe('s246');
        expect(device246.props.locType).toBe('geo');
        expect(device246.props.channelId).toBe('10.192.19.69:59980');
        expect(device246.props.longitude).toBe('-121.679');

        expect(device246.location.locType).toBe('geo');
        expect(device246.location.latOrY).toBe(40.15);
        expect(device246.location.longOrX).toBe(-121.679);
    });

    it('should read host 3 correctly', () => {
        const host3: Host = openflowRegionData.hosts[2][0];
        expect(host3.id).toBe('00:88:00:00:00:03/110');
        expect(host3.nodeType).toBe('host');
        expect(host3.layer).toBe('def');
        expect(host3.configured).toBe(false);
        expect(host3.ips.length).toBe(3);
        expect(host3.ips[0]).toBe('fe80::288:ff:fe00:3');
        expect(host3.ips[1]).toBe('2000::102');
        expect(host3.ips[2]).toBe('10.0.1.2');
    });

    it('should read link 3-205 correctly', () => {
        const link3_205: Link = openflowRegionData.links[0];
        expect(link3_205.id).toBe('00:AA:00:00:00:03/None~of:0000000000000205/6');
        expect(link3_205.epA).toBe('00:AA:00:00:00:03/None');
        expect(link3_205.epB).toBe('of:0000000000000205');
        expect(String(LinkType[link3_205.type])).toBe('2');
        expect(link3_205.portA).toBe(undefined);
        expect(link3_205.portB).toBe('6');

        expect(link3_205.rollup).toBeTruthy();
        expect(link3_205.rollup.length).toBe(1);
        expect(link3_205.rollup[0].id).toBe('00:AA:00:00:00:03/None~of:0000000000000205/6');
        expect(link3_205.rollup[0].epA).toBe('00:AA:00:00:00:03/None');
        expect(link3_205.rollup[0].epB).toBe('of:0000000000000205');
        expect(String(LinkType[link3_205.rollup[0].type])).toBe('2');
        expect(link3_205.rollup[0].portA).toBe(undefined);
        expect(link3_205.rollup[0].portB).toBe('6');

    });

    it('should handle regionData change - empty Region', () => {
        component.ngOnChanges(
            {'regionData' : new SimpleChange(<Region>{}, emptyRegion, true)});

        expect(component.graph.nodes.length).toBe(0);
    });

    it('should know how to format names', () => {
        expect(ForceSvgComponent.extractNodeName('00:AA:00:00:00:03/None', undefined))
            .toEqual('00:AA:00:00:00:03/None');

        expect(ForceSvgComponent.extractNodeName('00:AA:00:00:00:03/161', '161'))
            .toEqual('00:AA:00:00:00:03');

        // Like epB of first example in sampleData file - endPtStr contains port number
        expect(ForceSvgComponent.extractNodeName('of:0000000000000206/6', '6'))
            .toEqual('of:0000000000000206');

        // Like epB of second example in sampleData file - endPtStr does not contain port number
        expect(ForceSvgComponent.extractNodeName('of:0000000000000206', '6'))
            .toEqual('of:0000000000000206');
    });

    it('should handle openflow regionData change - sample Region', () => {
        component.regionData = openflowRegionData;
        component.ngOnChanges(
            {'regionData' : new SimpleChange(<Region>{}, openflowRegionData, true)});

        expect(component.graph.nodes.length).toBe(30);

        expect(component.graph.links.length).toBe(44);

    });

    it('should handle odtn regionData change - sample odtn Region', () => {
        component.regionData = odtnRegionData;
        component.ngOnChanges(
            {'regionData' : new SimpleChange(<Region>{}, odtnRegionData, true)});

        expect(component.graph.nodes.length).toBe(2);

        expect(component.graph.links.length).toBe(6);

    });
});
