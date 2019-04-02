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

import { PortComponent } from './port.component';
import { ActivatedRoute, Params } from '@angular/router';
import { of } from 'rxjs/index';
import {
    FnService,
    IconService,
    GlyphService,
    IconComponent,
    LogService,
    NavService,
    MastService,
    PrefsService,
    TableFilterPipe,
    ThemeService,
    WebSocketService, LoadingComponent
} from 'gui2-fw-lib';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { FormsModule } from '@angular/forms';
import { RouterTestingModule } from '@angular/router/testing';
import { DebugElement } from '@angular/core';
import { By } from '@angular/platform-browser';
import { PortDetailsComponent } from '../portdetails/portdetails.component';

class MockActivatedRoute extends ActivatedRoute {
    constructor(params: Params) {
        super();
        this.queryParams = of(params);
    }
}

class MockIconService {
    loadIconDef() { }
}

class MockPrefsService {
    setPrefs() { }
    getPrefs() { }
    asNumbers() { }
    updatePrefs() { }
}

class MockGlyphService { }

class MockNavService { }

class MockMastService { }

class MockThemeService { }

class MockWebSocketService {
    createWebSocket() { }
    isConnected() { return false; }
    unbindHandlers() { }
    bindHandlers() { }
    sendEvent() { }
}

/**
 * ONOS GUI -- Flow View Module - Unit Tests
 */


describe('PortComponent', () => {
    let fs: FnService;
    let ar: MockActivatedRoute;
    let windowMock: Window;
    let logServiceSpy: jasmine.SpyObj<LogService>;
    let component: PortComponent;
    let fixture: ComponentFixture<PortComponent>;

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
            imports: [BrowserAnimationsModule, FormsModule, RouterTestingModule],
            declarations: [
                PortComponent,
                IconComponent,
                TableFilterPipe,
                PortDetailsComponent,
                LoadingComponent
            ],
            providers: [
                { provide: FnService, useValue: fs },
                { provide: IconService, useClass: MockIconService },
                { provide: GlyphService, useClass: MockGlyphService },
                { provide: MastService, useClass: MockMastService },
                { provide: NavService, useClass: MockNavService },
                { provide: PrefsService, useClass: MockPrefsService },
                { provide: LogService, useValue: logSpy },
                { provide: ThemeService, useClass: MockThemeService },
                { provide: WebSocketService, useClass: MockWebSocketService },
                { provide: 'Window', useValue: windowMock },
            ]
        }).compileComponents();
        logServiceSpy = TestBed.get(LogService);
    }));

    beforeEach(() => {
        fixture = TestBed.createComponent(PortComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });

    it('should have a div.tabular-header inside a div#ov-port', () => {
        const portDe: DebugElement = fixture.debugElement;
        const divDe = portDe.query(By.css('div#ov-port div.tabular-header'));
        expect(divDe).toBeTruthy();
    });

    it('should have a h2 inside the div.tabular-header', () => {
        const portDe: DebugElement = fixture.debugElement;
        const divDe = portDe.query(By.css('div#ov-port div.tabular-header h2'));
        const div: HTMLElement = divDe.nativeElement;
        expect(div.textContent).toEqual(' Ports for Device  (0 Total) ');
    });

    it('should have .table-header with "Port ID..."', () => {
        const portDe: DebugElement = fixture.debugElement;
        const divDe = portDe.query(By.css('div#ov-port div.table-header'));
        const div: HTMLElement = divDe.nativeElement;
        expect(div.textContent).toEqual(
            'Port ID Pkts Received Pkts Sent Bytes Received Bytes Sent Pkts RX Dropped Pkts TX Dropped Duration (sec) ');
    });

    it('should have a refresh button inside the div.tabular-header', () => {
        const portDe: DebugElement = fixture.debugElement;
        const divDe = portDe.query(By.css('div#ov-port div.tabular-header div.ctrl-btns div.refresh'));
        expect(divDe).toBeTruthy();
    });

    it('should have a div.table-body ', () => {
        const portDe: DebugElement = fixture.debugElement;
        const divDe = portDe.query(By.css('div#ov-port  div.table-body'));
        expect(divDe).toBeTruthy();
    });
});
