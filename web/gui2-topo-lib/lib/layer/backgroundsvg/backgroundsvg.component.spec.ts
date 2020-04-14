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
import { async, ComponentFixture, TestBed, getTestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { BackgroundSvgComponent } from './backgroundsvg.component';
import {MapSvgComponent, TopoData} from '../mapsvg/mapsvg.component';
import {from} from 'rxjs';
import {HttpClient} from '@angular/common/http';
import {LocMeta, LogService, ZoomUtils} from 'org_onosproject_onos/web/gui2-fw-lib/public_api';
import {MapObject} from '../maputils';
import {ForceSvgComponent} from '../forcesvg/forcesvg.component';

import {DraggableDirective} from '../forcesvg/draggable/draggable.directive';
import {DeviceNodeSvgComponent} from '../forcesvg/visuals/devicenodesvg/devicenodesvg.component';
import {SubRegionNodeSvgComponent} from '../forcesvg/visuals/subregionnodesvg/subregionnodesvg.component';
import {LinkSvgComponent} from '../forcesvg/visuals/linksvg/linksvg.component';
import {HostNodeSvgComponent} from '../forcesvg/visuals/hostnodesvg/hostnodesvg.component';
import {BadgeSvgComponent} from '../forcesvg/visuals/badgesvg/badgesvg.component';


describe('BackgroundSvgComponent', () => {
    let httpMock: HttpTestingController;

    let logServiceSpy: jasmine.SpyObj<LogService>;
    let component: BackgroundSvgComponent;
    let fixture: ComponentFixture<BackgroundSvgComponent>;

    const testmap: MapObject = <MapObject>{
        scale: 1.0,
        id: 'bayareaGEO',
        description: 'test map',
        filePath: 'testmap'
    };

    const sampleTopoData = <TopoData>require('../mapsvg/tests/bayarea.json');

    beforeEach(() => {
        const logSpy = jasmine.createSpyObj('LogService', ['info', 'debug', 'warn', 'error']);


        TestBed.configureTestingModule({
            imports: [HttpClientTestingModule],
            declarations: [
                BackgroundSvgComponent,
                MapSvgComponent,
                ForceSvgComponent,
                DeviceNodeSvgComponent,
                HostNodeSvgComponent,
                SubRegionNodeSvgComponent,
                LinkSvgComponent,
                DraggableDirective,
                BadgeSvgComponent
            ],
            providers: [
                { provide: LogService, useValue: logSpy },
            ]
        })
        .compileComponents();

        logServiceSpy = TestBed.get(LogService);
        httpMock = TestBed.get(HttpTestingController);
        fixture = TestBed.createComponent(BackgroundSvgComponent);

        component = fixture.componentInstance;
        component.map = testmap;
        fixture.detectChanges();
    });

    it('should create', () => {
        httpMock.expectOne('testmap.topojson').flush(sampleTopoData);

        expect(component).toBeTruthy();

        httpMock.verify();
    });

    it('should convert latlong to xy', () => {
        const result = ZoomUtils.convertGeoToCanvas(<LocMeta>{lat: 52, lng: -8});
        expect(Math.round(result.x * 100)).toEqual(45556);
        expect(Math.round(result.y * 100)).toEqual(15333);
    });
});
