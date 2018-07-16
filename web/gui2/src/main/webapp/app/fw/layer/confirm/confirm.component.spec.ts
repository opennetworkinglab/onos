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

import { ConsoleLoggerService } from '../../../consolelogger.service';
import { LogService } from '../../../log.service';
import { ConfirmComponent } from './confirm.component';

/**
 * ONOS GUI -- Layer -- Confirm Component - Unit Tests
 */
describe('ConfirmComponent', () => {
    let log: LogService;
    let component: ConfirmComponent;
    let fixture: ComponentFixture<ConfirmComponent>;

    beforeEach(async(() => {
        log = new ConsoleLoggerService();
        TestBed.configureTestingModule({
            imports: [ BrowserAnimationsModule ],
            declarations: [ ConfirmComponent ],
            providers: [
                { provide: LogService, useValue: log },
            ]
        });
    }));

    beforeEach(() => {
        fixture = TestBed.createComponent(ConfirmComponent);
        component = fixture.debugElement.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });

    it('should have a div.dialog-button inside a div#app-dialog', () => {
        const appDe: DebugElement = fixture.debugElement;
        const divDe = appDe.query(By.css('div#app-dialog div.dialog-button'));
        const div: HTMLElement = divDe.nativeElement;
        // It selects the first one
        expect(div.textContent).toEqual('OK');
    });
});
