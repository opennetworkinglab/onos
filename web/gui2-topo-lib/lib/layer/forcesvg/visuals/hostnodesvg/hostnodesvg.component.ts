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
    EventEmitter,
    Input,
    OnChanges,
    Output,
    SimpleChanges
} from '@angular/core';
import {Badge, Host, HostLabelToggle, Node} from '../../models';
import {LogService} from 'org_onosproject_onos/web/gui2-fw-lib/public_api';
import {NodeVisual, SelectedEvent} from '../nodevisual';

/**
 * The Host node in the force graph
 *
 * Note: here the selector is given square brackets [] so that it can be
 * inserted in SVG element like a directive
 */
@Component({
    selector: '[onos-hostnodesvg]',
    templateUrl: './hostnodesvg.component.html',
    styleUrls: ['./hostnodesvg.component.css']
})
export class HostNodeSvgComponent extends NodeVisual implements OnChanges {
    @Input() host: Host;
    @Input() scale: number = 1.0;
    @Input() labelToggle: HostLabelToggle.Enum = HostLabelToggle.Enum.IP;
    @Input() badge: Badge;
    @Output() selectedEvent = new EventEmitter<SelectedEvent>();

    constructor(
        protected log: LogService
    ) {
        super();
    }

    ngOnChanges(changes: SimpleChanges) {
        if (changes['host']) {
            if (!this.host.x) {
                this.host.x = 0;
                this.host.y = 0;
            }
        }

        if (changes['badge']) {
            this.badge = changes['badge'].currentValue;
        }
    }

    hostName(): string {
        if (this.host === undefined) {
            return undefined;
        } else if (this.labelToggle === HostLabelToggle.Enum.IP) {
            return this.host.ips.join(',');
        } else if (this.labelToggle === HostLabelToggle.Enum.MAC) {
            return this.host.id;
        } else if (this.labelToggle === HostLabelToggle.Enum.NAME) {
            return this.host.props.name;
        } else {
            return this.host.id;
        }

    }
}
