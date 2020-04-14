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
import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { DeviceNodeSvgComponent } from './devicenodesvg.component';
import {FnService, IconService, LogService, SvgUtilService} from 'org_onosproject_onos/web/gui2-fw-lib/public_api';
import {ActivatedRoute, Params} from '@angular/router';
import {of} from 'rxjs';
import {ChangeDetectorRef} from '@angular/core';
import {Device} from '../../models';
import {BrowserAnimationsModule} from '@angular/platform-browser/animations';
import {TopologyService} from '../../../../topology.service';
import {BadgeSvgComponent} from '../badgesvg/badgesvg.component';

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

class MockTopologyService {
    public instancesIndex: Map<string, number>;
    constructor() {
        this.instancesIndex = new Map();
    }
}

describe('DeviceNodeSvgComponent', () => {
    let fs: FnService;
    let logServiceSpy: jasmine.SpyObj<LogService>;
    let component: DeviceNodeSvgComponent;
    let fixture: ComponentFixture<DeviceNodeSvgComponent>;
    let windowMock: Window;
    let ar: MockActivatedRoute;
    let testDevice: Device;


    beforeEach(async(() => {
        const logSpy = jasmine.createSpyObj('LogService', ['info', 'debug', 'warn', 'error']);
        ar = new MockActivatedRoute({ 'debug': 'txrx' });
        testDevice = new Device('test:1');
        testDevice.online = true;

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
        fs = new FnService(ar, logSpy, windowMock);

        TestBed.configureTestingModule({
            imports: [ BrowserAnimationsModule ],
            declarations: [ DeviceNodeSvgComponent, BadgeSvgComponent ],
            providers: [
                { provide: LogService, useValue: logSpy },
                { provide: ActivatedRoute, useValue: ar },
                { provide: ChangeDetectorRef, useClass: ChangeDetectorRef },
                { provide: IconService, useClass: MockIconService },
                { provide: SvgUtilService, useClass: MockSvgUtilService },
                { provide: TopologyService, useClass: MockTopologyService },
                { provide: 'Window', useValue: windowMock },
            ]
        })
        .compileComponents();
        logServiceSpy = TestBed.get(LogService);
    }));

    beforeEach(() => {
        fixture = TestBed.createComponent(DeviceNodeSvgComponent);
        component = fixture.componentInstance;
        component.device = testDevice;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});
