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
import {
    Component,
    Input,
    OnDestroy,
    OnInit,
    ViewEncapsulation
} from '@angular/core';
import { animate, state, style, transition, trigger } from '@angular/animations';
import * as d3 from 'd3';
import { TopoPanelBaseImpl } from '../topopanel.base';
import {
    LogService,
    FnService,
    WebSocketService,
    GlyphService
} from 'org_onosproject_onos/web/gui2-fw-lib/public_api';

export interface SummaryResponse {
    title: string;
}
/**
 * ONOS GUI -- Topology Summary Module.
 * Defines modeling of ONOS Summary Panel.
 * Note: This component uses the d3 DOM building technique from the old GUI - this
 * is not the Angular way of building components and should be avoided generally
 * See DetailsPanelComponent for a better way of doing this kind of thing
 */
@Component({
    selector: 'onos-summary',
    templateUrl: './summary.component.html',
    styleUrls: [
        './summary.component.css',
        '../../topology.common.css', '../../topology.theme.css',
        '../../../../gui2-fw-lib/lib/widget/panel.css',
        '../../../../gui2-fw-lib/lib/widget/panel-theme.css'
    ],
    encapsulation: ViewEncapsulation.None,
    animations: [
        trigger('summaryPanelState', [
            state('true', style({
                transform: 'translateX(0%)',
                opacity: '100'
            })),
            state('false', style({
                transform: 'translateX(100%)',
                opacity: '0'
            })),
            transition('0 => 1', animate('100ms ease-in')),
            transition('1 => 0', animate('100ms ease-out'))
        ])
    ]
})
export class SummaryComponent extends TopoPanelBaseImpl implements OnInit, OnDestroy {
    @Input() on: boolean = false; // Override the parent class attribute
    private handlers: string[] = [];
    private resp: string = 'showSummary';
    private summaryData: SummaryResponse;

    constructor(
        protected fs: FnService,
        protected log: LogService,
        protected wss: WebSocketService,
        protected gs: GlyphService
    ) {
        super(fs, log, 'summary');
        this.summaryData = <SummaryResponse>{};
        this.log.debug('SummaryComponent constructed');
    }


    ngOnInit() {
        this.wss.bindHandlers(new Map<string, (data) => void>([
            [this.resp, (data) => this.handleSummaryData(data)]
        ]));
        this.handlers.push(this.resp);

        this.init(d3.select('#topo2-p-summary'));

        this.wss.sendEvent('requestSummary', {});
    }

    ngOnDestroy() {
        this.wss.sendEvent('cancelSummary', {});
        this.wss.unbindHandlers(this.handlers);
    }

    handleSummaryData(data: SummaryResponse) {
        this.summaryData = data;
        this.render();
    }

    private render() {
        let endedWithSeparator;

        this.emptyRegions();

        const svg = this.appendToHeader('div')
                .classed('icon', true)
                .append('svg');
        const title = this.appendToHeader('h2');
        const table = this.appendToBody('table');
        const tbody = table.append('tbody');

        title.text(this.summaryData.title);
        this.gs.addGlyph(svg, 'bird', 24, 0, [1, 1]);
        endedWithSeparator = this.listProps(tbody, this.summaryData);
        // TODO : review whether we need to use/store end-with-sep state
    }
}
