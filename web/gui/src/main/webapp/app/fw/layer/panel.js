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
 ONOS GUI -- Layer -- Panel Service
 */
(function () {
    'use strict';

    var $log, fs;

    var defaultSettings = {
        edge: 'right',
        width: 200,
        margin: 20,
        hideMargin: 20,
        xtnTime: 750,
        fade: true,
    };

    var panels,
        panelLayer;


    function init() {
        panelLayer = d3.select('#floatpanels');
        panelLayer.text('');
        panels = {};
    }

    // helpers for panel
    function noop() {}

    function margin(p) {
        return p.settings.margin;
    }
    function hideMargin(p) {
        return p.settings.hideMargin;
    }
    function noPx(p, what) {
        return Number(p.el.style(what).replace(/px$/, ''));
    }
    function widthVal(p) {
        return noPx(p, 'width');
    }
    function heightVal(p) {
        return noPx(p, 'height');
    }
    function pxShow(p) {
        return margin(p) + 'px';
    }
    function pxHide(p) {
        return (-hideMargin(p) - widthVal(p) - (noPx(p, 'padding') * 2)) + 'px';
    }

    function makePanel(id, settings) {
        var p = {
                id: id,
                settings: settings,
                on: false,
                el: null,
            },
            api = {
                show: showPanel,
                hide: hidePanel,
                toggle: togglePanel,
                empty: emptyPanel,
                append: appendPanel,
                width: panelWidth,
                height: panelHeight,
                bbox: panelBBox,
                isVisible: panelIsVisible,
                classed: classed,
                el: panelEl,
            };

        p.el = panelLayer.append('div')
            .attr('id', id)
            .attr('class', 'floatpanel')
            .style('opacity', 0);

        // has to be called after el is set
        p.el.style(p.settings.edge, pxHide(p));
        panelWidth(p.settings.width);
        if (p.settings.height) {
            panelHeight(p.settings.height);
        }

        panels[id] = p;

        function showPanel(cb) {
            var endCb = fs.isF(cb) || noop;
            p.on = true;
            p.el.transition().duration(p.settings.xtnTime)
                .each('end', endCb)
                .style(p.settings.edge, pxShow(p))
                .style('opacity', 1);
        }

        function hidePanel(cb) {
            var endCb = fs.isF(cb) || noop,
                endOpacity = p.settings.fade ? 0 : 1;
            p.on = false;
            p.el.transition().duration(p.settings.xtnTime)
                .each('end', endCb)
                .style(p.settings.edge, pxHide(p))
                .style('opacity', endOpacity);
        }

        function togglePanel(cb) {
            if (p.on) {
                hidePanel(cb);
            } else {
                showPanel(cb);
            }
            return p.on;
        }

        function emptyPanel() {
            return p.el.text('');
        }

        function appendPanel(what) {
            return p.el.append(what);
        }

        function panelWidth(w) {
            if (w === undefined) {
                return widthVal(p);
            }
            p.el.style('width', w + 'px');
        }

        function panelHeight(h) {
            if (h === undefined) {
                return heightVal(p);
            }
            p.el.style('height', h + 'px');
        }

        function panelBBox() {
            return p.el.node().getBoundingClientRect();
        }

        function panelIsVisible() {
            return p.on;
        }

        function classed(cls, bool) {
            return p.el.classed(cls, bool);
        }

        function panelEl() {
            return p.el;
        }

        return api;
    }

    function removePanel(id) {
        panelLayer.select('#' + id).remove();
        delete panels[id];
    }

    angular.module('onosLayer')
    .factory('PanelService',
        ['$log', '$window', 'FnService',

        function (_$log_, _$window_, _fs_) {
            $log = _$log_;
            fs = _fs_;

            function createPanel(id, opts) {
                var settings = angular.extend({}, defaultSettings, opts);
                if (!id) {
                    $log.warn('createPanel: no ID given');
                    return null;
                }
                if (panels[id]) {
                    $log.warn('Panel with ID "' + id + '" already exists');
                    return null;
                }
                if (fs.debugOn('widget')) {
                    $log.debug('creating panel:', id, settings);
                }
                return makePanel(id, settings);
            }

            function destroyPanel(id) {
                if (panels[id]) {
                    if (fs.debugOn('widget')) {
                        $log.debug('destroying panel:', id);
                    }
                    removePanel(id);
                } else {
                    if (fs.debugOn('widget')) {
                        $log.debug('no panel to destroy:', id);
                    }
                }
            }

            return {
                init: init,
                createPanel: createPanel,
                destroyPanel: destroyPanel,
            };
        }]);
}());
