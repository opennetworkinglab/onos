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
    SvgUtilService
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

    constructor(
        protected fs: FnService,
        protected log: LogService,
        protected ps: PrefsService,
        protected is: IconService,
        protected sus: SvgUtilService
    ) {
        super(fs, log, ps);
        this.is.loadIconDef('bird');
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

}
