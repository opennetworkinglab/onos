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
import * as d3 from 'd3';

/**
 * ONOS GUI -- SVG -- Util Service
 *
 * The SVG Util Service provides a miscellany of utility functions.
 */
@Injectable({
    providedIn: 'root',
})
export class SvgUtilService {
    lightNorm: string[];
    lightMute: string[];
    darkNorm: string[];
    darkMute: string[];
    colors: any;

    constructor(
        private fs: FnService,
        private log: LogService
    ) {

        // --- Ordinal scales for 7 values.
        // TODO: migrate these colors to the theme service.

        // Colors per Mojo-Design's color palette.. (version one)
        //               blue       red        dk grey    steel      lt blue    lt red     lt grey
        // var lightNorm = ['#5b99d2', '#d05a55', '#716b6b', '#7e9aa8', '#66cef6', '#db7773', '#aeada8' ],
        //     lightMute = ['#a8cceb', '#f1a7a7', '#b9b5b5', '#bdcdd5', '#a8e9fd', '#f8c9c9', '#d7d6d4' ],

        // Colors per Mojo-Design's color palette.. (version two)
        //               blue       lt blue    red        green      brown      teal       lime
        this.lightNorm = ['#5b99d2', '#66cef6', '#d05a55', '#0f9d58', '#ba7941', '#3dc0bf', '#56af00'];
        this.lightMute = ['#9ebedf', '#abdef5', '#d79a96', '#7cbe99', '#cdab8d', '#96d5d5', '#a0c96d'];

        this.darkNorm = ['#5b99d2', '#66cef6', '#d05a55', '#0f9d58', '#ba7941', '#3dc0bf', '#56af00'];
        this.darkMute = ['#9ebedf', '#abdef5', '#d79a96', '#7cbe99', '#cdab8d', '#96d5d5', '#a0c96d'];


        this.colors = {
            light: {
                norm: d3.scaleOrdinal().range(this.lightNorm),
                mute: d3.scaleOrdinal().range(this.lightMute),
            },
            dark: {
                norm: d3.scaleOrdinal().range(this.darkNorm),
                mute: d3.scaleOrdinal().range(this.darkMute),
            },
        };

        this.log.debug('SvgUtilService constructed');
    }

    translate(x: number[], y?: any): string {
        if (this.fs.isA(x) && x.length === 2 && !y) {
            return 'translate(' + x[0] + ',' + x[1] + ')';
        }
        return 'translate(' + x + ',' + y + ')';
    }

    scale(x, y) {
        return 'scale(' + x + ',' + y + ')';
    }

    skewX(x) {
        return 'skewX(' + x + ')';
    }

    rotate(deg) {
        return 'rotate(' + deg + ')';
    }

    cat7() {
        const tcid = 'd3utilTestCard';

        function getColor(id, muted, theme) {
            // NOTE: since we are lazily assigning domain ids, we need to
            //       get the color from all 4 scales, to keep the domains
            //       in sync.
            const ln = this.colors.light.norm(id);
            const lm = this.colors.light.mute(id);
            const dn = this.colors.dark.norm(id);
            const dm = this.colors.dark.mute(id);
            if (theme === 'dark') {
                return muted ? dm : dn;
            } else {
                return muted ? lm : ln;
            }
        }

        function testCard(svg) {
            let g = svg.select('g#' + tcid);
            const dom = d3.range(7);
            let k;
            let muted;
            let theme;
            let what;

            if (!g.empty()) {
                g.remove();

            } else {
                g = svg.append('g')
                    .attr('id', tcid)
                    .attr('transform', 'scale(4)translate(20,20)');

                for (k = 0; k < 4; k++) {
                    muted = k % 2;
                    what = muted ? ' muted' : ' normal';
                    theme = k < 2 ? 'light' : 'dark';
                    dom.forEach(function (id, i) {
                        const x = i * 20;
                        const y = k * 20;
                        const f = getColor(id, muted, theme);
                        g.append('circle').attr({
                            cx: x,
                            cy: y,
                            r: 5,
                            fill: f,
                        });
                    });
                    g.append('rect').attr({
                        x: 140,
                        y: k * 20 - 5,
                        width: 32,
                        height: 10,
                        rx: 2,
                        fill: '#888',
                    });
                    g.append('text').text(theme + what)
                        .attr({
                            x: 142,
                            y: k * 20 + 2,
                            fill: 'white',
                        })
                        .style('font-size', '4pt');
                }
            }
        }

        return {
            testCard: testCard,
            getColor: getColor,
        };
    }

}
