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

import { MapSvgComponent } from './mapsvg.component';
import {HttpClient} from '@angular/common/http';
import {from} from 'rxjs';
import {LogService} from 'org_onosproject_onos/web/gui2-fw-lib/public_api';

class MockHttpClient {
    get() {
        return from(['{"id":"app","icon":"nav_apps","cat":"PLATFORM","label":"Applications"}']);
    }

    subscribe() {}
}

describe('MapSvgComponent', () => {
    let logServiceSpy: jasmine.SpyObj<LogService>;
    let component: MapSvgComponent;
    let fixture: ComponentFixture<MapSvgComponent>;

    beforeEach(async(() => {
        const logSpy = jasmine.createSpyObj('LogService', ['info', 'debug', 'warn', 'error']);

        TestBed.configureTestingModule({
            declarations: [ MapSvgComponent ],
            providers: [
                { provide: LogService, useValue: logSpy },
                { provide: HttpClient, useClass: MockHttpClient },
            ]
        })
        .compileComponents();

        logServiceSpy = TestBed.get(LogService);
    }));

    beforeEach(() => {
        fixture = TestBed.createComponent(MapSvgComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});
