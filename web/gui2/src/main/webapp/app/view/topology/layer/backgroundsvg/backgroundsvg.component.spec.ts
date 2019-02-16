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

import { BackgroundSvgComponent } from './backgroundsvg.component';
import {MapSvgComponent} from '../mapsvg/mapsvg.component';
import {from} from 'rxjs';
import {HttpClient} from '@angular/common/http';
import {LogService} from 'gui2-fw-lib';
import {MapObject} from '../maputils';
import {LocMeta} from '../forcesvg/models';
import {ForceSvgComponent} from '../forcesvg/forcesvg.component';
import {
    DeviceNodeSvgComponent,
    HostNodeSvgComponent, LinkSvgComponent, SubRegionNodeSvgComponent
} from '../forcesvg/visuals';
import {DraggableDirective} from '../forcesvg/draggable/draggable.directive';

class MockHttpClient {
    get() {
        return from(['{"id":"app","icon":"nav_apps","cat":"PLATFORM","label":"Applications"}']);
    }

    subscribe() {}
}

describe('BackgroundSvgComponent', () => {
    let logServiceSpy: jasmine.SpyObj<LogService>;
    let component: BackgroundSvgComponent;
    let fixture: ComponentFixture<BackgroundSvgComponent>;
    const testmap: MapObject = <MapObject>{
        scale: 1.0,
        id: 'test',
        description: 'test map'
    };

    beforeEach(async(() => {
        const logSpy = jasmine.createSpyObj('LogService', ['info', 'debug', 'warn', 'error']);

        TestBed.configureTestingModule({
            declarations: [
                BackgroundSvgComponent,
                MapSvgComponent,
                ForceSvgComponent,
                DeviceNodeSvgComponent,
                HostNodeSvgComponent,
                SubRegionNodeSvgComponent,
                LinkSvgComponent,
                DraggableDirective
            ],
            providers: [
                { provide: LogService, useValue: logSpy },
                { provide: HttpClient, useClass: MockHttpClient },
            ]
        })
        .compileComponents();

        logServiceSpy = TestBed.get(LogService);
    }));

    beforeEach(() => {
        fixture = TestBed.createComponent(BackgroundSvgComponent);
        component = fixture.componentInstance;
        component.map = testmap;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });

    it('should convert latlong to xy', () => {
        const result = BackgroundSvgComponent.convertGeoToCanvas(<LocMeta>{lat: 52, lng: -8});
        expect(Math.round(result.x * 100)).toEqual(45556);
        expect(Math.round(result.y * 100)).toEqual(15333);
    });

    /**
     * For some reason including the following causes "ForceSvgComponent should create error
     * TODO: Investigate
     */

    // it('should convert xy random extents to latlong', () => {
    //     const result = BackgroundSvgComponent.convertXYtoGeo(455.556, 153.33);
    //     expect(Math.round(result.equivLoc.lng)).toEqual(-8);
    //     expect(Math.round(result.equivLoc.lat)).toEqual(52);
    // });

    // it('should convert xy min extents to latlong', () => {
    //     const result = BackgroundSvgComponent.convertXYtoGeo(-500, 0);
    //     expect(Math.round(result.equivLoc.lng)).toEqual(-180);
    //     expect(Math.round(result.equivLoc.lat)).toEqual(75);
    // });

    // it('should convert xy full extents to latlong', () => {
    //     const result = BackgroundSvgComponent.convertXYtoGeo(1500, 1000);
    //     expect(Math.round(result.equivLoc.lng)).toEqual(180);
    //     expect(Math.round(result.equivLoc.lat)).toEqual(-75);
    // });
});
