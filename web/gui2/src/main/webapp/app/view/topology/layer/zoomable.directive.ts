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
import {Directive, ElementRef, Input, OnChanges} from '@angular/core';
import {LogService} from 'gui2-fw-lib';
import * as d3 from 'd3';

@Directive({
  selector: '[onosZoomableOf]'
})
export class ZoomableDirective implements OnChanges {
    @Input() zoomableOf: ElementRef;

    constructor(
        private _element: ElementRef,
        private log: LogService,
    ) {}

    /**
     * If the input object is changed then re-establish the zoom
     */
    ngOnChanges() {
        let zoomed, zoom;

        const svg = d3.select(this.zoomableOf);
        const container = d3.select(this._element.nativeElement);

        zoomed = () => {
            const transform = d3.event.transform;
            container.attr('transform', 'translate(' + transform.x + ',' + transform.y + ') scale(' + transform.k + ')');
        };

        zoom = d3.zoom().on('zoom', zoomed);
        svg.call(zoom);
        this.log.debug('Applying zoomable behaviour on', this.zoomableOf, this._element.nativeElement);
    }

    /**
     * Reset the zoom when the R key is pressed when in Topology view
     */
    resetZoom(): void {
        const container = d3.select(this._element.nativeElement);
        container.attr('transform', 'translate(0,0) scale(1.0)');
        this.log.debug('Pan to 0,0 and zoom to 1.0');
    }

}
