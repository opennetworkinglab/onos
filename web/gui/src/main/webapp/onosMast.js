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
 ONOS GUI -- Masthead script

 Defines the masthead for the UI. Injects logo and title, as well as providing
 the placeholder for a set of radio buttons.

 @author Simon Hunt
 */

(function (onos){
    'use strict';

    // API's
    var api = onos.api;

    // Config variables
    var guiTitle = 'Open Network Operating System';

    // DOM elements and the like
    var mast = d3.select('#mast');

    mast.append('img')
        .attr({
            id: 'logo',
            src: 'img/onos-logo.png'
        });

    mast.append('span')
        .attr({
            class: 'title'
        })
        .text(guiTitle);

    mast.append('span')
        .attr({
            id: 'mastRadio',
            class: 'right'
        });

}(ONOS));
