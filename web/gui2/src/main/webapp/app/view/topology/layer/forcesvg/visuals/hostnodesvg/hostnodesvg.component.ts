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
import {
    Component,
    Input,
    OnChanges,
    SimpleChanges
} from '@angular/core';
import { Host } from '../../models';
import {LogService} from 'gui2-fw-lib';

/**
 * The Host node in the force graph
 *
 * Note 1: here the selector is given square brackets [] so that it can be
 * inserted in SVG element like a directive
 * Note 2: the selector is exactly the same as the @Input alias to make this
 * directive trick work
 */
@Component({
    selector: '[onos-hostnodesvg]',
    templateUrl: './hostnodesvg.component.html',
    styleUrls: ['./hostnodesvg.component.css']
})
export class HostNodeSvgComponent implements OnChanges {
    @Input() host: Host;

    constructor(
        protected log: LogService
    ) {
    }

    ngOnChanges(changes: SimpleChanges) {
        if (!this.host.x) {
            this.host.x = 0;
            this.host.y = 0;
        }
    }
}
