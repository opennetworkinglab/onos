/*
 * Copyright 2018-present Open Networking Foundation
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
import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, Params } from '@angular/router';
import { of } from 'rxjs';
import { HttpClient } from '@angular/common/http';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

import { TopologyComponent } from './topology.component';
import {
    Instance,
    InstanceComponent
} from '../panel/instance/instance.component';
import { SummaryComponent } from '../panel/summary/summary.component';
import { ToolbarComponent } from '../panel/toolbar/toolbar.component';
import { DetailsComponent } from '../panel/details/details.component';
import { TopologyService } from '../topology.service';

import {
    FlashComponent,
    FnService,
    LogService
} from 'gui2-fw-lib';


class MockActivatedRoute extends ActivatedRoute {
    constructor(params: Params) {
        super();
        this.queryParams = of(params);
    }
}

class MockHttpClient {}

class MockTopologyService {
    init(instance: InstanceComponent) {
        instance.onosInstances = [
            <Instance>{
                'id': 'inst1',
                'ip': '127.0.0.1',
                'reachable': true,
                'online': true,
                'ready': true,
                'switches': 4,
                'uiAttached': true
            },
            <Instance>{
                'id': 'inst1',
                'ip': '127.0.0.2',
                'reachable': true,
                'online': true,
                'ready': true,
                'switches': 3,
                'uiAttached': false
            }
        ];
    }
    destroy() {}
}

/**
 * ONOS GUI -- Topology View -- Unit Tests
 */
describe('TopologyComponent', () => {
    let fs: FnService;
    let ar: MockActivatedRoute;
    let windowMock: Window;
    let logServiceSpy: jasmine.SpyObj<LogService>;
    let component: TopologyComponent;
    let fixture: ComponentFixture<TopologyComponent>;

    beforeEach(async(() => {
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
        fs = new FnService(ar, logSpy, windowMock);

        TestBed.configureTestingModule({
            imports: [ BrowserAnimationsModule ],
            declarations: [
                TopologyComponent,
                InstanceComponent,
                SummaryComponent,
                ToolbarComponent,
                DetailsComponent,
                FlashComponent
            ],
            providers: [
                { provide: FnService, useValue: fs },
                { provide: LogService, useValue: logSpy },
                { provide: 'Window', useValue: windowMock },
                { provide: HttpClient, useClass: MockHttpClient },
                { provide: TopologyService, useClass: MockTopologyService }
            ]
        }).compileComponents();
        logServiceSpy = TestBed.get(LogService);
    }));

    beforeEach(() => {
        fixture = TestBed.createComponent(TopologyComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});
