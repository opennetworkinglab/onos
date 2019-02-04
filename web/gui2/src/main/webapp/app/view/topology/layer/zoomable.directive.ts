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
    Directive,
    ElementRef,
    Input,
    OnChanges,
    OnInit,
    SimpleChanges
} from '@angular/core';
import {LogService, PrefsService} from 'gui2-fw-lib';
import * as d3 from 'd3';


/**
 * Model of the Zoom preferences
 */
export interface TopoZoomPrefs {
    tx: number;
    ty: number;
    sc: number;
}

const TOPO_ZOOM_PREFS = 'topo_zoom';

const ZOOM_PREFS_DEFAULT: TopoZoomPrefs = <TopoZoomPrefs>{
    tx: 0, ty: 0, sc: 1.0
};

/**
 * A directive that takes care of Zooming and Panning the Topology view
 *
 * It wraps the D3 Pan and Zoom functionality
 * See https://github.com/d3/d3-zoom/blob/master/README.md
 */
@Directive({
  selector: '[onosZoomableOf]'
})
export class ZoomableDirective implements OnChanges, OnInit {
    @Input() zoomableOf: ElementRef;

    zoom: any; // The d3 zoom behaviour

    constructor(
        private _element: ElementRef,
        private log: LogService,
        private ps: PrefsService
    ) {
        const container = d3.select(this._element.nativeElement);

        const zoomed = () => {
            const transform = d3.event.transform;
            container.attr('transform', 'translate(' + transform.x + ',' + transform.y + ') scale(' + transform.k + ')');
            this.updateZoomState(transform.x, transform.y, transform.k);
        };

        this.zoom = d3.zoom().on('zoom', zoomed);
    }

    ngOnInit() {
        const zoomState: TopoZoomPrefs = this.ps.getPrefs(TOPO_ZOOM_PREFS, ZOOM_PREFS_DEFAULT);
        const svg = d3.select(this.zoomableOf);

        svg.call(this.zoom);

        svg.transition().call(this.zoom.transform,
            d3.zoomIdentity.translate(zoomState.tx, zoomState.ty).scale(zoomState.sc));
        this.log.debug('Loaded topo_zoom_prefs',
            zoomState.tx, zoomState.ty, zoomState.sc);

    }

    /**
     * Updates the cache of zoom preferences locally and onwards to the PrefsService
     */
    updateZoomState(x: number, y: number, scale: number): void {
        this.ps.setPrefs(TOPO_ZOOM_PREFS, <TopoZoomPrefs>{
            tx: x,
            ty: y,
            sc: scale
        });
    }

    /**
     * If the input object is changed then re-establish the zoom
     */
    ngOnChanges(changes: SimpleChanges): void {
        if (changes['zoomableOf']) {
            const svg = d3.select(changes['zoomableOf'].currentValue);
            svg.call(this.zoom);
            this.log.debug('Applying zoomable behaviour on', this.zoomableOf, this._element.nativeElement);
        }
    }

    /**
     * Reset the zoom when the R key is pressed when in Topology view
     *
     * Animated to run over 750ms
     */
    resetZoom(): void {
        const svg = d3.select(this.zoomableOf);
        svg.transition().duration(750).call(this.zoom.transform, d3.zoomIdentity);
        this.updateZoomState(0, 0, 1.0);
        this.log.debug('Pan to 0,0 and zoom to 1.0');
    }

}
