/*
 * Copyright 2015-present Open Networking Foundation
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
import { of } from 'rxjs';

import { ConsoleLoggerService } from '../../../consolelogger.service';
import { FnService } from '../../../fw/util/fn.service';
import { IconComponent } from '../../svg/icon/icon.component';
import { IconService } from '../../svg/icon.service';
import { LionService } from '../../util/lion.service';
import { LogService } from '../../../log.service';
import { NavComponent } from './nav.component';
import { NavService } from '../nav.service';

class MockActivatedRoute extends ActivatedRoute {
    constructor(params: Params) {
        super();
        this.queryParams = of(params);
    }
}

class MockNavService {}

class MockIconService {
    loadIconDef() {}
}

/**
 * ONOS GUI -- Util -- Navigation Component - Unit Tests
 */
describe('NavComponent', () => {
    let log: LogService;
    let fs: FnService;
    let ar: MockActivatedRoute;
    let windowMock: Window;
    let component: NavComponent;
    let fixture: ComponentFixture<NavComponent>;
    const bundleObj = {
        'core.view.App': {
            test: 'test1'
        }
    };
    const mockLion = (key) =>  {
        return bundleObj[key] || '%' + key + '%';
    };

    beforeEach(async(() => {
        log = new ConsoleLoggerService();
        ar = new MockActivatedRoute({'debug': 'txrx'});

        windowMock = <any>{
            location: <any> {
                hostname: 'foo',
                host: 'foo',
                port: '80',
                protocol: 'http',
                search: { debug: 'true'},
                href: 'ws://foo:123/onos/ui/websock/path',
                absUrl: 'ws://foo:123/onos/ui/websock/path'
            }
        };
        fs = new FnService(ar, log, windowMock);

        TestBed.configureTestingModule({
            imports: [ BrowserAnimationsModule ],
            declarations: [ NavComponent, IconComponent ],
            providers: [
                { provide: FnService, useValue: fs },
                { provide: IconService, useClass: MockIconService },
                { provide: LionService, useFactory: (() => {
                        return {
                            bundle: ((bundleId) => mockLion),
                            ubercache: new Array(),
                            loadCbs: new Map<string, () => void>([])
                        };
                    })
                },
                { provide: LogService, useValue: log },
                { provide: NavService, useClass: MockNavService },
                { provide: 'Window', useValue: windowMock },
            ]
        })
        .compileComponents();
    }));

    beforeEach(() => {
        fixture = TestBed.createComponent(NavComponent);
        component = fixture.debugElement.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });

    it('should have a nav#nav', () => {
        const appDe: DebugElement = fixture.debugElement;
        const divDe = appDe.query(By.css('nav#nav'));
        expect(divDe).toBeTruthy();
    });

    it('should have a platform div.nav-hdr inside a nav#nav', () => {
        const appDe: DebugElement = fixture.debugElement;
        const divDe = appDe.query(By.css('nav#nav div.nav-hdr'));
        const div: HTMLElement = divDe.nativeElement;
        expect(div.textContent).toEqual('%cat_platform%');
    });

    it('should have an apps link inside a nav#nav', () => {
        const appDe: DebugElement = fixture.debugElement;
        const divDe = appDe.query(By.css('nav#nav a'));
        const div: HTMLElement = divDe.nativeElement;
        expect(div.textContent).toEqual('Apps');
    });
});
