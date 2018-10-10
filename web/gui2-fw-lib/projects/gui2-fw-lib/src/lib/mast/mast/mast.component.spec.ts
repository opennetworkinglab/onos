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
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { RouterTestingModule } from '@angular/router/testing';
import { DebugElement } from '@angular/core';
import { By } from '@angular/platform-browser';

import { LogService } from '../../log.service';
import { ConsoleLoggerService } from '../../consolelogger.service';
import { MastComponent } from './mast.component';
import { IconComponent } from '../../svg/icon/icon.component';
import { ConfirmComponent } from '../../layer/confirm/confirm.component';
import { LionService } from '../../util/lion.service';
import { IconService } from '../../svg/icon.service';
import { NavService } from '../../nav/nav.service';
import { WebSocketService } from '../../remote/websocket.service';

class MockNavService {}

class MockIconService {
    loadIconDef() {}
}

class MockWebSocketService {
    createWebSocket() {}
    isConnected() { return false; }
    unbindHandlers() {}
    bindHandlers() {}
}

/**
 * ONOS GUI -- Masthead Controller - Unit Tests
 */
describe('MastComponent', () => {
    let log: LogService;
    let windowMock: Window;
    let component: MastComponent;
    let fixture: ComponentFixture<MastComponent>;
    const bundleObj = {
        'core.view.App': {
            test: 'test1',
            tt_help: 'Help!'
        }
    };
    const mockLion = (key) =>  {
        return bundleObj[key] || '%' + key + '%';
    };

    beforeEach(async(() => {
        log = new ConsoleLoggerService();
        windowMock = <any>{
            location: <any> {
                hostname: 'foo',
                pathname: 'apps',
                host: 'foo',
                port: '80',
                protocol: 'http',
                search: { debug: 'true'},
                href: 'ws://foo:123/onos/ui/websock/path',
                absUrl: 'ws://foo:123/onos/ui/websock/path'
            }
        };

        TestBed.configureTestingModule({
            imports: [ BrowserAnimationsModule, RouterTestingModule ],
            declarations: [ MastComponent, IconComponent, ConfirmComponent ],
            providers: [
                { provide: LogService, useValue: log },
                { provide: NavService, useClass: MockNavService },
                { provide: LionService, useFactory: (() => {
                        return {
                            bundle: ((bundleId) => mockLion),
                            ubercache: new Array(),
                            loadCbs: new Map<string, () => void>([])
                        };
                    })
                },
                { provide: IconService, useClass: MockIconService },
                { provide: WebSocketService, useClass: MockWebSocketService },
                { provide: 'Window', useValue: windowMock }
            ]
        })
        .compileComponents();
    }));

    beforeEach(() => {
        fixture = TestBed.createComponent(MastComponent);
        component = fixture.debugElement.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });

    it('should have a div#mast-top-block', () => {
        const appDe: DebugElement = fixture.debugElement;
        const divDe = appDe.query(By.css('div#mast-top-block'));
        expect(divDe).toBeTruthy();
    });

    it('should have a span.nav-menu-button inside a div#mast', () => {
        const appDe: DebugElement = fixture.debugElement;
        const divDe = appDe.query(By.css('div#mast span.nav-menu-button'));
        expect(divDe).toBeTruthy();
    });

    it('should have a div.dropdown-parent inside a div#mast-right inside a div#mast', () => {
        const appDe: DebugElement = fixture.debugElement;
        const divDe = appDe.query(By.css('div#mast div#mast-right div.dropdown-parent'));
        const div: HTMLElement = divDe.nativeElement;
        expect(div.textContent).toEqual('  %logout% ');
    });

    it('should have an onos-icon inside a div#mast-right inside a div#mast', () => {
        const appDe: DebugElement = fixture.debugElement;
        const divDe = appDe.query(By.css('div#mast div#mast-right div.ctrl-btns div.active onos-icon'));
        expect(divDe).toBeTruthy();
    });

});
