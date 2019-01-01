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
import { Component, OnInit } from '@angular/core';
import { ViewControllerImpl } from '../viewcontroller';
import {
    FnService,
    LogService,
    PrefsService,
    IconService,
    SvgUtilService, LionService
} from 'gui2-fw-lib';

/**
 * ONOS GUI -- Topology No Connected Devices View.
 * View that contains the 'No Connected Devices' message
 *
 * This component is an SVG snippet that expects to be in an SVG element with a view box of 1000x1000
 *
 * It should be added to a template with a tag like <svg:g onos-nodeviceconnected />
 */
@Component({
  selector: '[onos-nodeviceconnected]',
  templateUrl: './nodeviceconnectedsvg.component.html',
  styleUrls: ['./nodeviceconnectedsvg.component.css']
})
export class NoDeviceConnectedSvgComponent extends ViewControllerImpl implements OnInit {
    lionFn; // Function

    constructor(
        protected fs: FnService,
        protected log: LogService,
        protected ps: PrefsService,
        protected sus: SvgUtilService,
        private lion: LionService
    ) {
        super(fs, log, ps);

        if (this.lion.ubercache.length === 0) {
            this.lionFn = this.dummyLion;
            this.lion.loadCbs.set('topo-nodevices', () => this.doLion());
        } else {
            this.doLion();
        }

        this.log.debug('NoDeviceConnectedSvgComponent constructed');
    }

    ngOnInit() {
        this.log.debug('NoDeviceConnectedSvgComponent initialized');
    }

    /*
     * The whole SVG canvas is based on a 1000 by 1000 box
     */
    centre(repositionBox: SVGRect) {
        const scale: number = Number.parseFloat((1000 / repositionBox.width).toFixed(3));
        repositionBox.x -= Number.parseFloat((repositionBox.width * scale / 2).toFixed(0));
        repositionBox.y -= Number.parseFloat((repositionBox.height * scale / 2).toFixed(0));
        return this.sus.translate([repositionBox.x, repositionBox.y]) + '' + this.sus.scale(scale, scale);
    }

    /**
     * Read the LION bundle for Details panel and set up the lionFn
     */
    doLion() {
        this.lionFn = this.lion.bundle('core.view.Topo');

    }

    /**
     * A dummy implementation of the lionFn until the response is received and the LION
     * bundle is received from the WebSocket
     */
    dummyLion(key: string): string {
        return '%' + key + '%';
    }
}
