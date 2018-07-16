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
import { Directive, ElementRef } from '@angular/core';
import { FnService } from '../util/fn.service';
import { LogService } from '../../log.service';

/**
 * ONOS GUI -- Widget -- Table Resize Directive
 */
@Directive({
    selector: '[onosTableResize]',
})
export class TableResizeDirective {

    constructor(
        private fs: FnService,
        public log: LogService,
        private el: ElementRef,
    ) {

        this.windowSize();
        this.log.debug('TableResizeDirective constructed');
    }

    windowSize() {
        const wsz = this.fs.windowSize(0, 30);
        this.el.nativeElement.style.width = wsz.width + 'px';
    }
}
