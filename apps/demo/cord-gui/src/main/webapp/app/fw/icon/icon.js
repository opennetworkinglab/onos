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

(function () {
    'use strict';

    angular.module('cordGui')

        .directive('icon', [function () {
            return {
                restrict: 'E',
                compile: function (element, attrs) {
                    var html =
                        '<svg class="embedded-icon" width="' + attrs.size + '" ' +
                        'height="' + attrs.size + '" viewBox="0 0 50 50">' +
                            '<g class="icon">' +
                                '<circle cx="25" cy="25" r="25"></circle>' +
                                '<use width="50" height="50" class="glyph '
                                + attrs.id + '" xlink:href="#' + attrs.id +
                                '"></use>' +
                            '</g>' +
                        '</svg>';
                    element.replaceWith(html);
                }
            };
        }]);
}());
