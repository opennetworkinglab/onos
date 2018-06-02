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

/*
 ONOS GUI -- Layer -- Veil Service - Unit Tests
 */
import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { VeilComponent } from './veil.component';
import { ConsoleLoggerService } from '../../../consolelogger.service';
import { LogService } from '../../../log.service';
import { KeyService } from '../../util/key.service';
import { GlyphService } from '../../svg/glyph.service';

class MockKeyService {}

class MockGlyphService {}

describe('VeilComponent', () => {
    let log: LogService;

    beforeEach(() => {
        log = new ConsoleLoggerService();

        TestBed.configureTestingModule({
            declarations: [ VeilComponent ],
            providers: [
                { provide: LogService, useValue: log },
                { provide: KeyService, useClass: MockKeyService },
                { provide: GlyphService, useClass: MockGlyphService },
            ]
        });
    });

    it('should create', () => {
        const fixture = TestBed.createComponent(VeilComponent);
        const component = fixture.componentInstance;
        expect(component).toBeTruthy();
    });
});
