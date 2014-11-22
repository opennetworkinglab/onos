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

    var glyphData = {
        unknown: "M-20 -15 a5 5 0 0 1 5 -5 h30 a5 5 0 0 1 5 5 v30 " +
            "a5 5 0 0 1 -5 5 h-30 a5 5 0 0 1 -5 -5 z",

        node: "M-40 45 a5 5 0 0 1 -5 -5 v-65 a5 5 0 0 1 5 -5 h80 " +
            "a5 5 0 0 1 5 5 v65 a5 5 0 0 1 -5 5 z M-41 -32.5 l11 -11  " +
            "a10 3 0 0 1 10 -2 h40 a10 3 0 0 1 10 2 l11 11 z M-39 -20 " +
            "a5 5 0 0 1 10 0 a5 5 0 0 1 -10 0 z",

        switch: "M-45 -35 a10 10 0 0 1 10 -10 h70 a 10 10 0 0 1 10 10 " +
            "v70 a 10 10 0 0 1 -10 10 h -70 a 10 10 0 0 1 -10 -10 z " +
            "M 5 -29 l 12 0, 0 -8, 18 13, -18 13, 0 -8, -12 0 z M 5 5 " +
            "l 12 0, 0 -8, 18 13, -18 13, 0 -8, -12 0 z M -5 -15 " +
            "l -12 0, 0 -8, -18 13, 18 13, 0 -8, 12 0 z M -5 19 " +
            "l -12 0, 0 -8, -18 13, 18 13, 0 -8, 12 0 z",

        roadm: "M-45 -20 l25 -25 h40 l25 25 v40 l-25 25 h-40 l-25 -25 z " +
            "M 3 -29 l 12 0, 0 -8, 18 13, -18 13, 0 -8, -12 0 z M 3 5 " +
            "l 12 0, 0 -8, 18 13, -18 13, 0 -8, -12 0 z M -3 -15 " +
            "l -12 0, 0 -8, -18 13, 18 13, 0 -8, 12 0 z M -3 19 " +
            "l -12 0, 0 -8, -18 13, 18 13, 0 -8, 12 0 z",

        endstation: "M-45 -40 a5 5 0 0 1 5 -5 h65 a5 5 0 0 1 5 5 v80 " +
            "a5 5 0 0 1 -5 5 h-65 a5 5 0 0 1 -5 -5 z M32.5 -41 l11 11  " +
            "a3 10 0 0 1 2 10 v40 a3 10 0 0 1 -2 10 l-11 11 z M-38 -36 " +
            "a2 2 0 0 1 2 -2 h56 a2 2 0 0 1 2 2 v26 a2 2 0 0 1 -2 2 h-56 " +
            "a2 2 0 0 1 -2 -2 z M-35 -35 h54 v10 h-54 z M-35 -22 h54 v10 " +
            "h-54 z M-13 15 a5 5 0 0 1 10 0 a5 5 0 0 1 -10 0 z",

        router: "M-45 0 A45 45 0 0 1 45 0 A45 45 0 0 1 -45 0 M -35 -5 " +
            "l 12 0, 0 -8, 18 13, -18 13, 0 -8, -12 0 z M 35 -5 " +
            "l -12 0, 0 -8, -18 13, 18 13, 0 -8, 12 0 z M -5 -8 " +
            "l 0 -12, -8 0, 13 -18, 13 18, -8 0, 0 12 z M -5 8 " +
            "l 0 12, -8 0, 13 18, 13 -18, -8 0, 0 -12 z",

        bgpSpeaker: "M-45 -15 a45 35 0 0 1 90 0 Q45 22 0 45 Q-45 22 -45 -15 z " +
            "M -5 -26 l 12 0, 0 -8, 18 13, -18 13, 0 -8, -12 0 z M 5 2" +
            " l -12 0, 0 -8, -18 13, 18 13, 0 -8, 12 0 z"
    };

    var glyphParams = {
        viewBox: '-55 -55 110 110'
    };


    var badgeData = {
        uiAttached: "M-3-2.5 a.5 .5 0 0 1 .5-.5 h5 a.5 .5 0 0 1 .5 .5 v3 " +
        "a.5 .5 0 0 1-.5 .5 h-5 a.5 .5 0 0 1-.5-.5 z M-2.5-2.2 " +
        "a.3 .3 0 0 1 .3-.3 h4.4 a.3 .3 0 0 1 .3 .3 v2.4 a.3 .3 0 0 1-.3 .3 " +
        "h-4.4 a.3 .3 0 0 1-.3-.3 z M-3 1.55 h6 l1 1.45 h-8 z"
    };

    var badgeParams = {
        viewBox: '-5 -5 10 10'
    };

    function defStuff(defs, params, data) {
        d3.map(data).keys().forEach(function (key) {
            defs.append('symbol')
                .attr({
                    id: key,
                    viewBox: params.viewBox
                })
                .append('path').attr('d', data[key]);
        });
    }

    // === register the functions as a library
    onos.ui.addLib('glyphs', {
        defBird: defBird,
        defGlyphs: function (defs) { defStuff(defs, glyphParams, glyphData); },
        defBadges: function (defs) { defStuff(defs, badgeParams, badgeData); }
    });

}(ONOS));
