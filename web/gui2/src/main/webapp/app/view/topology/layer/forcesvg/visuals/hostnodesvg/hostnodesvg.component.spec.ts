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

import { HostNodeSvgComponent } from './hostnodesvg.component';
import {ActivatedRoute, Params} from '@angular/router';
import {of} from 'rxjs';
import {LogService} from 'gui2-fw-lib';
import {Host} from '../../models';
import {BrowserAnimationsModule} from '@angular/platform-browser/animations';
import {ChangeDetectorRef} from '@angular/core';

class MockActivatedRoute extends ActivatedRoute {
  constructor(params: Params) {
    super();
    this.queryParams = of(params);
  }
}

describe('HostNodeSvgComponent', () => {
    let logServiceSpy: jasmine.SpyObj<LogService>;
    let component: HostNodeSvgComponent;
    let fixture: ComponentFixture<HostNodeSvgComponent>;
    let ar: MockActivatedRoute;
    let testHost: Host;

    beforeEach(async(() => {
        const logSpy = jasmine.createSpyObj('LogService', ['info', 'debug', 'warn', 'error']);
        ar = new MockActivatedRoute({ 'debug': 'txrx' });
        testHost = new Host('host:1');
        testHost.ips = ['10.205.86.123', '192.168.56.10'];

        TestBed.configureTestingModule({
            imports: [ BrowserAnimationsModule ],
            declarations: [ HostNodeSvgComponent ],
            providers: [
              { provide: LogService, useValue: logSpy },
              { provide: ActivatedRoute, useValue: ar },
              { provide: ChangeDetectorRef, useClass: ChangeDetectorRef }
            ]
        })
        .compileComponents();
        logServiceSpy = TestBed.get(LogService);
    }));

    beforeEach(() => {
        fixture = TestBed.createComponent(HostNodeSvgComponent);
        component = fixture.componentInstance;
        component.host = testHost;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});
