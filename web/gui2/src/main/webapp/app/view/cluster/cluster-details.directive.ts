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
import {Directive, ElementRef, EventEmitter, Inject, Input, OnChanges, OnDestroy, OnInit, Output} from '@angular/core';
import {
    FnService,
    LogService,
    MastService,
    DetailsPanelBaseImpl,
    IconService,
    LionService,
    PanelService,
    WebSocketService
} from 'org_onosproject_onos/web/gui2-fw-lib/public_api';
import * as d3 from 'd3';
import {HostListener} from '@angular/core';

// internal state
let detailsPanel,
    pStartY,
    pHeight,
    top,
    topTable,
    bottom,
    iconDiv,
    wSize;


// constants
const topPdg = 28,
    ctnrPdg = 24,
    scrollSize = 17,
    portsTblPdg = 100,
    pName = 'details-panel',
    propOrder = [
        'id', 'ip',
    ],
    deviceCols = [
        'id', 'type', 'chassisid', 'mfr',
        'hw', 'sw', 'protocol', 'serial',
    ];

function addProp(tbody, label, value) {
    const tr = tbody.append('tr');

    function addCell(cls, txt) {
        tr.append('td').attr('class', cls).text(txt);
    }

    addCell('label', label + ' :');
    addCell('value', value);
}

function addDeviceRow(tbody, device) {
    const tr = tbody.append('tr');

    deviceCols.forEach(function (col) {
        tr.append('td').text(device[col]);
    });
}


/**
 * This should not be a directive - this should be a component, like all of the other details views
 * Change it when time allows
 */
@Directive({
    selector: '[onosClusterDetails]',
})
export class ClusterDetailsDirective extends DetailsPanelBaseImpl implements OnInit, OnDestroy, OnChanges {
    @Input() id: string;
    @Output() closeEvent = new EventEmitter<string>();

    lionFn; // Function

    constructor(protected fs: FnService,
                protected is: IconService,
                protected lion: LionService,
                protected wss: WebSocketService,
                protected log: LogService,
                protected mast: MastService,
                protected ps: PanelService,
                protected el: ElementRef,
                @Inject('Window') private w: Window) {
        super(fs, log, wss, 'cluster');

        if (this.lion.ubercache.length === 0) {
            this.lionFn = this.dummyLion;
            this.lion.loadCbs.set('clusterdetails', () => this.doLion());
        } else {
            this.doLion();
        }
        this.log.debug('ClusterDetailsDirective constructed');
    }

    ngOnInit() {
        this.init();
        this.initPanel();
        this.log.debug('Cluster Details Component initialized');
    }

    /**
     * Stop listening to clusterDetailsResponse on WebSocket
     */
    ngOnDestroy() {
        this.lion.loadCbs.delete('clusterdetails');
        this.destroy();
        this.ps.destroyPanel(pName);
        this.log.debug('Cluster Details Component destroyed');
    }

    @HostListener('window:resize', ['event'])
    onResize(event: any) {
        this.heightCalc();
        this.populateDetails(this.detailsData);
        return {
            h: this.w.innerHeight,
            w: this.w.innerWidth
        };
    }

    @HostListener('document:click', ['$event'])
    onClick(event) {
        if (event.path !== undefined) {
            for (let i = 0; i < event.path.length; i++) {
                if (event.path[i].className === 'close-btn') {
                    this.close();
                    break;
                }
            }
        } else if (event.target.href === undefined) {
            if (event.target.parentNode.className === 'close-btn') {
                this.close();
            }
        } else if (event.target.href.baseVal === '#xClose') {
            this.close();
        }
    }

    /**
     * Details Panel Data Request on row selection changes
     * Should be called whenever id changes
     * If id is empty, no request is made
     */
    ngOnChanges() {
        if (this.id === '') {
            if (detailsPanel) {
                detailsPanel.hide();
            }
            return '';
        } else {
            const query = {
                'id': this.id
            };
            this.requestDetailsPanelData(query);
            this.heightCalc();

            /*
             * Details data takes around 2ms to come up on web-socket
             * putting a timeout interval of 5ms
             */
            setTimeout(() => {
                this.populateDetails(this.detailsData);
                detailsPanel.show();
            }, 500);


        }
    }

