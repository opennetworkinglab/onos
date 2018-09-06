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

import { FlowDetailsComponent } from './flowdetails.component';
import { ActivatedRoute, Params } from '@angular/router';
import { of } from 'rxjs';
import {
    FnService,
    IconService,
    LogService,
    WebSocketService,
    IconComponent
} from 'gui2-fw-lib';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { DebugElement } from '@angular/core';
import { By } from '@angular/platform-browser';

class MockActivatedRoute extends ActivatedRoute {
    constructor(params: Params) {
        super();
        this.queryParams = of(params);
    }
}

class MockIconService {
    classes = 'active-close';
    loadIconDef() { }
}

class MockWebSocketService {
    createWebSocket() { }
    isConnected() { return false; }
    unbindHandlers() { }
    bindHandlers() { }
}

describe('FlowDetailsComponent', () => {
    let fs: FnService;
    let ar: MockActivatedRoute;
    let windowMock: Window;
    let logServiceSpy: jasmine.SpyObj<LogService>;
    let component: FlowDetailsComponent;
    let fixture: ComponentFixture<FlowDetailsComponent>;

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
            declarations: [FlowDetailsComponent, IconComponent],
            providers: [
                { provide: FnService, useValue: fs },
                { provide: IconService, useClass: MockIconService },
                { provide: LogService, useValue: logSpy },
                { provide: WebSocketService, useClass: MockWebSocketService },
                { provide: 'Window', useValue: windowMock },
            ]
        })
            .compileComponents();
        logServiceSpy = TestBed.get(LogService);
    }));

    beforeEach(() => {
        fixture = TestBed.createComponent(FlowDetailsComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });

    it('should have an div.close-btn div.top inside a div.container', () => {
        const flowDe: DebugElement = fixture.debugElement;
        const divDe = flowDe.query(By.css('div.container div.top div.close-btn'));
        expect(divDe).toBeTruthy();
    });

    it('should have a div.flow-icon inside a div.top inside a div.container', () => {
        const flowDe: DebugElement = fixture.debugElement;
        const divDe = flowDe.query(By.css('div.container div.top div.flow-icon'));
        const div: HTMLElement = divDe.nativeElement;
        expect(div.textContent).toEqual('');
    });

    it('should have a div.top-content inside a div.top inside a div.container', () => {
        const flowDe: DebugElement = fixture.debugElement;
        const divDe = flowDe.query(By.css('div.container div.top div.top-content'));
        expect(divDe).toBeTruthy();
    });

    it('should have a div.scroll inside a div.container', () => {
        const flowDe: DebugElement = fixture.debugElement;
        const divDe = flowDe.query(By.css('div.container div.scroll'));
        expect(divDe).toBeTruthy();
    });

    it('should have a h2 inside a div.top inside a div.container', () => {
        const flowDe: DebugElement = fixture.debugElement;
        const divDe = flowDe.query(By.css('div.container div.top h2'));
        expect(divDe).toBeTruthy();
    });

    it('should have a h3 inside a div.scroll inside a div.top inside a div.container', () => {
        const flowDe: DebugElement = fixture.debugElement;
        const divDe = flowDe.query(By.css('div.container div.top div.scroll h3'));
        expect(divDe).toBeTruthy();
    });

    it('should have a hr inside a div.scroll inside a div.top inside a div.container', () => {
        const flowDe: DebugElement = fixture.debugElement;
        const divDe = flowDe.query(By.css('div.container div.top div.scroll hr'));
        expect(divDe).toBeTruthy();
    });
});
