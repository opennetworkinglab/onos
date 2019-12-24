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
import * as d3 from 'd3';
import {TopoZoomPrefs} from './zoomutils';
import {LogService} from '../log.service';
import {PrefsService} from '../util/prefs.service';

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
    zoomCached: TopoZoomPrefs = <TopoZoomPrefs>{tx: 0, ty: 0, sc: 1.0};

    constructor(
        private _element: ElementRef,
        private log: LogService,
        private ps: PrefsService
    ) {
        const container = d3.select(this._element.nativeElement);

        const zoomed = () => {
            const transform = d3.event.transform;
            container.attr('transform', 'translate(' + transform.x + ',' + transform.y + ') scale(' + transform.k + ')');
            this.updateZoomState(<TopoZoomPrefs>{tx: transform.x, ty: transform.y, sc: transform.k});
        };

        this.zoom = d3.zoom().on('zoom', zoomed);
    }

    ngOnInit() {
        this.zoomCached = this.ps.getPrefs(TOPO_ZOOM_PREFS, ZOOM_PREFS_DEFAULT);
        const svg = d3.select(this.zoomableOf);

        svg.call(this.zoom);

        svg.transition().call(this.zoom.transform,
            d3.zoomIdentity.translate(this.zoomCached.tx, this.zoomCached.ty).scale(this.zoomCached.sc));
        this.log.debug('Loaded topo_zoom_prefs',
            this.zoomCached.tx, this.zoomCached.ty, this.zoomCached.sc);

    }

    /**
     * Updates the cache of zoom preferences locally and onwards to the PrefsService
     */
    updateZoomState(zoomPrefs: TopoZoomPrefs): void {
        this.zoomCached = zoomPrefs;
        this.ps.setPrefs(TOPO_ZOOM_PREFS, zoomPrefs);
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
     * Change the zoom level when a map is chosen in Topology view
     *
     * Animated to run over 750ms
     */
    changeZoomLevel(zoomState: TopoZoomPrefs, fast?: boolean): void {
        const svg = d3.select(this.zoomableOf);
        svg.transition().duration(fast ? 0 : 750).call(this.zoom.transform,
            d3.zoomIdentity.translate(zoomState.tx, zoomState.ty).scale(zoomState.sc));
        this.updateZoomState(zoomState);
        this.log.debug('Pan to', zoomState.tx, zoomState.ty, 'and zoom to', zoomState.sc);
    }

}
