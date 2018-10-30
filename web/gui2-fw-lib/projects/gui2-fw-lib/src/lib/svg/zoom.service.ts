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
import { Injectable } from '@angular/core';
import * as d3 from 'd3';
import { LogService } from '../log.service';

export interface ZoomOpts {
    svg: any;                         // D3 selection of <svg> element
    zoomLayer: any;                   // D3 selection of <g> element
    zoomMin: number;                  // Min zoom level - usually 0.25
    zoomMax: number;                  // Max zoom level - usually 10
    zoomEnabled(ev: any): boolean;   // Function that takes event and returns boolean
    zoomCallback(translate: number[], scale: number): void; // Function that is called on zoom
}

export interface Zoomer {
    panZoom(translate: number[], scale: number, transition?: number): void;
    reset(): void;
    translate(): number[];
    scale(): number;
    scaleExtent(): number[];
}

export const CZ: string = 'ZoomService.createZoomer(): ';
export const D3S: string = ' (D3 selection) property defined';

/**
 * ONOS GUI -- Topology Zoom Service Module.
 */
@Injectable({
    providedIn: 'root',
})
export class ZoomService {
    defaultSettings: ZoomOpts;

    zoom: any;
    public zoomer: Zoomer;
    settings: ZoomOpts;

    constructor(
        protected log: LogService,
    ) {
        this.defaultSettings = <ZoomOpts>{
            zoomMin: 0.05,
            zoomMax: 50,
            zoomEnabled: (ev) => true,
            zoomCallback: (t, s) => { return; }
        };

        this.log.debug('ZoomService constructed');
    }

    createZoomer(opts: ZoomOpts): Zoomer {
        this.settings = Object.assign(this.defaultSettings, opts);

        if (!this.settings.svg) {
            this.log.error(CZ + 'No "svg" (svg tag)' + D3S);
            throw new Error(CZ + 'No "svg" (svg tag)' + D3S);
        }
        if (!this.settings.zoomLayer) {
            this.log.error(CZ + 'No "zoomLayer" (g tag)' + D3S);
            throw new Error(CZ + 'No "zoomLayer" (g tag)' + D3S);
        }

        this.zoom = d3.zoom()
            .scaleExtent([this.settings.zoomMin, this.settings.zoomMax])
            .extent([[0, 0], [1000, 1000]])
            .on('zoom', () => this.zoomed);


        this.zoomer = <Zoomer>{
            panZoom: (translate: number[], scale: number, transition?: number) => {
                this.settings.svg.call(this.zoom.translateBy, translate[0], translate[1]);
                this.settings.svg.call(this.zoom.scaleTo, scale);
                this.adjustZoomLayer(translate, scale, transition);
            },

            reset: () => {
                this.settings.svg.call(this.zoom.translateTo, 500, 500);
                this.settings.svg.call(this.zoom.scaleTo, 1);
                this.adjustZoomLayer([0, 0], 1, 0);
            },

            translate: () => {
                const trans = d3.zoomTransform(this.settings.svg.node());
                return [trans.x, trans.y];
            },

            scale: () => {
                const trans = d3.zoomTransform(this.settings.svg.node());
                return trans.k;
            },

            scaleExtent: () => {
                return this.zoom.scaleExtent();
            },
        };

        // apply the zoom behavior to the SVG element
/*
        if (this.settings.svg ) {
            this.settings.svg.call(this.zoom);
        }
*/
        // Remove zoom on double click (prevents a
        // false zoom navigating regions)
        // this.settings.svg.on('dblclick.zoom', null);

        return this.zoomer;
    }

    /**
     * zoom events from mouse gestures...
     */
    zoomed() {
        const ev = d3.event.sourceEvent;
        if (this.settings.zoomEnabled(ev)) {
            this.adjustZoomLayer(d3.event.translate, d3.event.scale);
        }
    }

    /**
     * Adjust the zoom layer
     */
    adjustZoomLayer(translate: number[], scale: number, transition?: any): void {

        this.settings.zoomLayer.transition()
            .duration(transition || 0)
            .attr('transform',
                'translate(' + translate + ') scale(' + scale + ')');

        this.settings.zoomCallback(translate, scale);
    }

}
