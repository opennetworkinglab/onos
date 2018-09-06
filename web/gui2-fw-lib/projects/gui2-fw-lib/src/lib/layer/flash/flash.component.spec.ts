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
import { DebugElement } from '@angular/core';
import { By } from '@angular/platform-browser';

import { ConsoleLoggerService } from '../../consolelogger.service';
import { LogService } from '../../log.service';
import { FlashComponent } from './flash.component';

/**
 * ONOS GUI -- Layer -- Flash Component - Unit Tests
 */
describe('FlashComponent', () => {
    let log: LogService;
    let component: FlashComponent;
    let fixture: ComponentFixture<FlashComponent>;

    beforeEach(async(() => {
        log = new ConsoleLoggerService();
        TestBed.configureTestingModule({
            imports: [ BrowserAnimationsModule ],
            declarations: [ FlashComponent ],
            providers: [
                { provide: LogService, useValue: log },
            ]
        });
    }));

    beforeEach(() => {
        fixture = TestBed.createComponent(FlashComponent);
        component = fixture.debugElement.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });

//    it('should have a div#flash', () => {
//        component.enabled = true;
//        const appDe: DebugElement = fixture.debugElement;
//        const divDe = appDe.query(By.css('div#flash'));
//        expect(divDe).toBeTruthy();
//    });
});
