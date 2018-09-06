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
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { DebugElement } from '@angular/core';
import { By } from '@angular/platform-browser';
import {
    FnService,
    IconService,
    LogService,
    UrlFnService,
    WebSocketService,
    IconComponent
} from 'gui2-fw-lib';
import { of } from 'rxjs';
import { } from 'jasmine';

import { HostDetailsComponent } from './hostdetails.component';

class MockActivatedRoute extends ActivatedRoute {
    constructor(params: Params) {
        super();
        this.queryParams = of(params);
    }
}

class MockFnService { }

class MockIconService {
    loadIconDef() { }
}

class MockUrlFnService { }

class MockWebSocketService {
    createWebSocket() { }
    isConnected() { return false; }
    unbindHandlers() { }
    bindHandlers() { }
}

/**
 * ONOS GUI -- Host Detail Panel View -- Unit Tests
 */

describe('HostdetailsComponent', () => {
    let fs: FnService;
    let ar: MockActivatedRoute;
    let windowMock: Window;
    let logServiceSpy: jasmine.SpyObj<LogService>;
    let component: HostDetailsComponent;
    let fixture: ComponentFixture<HostDetailsComponent>;

    const bundleObj = {
        'core.view.Hosts': {
        }
    };

    const mockLion = (key) => {
        return bundleObj[key] || '%' + key + '%';
    };

    beforeEach(async(() => {

        const logSpy = jasmine.createSpyObj('LogService', ['info', 'debug', 'warn', 'error']);
        ar = new MockActivatedRoute({ 'debug': 'panel' });

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
            imports: [BrowserAnimationsModule],
            declarations: [HostDetailsComponent, IconComponent],
            providers: [
                { provide: FnService, useValue: fs },
                { provide: IconService, useClass: MockIconService },
                { provide: LogService, useValue: logSpy },
                { provide: UrlFnService, useClass: MockUrlFnService },
                { provide: WebSocketService, useClass: MockWebSocketService },
                { provide: 'Window', useValue: windowMock },
            ]
        })
            .compileComponents();
        logServiceSpy = TestBed.get(LogService);
    }));

    beforeEach(() => {
        fixture = TestBed.createComponent(HostDetailsComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });

    it('should have an onos-icon.close-btn inside a div.top inside a div.container', () => {
        const hostDe: DebugElement = fixture.debugElement;
        const divDe = hostDe.query(By.css('div.container div.top onos-icon.close-btn'));
        expect(divDe).toBeTruthy();
    });

    it('should have a div.host-icon inside a div.container', () => {
        const hostDe: DebugElement = fixture.debugElement;
        const divDe = hostDe.query(By.css('div.container div.host-icon'));
        expect(divDe).toBeTruthy();
    });

    it('should have a h2 inside the div.container', () => {
        const hostDe: DebugElement = fixture.debugElement;
        const divDe = hostDe.query(By.css('div#host-details-panel div.container h2'));
        const div: HTMLElement = divDe.nativeElement;
        expect(div.textContent).toEqual('');
    });

    it('should have a div.top-content inside a div.container', () => {
        const hostDe: DebugElement = fixture.debugElement;
        const divDe = hostDe.query(By.css('div.container div.top-content'));
        expect(divDe).toBeTruthy();
    });

    it('should have a div.top-tables inside a div.top-content inside a div.container', () => {
        const hostDe: DebugElement = fixture.debugElement;
        const divDe = hostDe.query(By.css('div.container div.top-content div.top-tables'));
        expect(divDe).toBeTruthy();
    });

    it('should have a div.left inside a div.top-tables inside a div.top-content inside a div.container', () => {
        const hostDe: DebugElement = fixture.debugElement;
        const divDe = hostDe.query(By.css('div.container div.top-content div.top-tables div.left'));
        expect(divDe).toBeTruthy();
    });

    it('should have a div.right inside a div.top-tables inside a div.top-content inside a div.container', () => {
        const hostDe: DebugElement = fixture.debugElement;
        const divDe = hostDe.query(By.css('div.container div.top-content div.top-tables div.right'));
        expect(divDe).toBeTruthy();
    });

    it('should have a div.bottom inside a div.container', () => {
        const hostDe: DebugElement = fixture.debugElement;
        const divDe = hostDe.query(By.css('div.container div.bottom'));
        expect(divDe).toBeTruthy();
    });
});
