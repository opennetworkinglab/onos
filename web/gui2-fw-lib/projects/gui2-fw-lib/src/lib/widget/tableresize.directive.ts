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
import { AfterContentChecked, Directive, Inject } from '@angular/core';
import { FnService } from '../util/fn.service';
import { LogService } from '../log.service';
import { MastService } from '../mast/mast.service';
import { HostListener } from '@angular/core';
import * as d3 from 'd3';

/**
 * ONOS GUI -- Widget -- Table Resize Directive
 */
@Directive({
    selector: '[onosTableResize]',
})
export class TableResizeDirective implements AfterContentChecked {

    pdg = 22;
    tables: any;

    constructor(protected fs: FnService,
        protected log: LogService,
        protected mast: MastService,
        @Inject('Window') private w: any) {

        log.info('TableResizeDirective constructed');
    }

    ngAfterContentChecked() {
        this.tables = {
            thead: d3.select('div.table-header').select('table'),
            tbody: d3.select('div.table-body').select('table')
        };
        this.windowSize(this.tables);
    }

    windowSize(tables: any) {
        const wsz = this.fs.windowSize(0, 30);
        this.adjustTable(tables, wsz.width, wsz.height);
    }

    @HostListener('window:resize', ['$event.target'])
    onResize(event: any) {
        this.windowSize(this.tables);
        return {
            h: this.w.innerHeight,
            w: this.w.innerWidth
        };
    }

    adjustTable(tables: any, width: number, height: number) {
        this._width(tables.thead, width + 'px');
        this._width(tables.tbody, width + 'px');

        this.setHeight(tables.thead, d3.select('div.table-body'), height);
    }

    _width(elem, width) {
        elem.style('width', width);
    }

    setHeight(thead: any, body: any, height: number) {
        const h = height - (this.mast.mastHeight +
            this.fs.noPxStyle(d3.select('.tabular-header'), 'height') +
            this.fs.noPxStyle(thead, 'height') + this.pdg);
        body.style('height', h + 'px');
    }

}
