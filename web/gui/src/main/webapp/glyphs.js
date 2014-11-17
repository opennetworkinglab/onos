/*
 * Copyright 2014 Open Networking Laboratory
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

/*
 SVG Glyphs.

 @author Simon Hunt
 */


(function (onos) {
    'use strict';

    // TODO: refactor this library...

    var birdData = "M427.7,300.4 c-6.9,0.6-13.1,5-19.2,7.1" +
        "c-18.1,6.2-33.9,9.1-56.5,4.7c24.6,17.2,36.6,13,63.7,0.1" +
        "c-0.5,0.6-0.7,1.3-1.3,1.9c1.4-0.4,2.4-1.7,3.4-2.2" +
        "c-0.4,0.7-0.9,1.5-1.4,1.9c2.2-0.6,3.7-2.3,5.9-3.9" +
        "c-2.4,2.1-4.2,5-6,8c-1.5,2.5-3.1,4.8-5.1,6.9c-1,1-1.9,1.9-2.9,2.9" +
        "c-1.4,1.3-2.9,2.5-5.1,2.9c1.7,0.1,3.6-0.3,6.5-1.9" +
        "c-1.6,2.4-7.1,6.2-9.9,7.2c10.5-2.6,19.2-15.9,25.7-18" +
        "c18.3-5.9,13.8-3.4,27-14.2c1.6-1.3,3-1,5.1-0.8" +
        "c1.1,0.1,2.1,0.3,3.2,0.5c0.8,0.2,1.4,0.4,2.2,0.8l1.8,0.9" +
        "c-1.9-4.5-2.3-4.1-5.9-6c-2.3-1.3-3.3-3.8-6.2-4.9" +
        "c-7.1-2.6-11.9,11.7-11.7-5c0.1-8,4.2-14.4,6.4-22" +
        "c1.1-3.8,2.3-7.6,2.4-11.5c0.1-2.3,0-4.7-0.4-7" +
        "c-2-11.2-8.4-21.5-19.7-24.8c-1-0.3-1.1-0.3-0.9,0" +
        "c9.6,17.1,7.2,38.3,3.1,54.2C429.9,285.5,426.7,293.2,427.7,300.4z";

    function defBird(defs) {
        defs.append('symbol')
            .attr({
                id: 'bird',
                viewBox: '352 224 113 112'
            })
            .append('path').attr('d', birdData);
    }

    var bullhornData = "M0,13c0,3.733,2.561,6.148,6.019,6.809 " +
        "C6.013,19.873,6,19.935,6,20v8 c0,1.105,0.895,2,2,2h3 " +
        "c1.105,0,2-0.896,2-2v-8h3V6H8C3.582,6,0,8.582,0,13z " +
        "M18,20h3V6h-3V20z M30,0l-7,4.667v16.667L30,26 c1.105,0,2-0.895,2-2" +
        "V2 C32,0.896,31.105,0,30,0z";

    function defBullhorn(defs) {
        defs.append('symbol')
            .attr({
                id: 'bullhorn',
                viewBox: '-4 -5 40 40'
            })
            .append('path').attr('d', bullhornData);
    }

    // === register the functions as a library
    onos.ui.addLib('glyphs', {
        defBird: defBird,
        defBullhorn: defBullhorn
    });

}(ONOS));
