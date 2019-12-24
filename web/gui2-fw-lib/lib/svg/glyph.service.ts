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
import { FnService } from '../util/fn.service';
import { LogService } from '../log.service';
import * as gds from './glyphdata.service';
import * as d3 from 'd3';
import { SvgUtilService } from './svgutil.service';

// constants
const msgGS = 'GlyphService.';
const rg = 'registerGlyphs(): ';
const rgs = 'registerGlyphSet(): ';

/**
 * ONOS GUI -- SVG -- Glyph Service
 */
@Injectable({
  providedIn: 'root',
})
export class GlyphService {
    // internal state
    glyphs = d3.map();
    api: Object;

    constructor(
        private fs: FnService,
        //        private gd: GlyphDataService,
        private log: LogService,
        private sus: SvgUtilService
    ) {
        this.clear();
        this.init();
        this.api = {
            registerGlyphs: this.registerGlyphs,
            registerGlyphSet: this.registerGlyphSet,
            ids: this.ids,
            glyph: this.glyph,
            glyphDefined: this.glyphDefined,
            loadDefs: this.loadDefs,
            addGlyph: this.addGlyph,
        };
        this.log.debug('GlyphService constructed');
    }

    warn(msg: string): void {
        this.log.warn(msgGS + msg);
    }

    addToMap(key, value, vbox, overwrite: boolean, dups) {
        if (!overwrite && this.glyphs.get(key)) {
            dups.push(key);
        } else {
            this.glyphs.set(key, { id: key, vb: vbox, d: value });
        }
    }

    reportDups(dups: string[], which: string): boolean {
        const ok: boolean = (dups.length === 0);
        const msg = 'ID collision: ';

        if (!ok) {
            dups.forEach((id) => {
                this.warn(which + msg + '"' + id + '"');
            });
        }
        return ok;
    }

    reportMissVb(missing: string[], which: string): boolean {
        const ok: boolean = (missing.length === 0);
        const msg = 'Missing viewbox property: ';

        if (!ok) {
            missing.forEach((vbk) => {
                this.warn(which + msg + '"' + vbk + '"');
            });
        }
        return ok;
    }

    clear() {
        // start with a fresh map
        this.glyphs = d3.map();
    }

    init() {
        this.log.info('Registering glyphs');
        this.registerGlyphs(gds.logos);
        this.registerGlyphSet(gds.glyphDataSet);
        this.registerGlyphSet(gds.badgeDataSet);
        this.registerGlyphs(gds.spriteData);
        this.registerGlyphSet(gds.mojoDataSet);
        this.registerGlyphs(gds.extraGlyphs);
    }

    registerGlyphs(data: Map<string, string>, overwrite: boolean = false): boolean {
        const dups: string[] = [];
        const missvb: string[] = [];
        for (const [key, value] of data.entries()) {
            const vbk = '_' + key;
            const vb = data.get(vbk);

            if (key[0] !== '_') {
                if (!vb) {
                    missvb.push(vbk);
                    continue;
                }
                this.addToMap(key, value, vb, overwrite, dups);
            }
        }
        return this.reportDups(dups, rg) && this.reportMissVb(missvb, rg);
    }

    registerGlyphSet(data: Map<string, string>, overwrite: boolean = false): boolean {
        const dups: string[] = [];
        const vb: string = data.get('_viewbox');

        if (!vb) {
            this.warn(rgs + 'no "_viewbox" property found');
            return false;
        }

        for (const [key, value] of data.entries()) {
            //        angular.forEach(data, function (value, key) {
            if (key[0] !== '_') {
                this.addToMap(key, value, vb, overwrite, dups);
            }
        }
        return this.reportDups(dups, rgs);
    }

    ids() {
        return this.glyphs.keys();
    }

    glyph(id) {
        return this.glyphs.get(id);
    }

    glyphDefined(id) {
        return this.glyphs.has(id);
    }


    /**
     * Load definitions of a glyph
     *
     * Note: defs should be a D3 selection of a single <defs> element
     */
    loadDefs(defs, glyphIds: string[], noClear: boolean, asName?: string[]) {
        const list = this.fs.isA(glyphIds) || this.ids();

        if (!noClear) {
            // remove all existing content
            defs.html(null);
        }

        // load up the requested glyphs
        list.forEach((id, idx) => {
            const g = this.glyph(id);
            let asNameStr: string = asName[idx];
            if (!asNameStr) {
                asNameStr = id;
            }
            if (g) {
                if (noClear) {
                    // quick exit if symbol is already present
                    // TODO: check if this should be a continue or break instead
                    if (defs.select('symbol#' + asNameStr).size() > 0) {
                        return;
                    }
                }
                defs.append('symbol')
                    .attr('id', asNameStr)
                    .attr('viewBox', g.vb)
                    .append('path')
                    .attr('d', g.d);
            }
        });
    }

    addGlyph(elem: any, glyphId: string, size: number, overlay: any, trans: any) {
        const sz = size || 40;
        const ovr = !!overlay;
        const xns = this.fs.isA(trans);

        const glyphUse = elem
            .append('use')
            .attr('width', sz)
            .attr('height', sz)
            .attr('class', 'glyph')
            .attr('xlink:href', '#' + glyphId)
            .classed('overlay', ovr);

        if (xns) {
            glyphUse.attr('transform', this.sus.translate(trans));
        }

        return glyphUse;
    }
}
