import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { LogService } from '../../log.service';
import { ConsoleLoggerService } from '../../consolelogger.service';
import { IconComponent } from './icon.component';
import { IconService } from '../icon.service';

class MockIconService {}

describe('IconComponent', () => {
    let log: LogService;

    beforeEach(() => {
        log = new ConsoleLoggerService();

        TestBed.configureTestingModule({
            declarations: [ IconComponent ],
            providers: [
                { provide: LogService, useValue: log },
                { provide: IconService, useClass: MockIconService },
            ]
        });
    });

    it('should create', () => {
        const fixture = TestBed.createComponent(IconComponent);
        const component = fixture.componentInstance;
        expect(component).toBeTruthy();
    });
});
