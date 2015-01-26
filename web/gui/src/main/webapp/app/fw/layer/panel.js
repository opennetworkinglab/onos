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
 ONOS GUI -- Layer -- Panel Service
 */
(function () {
    'use strict';

    var $log, fs;

    var defaultSettings = {
        edge: 'right',
        width: 200,
        height: 80,
        margin: 20,
        xtnTime: 750
    };

    var panels,
        panelLayer;


    function init() {
        panelLayer = d3.select('#floatpanels');
        panelLayer.html('');
        panels = {};
    }

    // helpers for panel
    function noop() {}

    function margin(p) {
        return p.settings.margin;
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
        return (-margin(p) - widthVal(p)) + 'px';
    }

    function makePanel(id, settings) {
        var p = {
                id: id,
                settings: settings,
                on: false,
                el: null
            },
            api = {
                show: showPanel,
                hide: hidePanel,
                empty: emptyPanel,
                append: appendPanel,
                width: panelWidth,
                height: panelHeight,
                isVisible: panelIsVisible
            };

        p.el = panelLayer.append('div')
            .attr('id', id)
            .attr('class', 'floatpanel')
            .style('opacity', 0);

        // has to be called after el is set
        p.el.style(p.settings.edge, pxHide(p));
        panelWidth(p.settings.width);
        panelHeight(p.settings.height);

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
            var endCb = fs.isF(cb) || noop;
            p.on = false;
            p.el.transition().duration(p.settings.xtnTime)
                .each('end', endCb)
                .style(p.settings.edge, pxHide(p))
                .style('opacity', 0);
        }

        function emptyPanel() {
            return p.el.html('');
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

        function panelIsVisible() {
            return p.on;
        }

        return api;
    }

    function removePanel(id) {
        panelLayer.select('#' + id).remove();
        delete panels[id];
    }

    angular.module('onosLayer')
        .factory('PanelService', ['$log', 'FnService', function (_$log_, _fs_) {
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
                $log.debug('creating panel:', id, settings);
                return makePanel(id, settings);
            }

            function destroyPanel(id) {
                if (panels[id]) {
                    $log.debug('destroying panel:', id);
                    removePanel(id);
                } else {
                    $log.debug('no panel to destroy:', id);
                }
            }

            return {
                init: init,
                createPanel: createPanel,
                destroyPanel: destroyPanel
            };
        }]);

}());
