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
import { Component, OnInit } from '@angular/core';
import {
    FnService,
    LogService,
    ZoomService, Zoomer, ZoomOpts
} from 'gui2-fw-lib';

/**
 * ONOS GUI -- Topology Zoom Layer View.
 * View that contains the 'Force graph' message
 *
 * This component is an SVG snippet that expects to be in an SVG element with a view box of 1000x1000
 *
 * It should be added to a template with a tag like <svg:g onos-zoomlayer />
 */
@Component({
  selector: '[onos-zoomlayer]',
  templateUrl: './zoomlayersvg.component.html',
  styleUrls: ['./zoomlayersvg.component.css']
})
export class ZoomLayerSvgComponent implements OnInit {
    zoomer: Zoomer;
    zoomEventListeners: any[];

    constructor(
        protected fs: FnService,
        protected log: LogService,
        protected zs: ZoomService
    ) {
        this.log.debug('ZoomLayerSvgComponent constructed');
    }

    ngOnInit() {

    }

    createZoomer(options: ZoomOpts) {
        // need to wrap the original zoom callback to extend its behavior
        const origCallback = this.fs.isF(options.zoomCallback) ? options.zoomCallback : () => {};

        options.zoomCallback = () => {
            origCallback([0, 0], 1);

            this.zoomEventListeners.forEach((ev) => ev(this.zoomer));
        };

        this.zoomer = this.zs.createZoomer(options);
        return this.zoomer;
    }

    getZoomer() {
        return this.zoomer;
    }

    findZoomEventListener(ev) {
        for (let i = 0, len = this.zoomEventListeners.length; i < len; i++) {
            if (this.zoomEventListeners[i] === ev) {
                return i;
            }
        }
        return -1;
    }

    addZoomEventListener(callback) {
        this.zoomEventListeners.push(callback);
    }

    removeZoomEventListener(callback) {
        const evIndex = this.findZoomEventListener(callback);

        if (evIndex !== -1) {
            this.zoomEventListeners.splice(evIndex);
        }
    }

    adjustmentScale(min: number, max: number): number {
        let _scale = 1;
        const size = (min + max) / 2;

        if (size * this.scale() < max) {
            _scale = min / (size * this.scale());
        } else if (size * this.scale() > max) {
            _scale = min / (size * this.scale());
        }

        return _scale;
    }

    scale(): number {
        return this.zoomer.scale();
    }

    panAndZoom(translate: number[], scale: number, transition?: number) {
        this.zoomer.panZoom(translate, scale, transition);
    }

}
