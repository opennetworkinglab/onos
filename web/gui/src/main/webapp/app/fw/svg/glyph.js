/*
 * Copyright 2015 Open Networking Laboratory
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
 ONOS GUI -- SVG -- Glyph Service

 @author Simon Hunt
 */
(function () {
    'use strict';

    var $log,
        glyphs = d3.map();

    angular.module('onosSvg')
        .factory('GlyphService', ['$log', function (_$log_) {
            $log = _$log_;


            function init() {
                // TODO: load the core set of glyphs

            }

            function register(viewBox, data, overwrite) {
                // TODO: register specified glyph definitions

            }

            function ids() {
                return glyphs.keys();
            }

            function loadDefs(defs) {
                // TODO: clear defs element, then load all glyph definitions

            }

            return {
                init: init,
                register: register,
                ids: ids,
                loadDefs: loadDefs
            };
        }]);

}());
