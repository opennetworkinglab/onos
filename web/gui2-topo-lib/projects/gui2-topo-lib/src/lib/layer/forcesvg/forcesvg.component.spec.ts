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

import { ForceSvgComponent } from './forcesvg.component';
import {FnService, LogService} from 'gui2-fw-lib';
import {DraggableDirective} from './draggable/draggable.directive';
import {ActivatedRoute, Params} from '@angular/router';
import {of} from 'rxjs';
import {MapSvgComponent} from '../mapsvg/mapsvg.component';
import {DeviceNodeSvgComponent} from './visuals/devicenodesvg/devicenodesvg.component';
import {SubRegionNodeSvgComponent} from './visuals/subregionnodesvg/subregionnodesvg.component';
import {HostNodeSvgComponent} from './visuals/hostnodesvg/hostnodesvg.component';
import {LinkSvgComponent} from './visuals/linksvg/linksvg.component';

class MockActivatedRoute extends ActivatedRoute {
    constructor(params: Params) {
        super();
        this.queryParams = of(params);
    }
}

describe('ForceSvgComponent', () => {
    let fs: FnService;
    let ar: MockActivatedRoute;
    let windowMock: Window;
    let logServiceSpy: jasmine.SpyObj<LogService>;
    let component: ForceSvgComponent;
    let fixture: ComponentFixture<ForceSvgComponent>;

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
            declarations: [
                ForceSvgComponent,
                DeviceNodeSvgComponent,
                HostNodeSvgComponent,
                SubRegionNodeSvgComponent,
                LinkSvgComponent,
                DraggableDirective,
                MapSvgComponent
            ],
            providers: [
                { provide: FnService, useValue: fs },
                { provide: LogService, useValue: logSpy },
                { provide: 'Window', useValue: windowMock },
            ]
        })
        .compileComponents();
        logServiceSpy = TestBed.get(LogService);
    }));

    beforeEach(() => {
        fixture = TestBed.createComponent(ForceSvgComponent);
        component = fixture.debugElement.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});
