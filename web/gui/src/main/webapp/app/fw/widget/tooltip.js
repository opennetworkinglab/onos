/*
 * Copyright 2015-present Open Networking Foundation
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
 ONOS GUI -- Widget -- Tooltip Service
 */

(function () {
    'use strict';

    // injected references
    var $rootScope, fs;

    // constants
    var hoverHeight = 35,
        hoverDelay = 150,
        exitDelay = 150;

    // internal state
    var tooltip, currElemId;

    // === Helper functions ---------------------------------------------

    function init() {
        tooltip = d3.select('#tooltip');
        tooltip.text('');
    }

    function tipStyle(mouseX, mouseY) {
        var winWidth = fs.windowSize().width,
            winHeight = fs.windowSize().height,
            style = {
                display: 'inline-block',
                left: 'auto',
                right: 'auto',
            };

        if (mouseX <= (winWidth / 2)) {
            style.left = mouseX + 'px';
        } else {
            style.right = (winWidth - mouseX) + 'px';
        }

        if (mouseY <= (winHeight / 2)) {
            style.top = (mouseY + (hoverHeight - 10)) + 'px';
        } else {
            style.top = (mouseY - hoverHeight) + 'px';
        }

        return style;
    }

    // === API functions ------------------------------------------------

    function addTooltip(elem, tooltip) {
        elem.on('mouseover', function () { showTooltip(this, tooltip); });
        elem.on('mouseout', function () { cancelTooltip(this); });
        $rootScope.$on('$routeChangeStart', function () {
            cancelTooltip(elem.node());
        });
    }

    function showTooltip(el, msg) {
        // tooltips don't make sense on mobile devices
        if (!el || !msg || fs.isMobile()) {
            return;
        }

        var elem = d3.select(el),
            mouseX = d3.event.pageX,
            mouseY = d3.event.pageY,
            style = tipStyle(mouseX, mouseY),
            ttMsg = fs.isF(msg) ? msg() : msg;

        currElemId = elem.attr('id');

        tooltip.transition()
            .delay(hoverDelay)
            .each('start', function () {
                d3.select(this).style('display', 'none');
            })
            .each('end', function () {
                d3.select(this).style(style)
                    .text(ttMsg);
            });
    }

    function cancelTooltip(el) {
        if (!el) {
            return;
        }
        var elem = d3.select(el);

        if (elem.attr('id') === currElemId) {
            tooltip.transition()
                .delay(exitDelay)
                .style({
                    display: 'none',
                })
                .text('');
        }
    }

    angular.module('onosWidget')

        .directive('tooltip', ['$rootScope', 'FnService',
            function (_$rootScope_, _fs_) {
                $rootScope = _$rootScope_;
                fs = _fs_;

                init();

                return {
                    restrict: 'A',
                    link: function (scope, elem, attrs) {
                        addTooltip(d3.select(elem[0]), scope[attrs.ttMsg]);
                    },
                };
        }])

        .factory('TooltipService', ['$rootScope', 'FnService',
            function (_$rootScope_, _fs_) {
                $rootScope = _$rootScope_;
                fs = _fs_;

                init();

                return {
                    addTooltip: addTooltip,
                    showTooltip: showTooltip,
                    cancelTooltip: cancelTooltip,
                };
            }]);
}());