    doLion() {
        this.lionFn = this.lion.bundle('core.view.Cluster');
    }

    heightCalc() {
        pStartY = this.fs.noPxStyle(d3.select('.tabular-header'), 'height')
            + this.mast.mastHeight + topPdg;
        wSize = this.fs.windowSize(this.fs.noPxStyle(d3.select('.tabular-header'), 'height'));
        pHeight = wSize.height;
    }

    createDetailsPane() {
        detailsPanel = this.ps.createPanel(pName, {
            width: wSize.width,
            margin: 0,
            hideMargin: 0,
        });
        detailsPanel.el().style('top', pStartY + 'px');
        detailsPanel.el().style('position', 'absolute');
        this.hidePanel = function () {
            detailsPanel.hide();
        };
        detailsPanel.hide();
    }

    initPanel() {
        this.heightCalc();
        this.createDetailsPane();
    }

    populateDetails(details) {
        this.setUpPanel();
        this.populateTop(details);
        this.populateBottom(details.devices);
        detailsPanel.height(pHeight);
    }

    setUpPanel() {
        let container, closeBtn;
        detailsPanel.empty();

        container = detailsPanel.append('div').classed('container', true);

        top = container.append('div').classed('top', true);
        closeBtn = top.append('div').classed('close-btn', true);
        this.addCloseBtn(closeBtn);
        iconDiv = top.append('div').classed('dev-icon', true);
        top.append('h2');
        topTable = top.append('div').classed('top-content', true)
            .append('table');
        top.append('hr');

        bottom = container.append('div').classed('bottom', true);
        bottom.append('h2').classed('devices-title', true).text('Devices');
        bottom.append('table');
    }

    addCloseBtn(div) {
        // This whole cluster app needs to be changed over to the Angular 7 style
        // It is the only one remaining that uses the d3 structure
        // this.is.loadEmbeddedIcon(div, 'close', 20);
        div.on('click', this.closePanel);
    }

    closePanel(): boolean {
        if (detailsPanel.isVisible()) {
            detailsPanel.hide();
            return true;
        }
        return false;
    }

    populateTop(details) {
        const propLabels = this.getLionProps();

        // This whole cluster app needs to be changed over to the Angular 7 style
        // It is the only one remaining that uses the d3 structure
        // this.is.loadEmbeddedIcon(iconDiv, 'node', 40);
        top.select('h2').text(details.id);

        const tbody = topTable.append('tbody');

        propOrder.forEach(function (prop, i) {
            addProp(tbody, propLabels[i], details[prop]);
        });
    }

    getLionDeviceCols() {
        return [
            this.lionFn('uri'),
            this.lionFn('type'),
            this.lionFn('chassis_id'),
            this.lionFn('vendor'),
            this.lionFn('hw_version'),
            this.lionFn('sw_version'),
            this.lionFn('protocol'),
            this.lionFn('serial_number'),
        ];
    }

    populateBottom(devices) {
        const table = bottom.select('table'),
            theader = table.append('thead').append('tr'),
            tbody = table.append('tbody');

        let tbWidth, tbHeight;

        this.getLionDeviceCols().forEach(function (col) {
            theader.append('th').text(col);
        });
        if (devices !== undefined) {
            devices.forEach(function (device) {
                addDeviceRow(tbody, device);
            });
        }

        tbWidth = this.fs.noPxStyle(tbody, 'width') + scrollSize;
        tbHeight = pHeight
            - (this.fs.noPxStyle(detailsPanel.el()
                    .select('.top'), 'height')
                + this.fs.noPxStyle(detailsPanel.el()
                    .select('.devices-title'), 'height')
                + portsTblPdg);

        table.style('zIndex', '0');
        table.style('height', tbHeight + 'px');
        table.style('width', tbWidth + 'px');
        table.style('overflow', 'auto');
        table.style('display', 'block');

        detailsPanel.width(tbWidth + ctnrPdg);
    }

    getLionProps() {
        return [
            this.lionFn('node_id'),
            this.lionFn('ip_address'),
        ];
    }
}
