/*
 * Copyright 2019-present Open Networking Foundation
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

import { QuickhelpComponent } from './quickhelp.component';
import {LogService} from '../../log.service';
import {ConsoleLoggerService} from '../../consolelogger.service';
import {BrowserAnimationsModule} from '@angular/platform-browser/animations';
import {FnService} from '../../util/fn.service';
import {LionService} from '../../util/lion.service';
import {KeysService} from '../../util/keys.service';

class MockFnService {}

class MockKeysService {
    keyHandler: {
        viewKeys: any[],
        globalKeys: any[]
    };

    mockViewKeys: Object[];
    constructor() {
        this.mockViewKeys = [];
        this.keyHandler = {
            viewKeys: this.mockViewKeys,
            globalKeys: this.mockViewKeys
        };
    }
}

/**
 * ONOS GUI -- Layer -- Quickhelp Component - Unit Tests
 */
describe('QuickhelpComponent', () => {
    let log: LogService;
    let component: QuickhelpComponent;
    let fixture: ComponentFixture<QuickhelpComponent>;
    const bundleObj = {
        'core.fw.QuickHelp': {
            test: 'test1',
            tt_help: 'Help!'
        }
    };
    const mockLion = (key) =>  {
        return bundleObj[key] || '%' + key + '%';
    };

    beforeEach(async(() => {
        log = new ConsoleLoggerService();
        TestBed.configureTestingModule({
            imports: [ BrowserAnimationsModule ],
            declarations: [ QuickhelpComponent ],
            providers: [
                { provide: LogService, useValue: log },
                { provide: FnService, useClass: MockFnService },
                { provide: LionService, useFactory: (() => {
                        return {
                            bundle: ((bundleId) => mockLion),
                            ubercache: new Array(),
                            loadCbs: new Map<string, () => void>([])
                        };
                    })
                },
                { provide: KeysService, useClass: MockKeysService }
            ]
        })
        .compileComponents();
    }));

    beforeEach(() => {
        fixture = TestBed.createComponent(QuickhelpComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});
