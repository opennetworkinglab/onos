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

import { GroupComponent } from './group.component';
import {
    ConsoleLoggerService,
    FnService,
    IconService,
    IconComponent,
    LogService,
    TableFilterPipe,
    ThemeService,
    UrlFnService,
    WebSocketService, LoadingComponent
} from 'gui2-fw-lib';
import { ActivatedRoute, Params } from '@angular/router';
import { of } from 'rxjs';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { FormsModule } from '@angular/forms';
import { RouterTestingModule } from '@angular/router/testing';
import { DebugElement } from '@angular/core';
import { By } from '@angular/platform-browser';

class MockActivatedRoute extends ActivatedRoute {
    constructor(params: Params) {
        super();
        this.queryParams = of(params);
    }
}

class MockIconService {
    loadIconDef() { }
}

class MockThemeService { }

class MockUrlFnService { }

class MockWebSocketService {
    createWebSocket() { }
    isConnected() { return false; }
    unbindHandlers() { }
    bindHandlers() { }
}

/**
 * ONOS GUI -- Group View Module - Unit Tests
 */
describe('GroupComponent', () => {
    let component: GroupComponent;
    let fixture: ComponentFixture<GroupComponent>;
    let log: LogService;
    let fs: FnService;
    let ar: MockActivatedRoute;
    let windowMock: Window;
    const bundleObj = {
        'core.view.Group': {
            test: 'test1',
            tt_help: 'Help!'
        }
    };
    const mockLion = (key) => {
        return bundleObj[key] || '%' + key + '%';
    };

    beforeEach(async(() => {
        log = new ConsoleLoggerService();
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
        fs = new FnService(ar, log, windowMock);

        TestBed.configureTestingModule({
            imports: [BrowserAnimationsModule, FormsModule, RouterTestingModule],
            declarations: [
                GroupComponent,
                IconComponent,
                TableFilterPipe,
                LoadingComponent
            ],
            providers: [
                { provide: FnService, useValue: fs },
                { provide: IconService, useClass: MockIconService },
                { provide: LogService, useValue: log },
                { provide: ThemeService, useClass: MockThemeService },
                { provide: UrlFnService, useClass: MockUrlFnService },
                { provide: WebSocketService, useClass: MockWebSocketService },
                { provide: 'Window', useValue: windowMock },
            ]
        })
            .compileComponents();
    }));

    beforeEach(() => {
        fixture = TestBed.createComponent(GroupComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });

    it('should have a div.tabular-header inside a div#ov-group', () => {
        const groupDe: DebugElement = fixture.debugElement;
        const divDe = groupDe.query(By.css('div#ov-group div.tabular-header'));
        expect(divDe).toBeTruthy();
    });

    it('should have a h2 inside the div.tabular-header', () => {
        const groupDe: DebugElement = fixture.debugElement;
        const divDe = groupDe.query(By.css('div#ov-group div.tabular-header h2'));
        const div: HTMLElement = divDe.nativeElement;
        expect(div.textContent).toEqual(' Groups for Device  (0 total) ');
    });

    it('should have a refresh button inside the div.tabular-header', () => {
        const groupDe: DebugElement = fixture.debugElement;
        const divDe = groupDe.query(By.css('div#ov-group div.tabular-header div.ctrl-btns div.refresh'));
        expect(divDe).toBeTruthy();
    });

    it('should have a div.summary-list inside a div#ov-group', () => {
        const groupDe: DebugElement = fixture.debugElement;
        const divDe = groupDe.query(By.css('div#ov-group div.summary-list'));
        expect(divDe).toBeTruthy();
    });

    it('should have a div.table-header inside a div.summary-list inside a div#ov-group', () => {
        const groupDe: DebugElement = fixture.debugElement;
        const divDe = groupDe.query(By.css('div#ov-group div.summary-list div.table-header'));
        expect(divDe).toBeTruthy();
    });

    it('should have a div.table-body inside a div.summary-list inside a div#ov-group', () => {
        const groupDe: DebugElement = fixture.debugElement;
        const divDe = groupDe.query(By.css('div#ov-group div.summary-list div.table-body'));
        expect(divDe).toBeTruthy();
    });
});
