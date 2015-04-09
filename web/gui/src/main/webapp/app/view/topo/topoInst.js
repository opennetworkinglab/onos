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
 ONOS GUI -- Topology Instances Module.
 Defines modeling of ONOS instances.
 */

(function () {
    'use strict';

    // injected refs
    var $log, ps, sus, gs, ts, fs, flash;

    // api from topo
    var api;
    /*
        showMastership( id )
     */

    // configuration
    var instCfg = {
            rectPad: 8,
            nodeOx: 9,
            nodeOy: 9,
            nodeDim: 40,
            birdOx: 19,
            birdOy: 21,
            birdDim: 21,
            uiDy: 45,
            titleDy: 30,
            textYOff: 20,
            textYSpc: 15
        },
        showLogicErrors = true,
        idIns = 'topo-p-instance',
        instOpts = {
            edge: 'left',
            width: 20
        };

    // internal state
    var onosInstances,
        onosOrder,
        oiShowMaster,
        oiBox,
        themeListener;


    // ==========================

    function addInstance(data) {
        var id = data.id;

        if (onosInstances[id]) {
            updateInstance(data);
            return;
        }
        onosInstances[id] = data;
        onosOrder.push(data);
        updateInstances();
    }

    function updateInstance(data) {
        var id = data.id,
            d = onosInstances[id];
        if (d) {
            angular.extend(d, data);
            updateInstances();
        } else {
            logicError('updateInstance: lookup fail: ID = "' + id + '"');
        }
    }

    function removeInstance(data) {
        var id = data.id,
            d = onosInstances[id];
        if (d) {
            var idx = fs.find(id, onosOrder);
            if (idx >= 0) {
                onosOrder.splice(idx, 1);
            }
            delete onosInstances[id];
            updateInstances();
        } else {
            logicError('removeInstance lookup fail. ID = "' + id + '"');
        }
    }

    // ==========================

    function computeDim(self) {
        var css = window.getComputedStyle(self);
        return {
            w: sus.stripPx(css.width),
            h: sus.stripPx(css.height)
        };
    }

    function clickInst(d) {
        var el = d3.select(this),
            aff = el.classed('affinity');
        if (!aff) {
            setAffinity(el, d);
        } else {
            cancelAffinity();
        }
    }

    function setAffinity(el, d) {
        d3.selectAll('.onosInst')
            .classed('mastership', true)
            .classed('affinity', false);
        el.classed('affinity', true);

        // suppress all elements except nodes whose master is this instance
        api.showMastership(d.id);
        oiShowMaster = true;
    }

    function cancelAffinity() {
        d3.selectAll('.onosInst')
            .classed('mastership affinity', false);

        api.showMastership(null);
        oiShowMaster = false;
    }

    function instRectAttr(dim) {
        var pad = instCfg.rectPad;
        return {
            x: pad,
            y: pad,
            width: dim.w - pad*2,
            height: dim.h - pad*2,
            rx: 6
        };
    }

    function viewBox(dim) {
        return '0 0 ' + dim.w + ' ' + dim.h;
    }

    function attachUiBadge(svg) {
        gs.addGlyph(svg, 'uiAttached', 30, true, [12, instCfg.uiDy])
            .classed('badgeIcon uiBadge', true);
    }

    function instColor(id, online) {
        return sus.cat7().getColor(id, !online, ts.theme());
    }

    // ==============================

    function updateInstances() {
        var onoses = oiBox.el().selectAll('.onosInst')
                .data(onosOrder, function (d) { return d.id; }),
            instDim = {w:0,h:0},
            c = instCfg;

        function nSw(n) {
            return '# Switches: ' + n;
        }

        // operate on existing onos instances if necessary
        onoses.each(function (d) {
            var el = d3.select(this),
                svg = el.select('svg');
            instDim = computeDim(this);

            // update online state
            el.classed('online', d.online);

            // update ui-attached state
            svg.select('use.uiBadge').remove();
            if (d.uiAttached) {
                attachUiBadge(svg);
            }

            function updAttr(id, value) {
                svg.select('text.instLabel.'+id).text(value);
            }

            updAttr('ip', d.ip);
            updAttr('ns', nSw(d.switches));
        });


        // operate on new onos instances
        var entering = onoses.enter()
            .append('div')
            .attr('class', 'onosInst')
            .classed('online', function (d) { return d.online; })
            .on('click', clickInst);

        entering.each(function (d) {
            var el = d3.select(this),
                rectAttr,
                svg;
            instDim = computeDim(this);
            rectAttr = instRectAttr(instDim);

            svg = el.append('svg').attr({
                width: instDim.w,
                height: instDim.h,
                viewBox: viewBox(instDim)
            });

            svg.append('rect').attr(rectAttr);

            gs.addGlyph(svg, 'bird', 28, true, [14, 14])
                .classed('badgeIcon', true);

            if (d.uiAttached) {
                attachUiBadge(svg);
            }

            var left = c.nodeOx + c.nodeDim,
                len = rectAttr.width - left,
                hlen = len / 2,
                midline = hlen + left;

            // title
            svg.append('text')
                .attr({
                    class: 'instTitle',
                    x: midline,
                    y: c.titleDy
                })
                .text(d.id);

            // a couple of attributes
            var ty = c.titleDy + c.textYOff;

            function addAttr(id, label) {
                svg.append('text').attr({
                    class: 'instLabel ' + id,
                    x: midline,
                    y: ty
                }).text(label);
                ty += c.textYSpc;
            }

            addAttr('ip', d.ip);
            addAttr('ns', nSw(d.switches));
        });

        // operate on existing + new onoses here
        // set the affinity colors...
        onoses.each(function (d) {
            var el = d3.select(this),
                rect = el.select('svg').select('rect'),
                col = instColor(d.id, d.online);
            rect.style('fill', col);
        });

        // adjust the panel size appropriately...
        oiBox.width(instDim.w * onosOrder.length);
        oiBox.height(instDim.h);

        // remove any outgoing instances
        onoses.exit().remove();
    }


    // ==========================

    function logicError(msg) {
        if (showLogicErrors) {
            $log.warn('TopoInstService: ' + msg);
        }
    }

    function initInst(_api_) {
        api = _api_;
        oiBox = ps.createPanel(idIns, instOpts);
        oiBox.show();

        onosInstances = {};
        onosOrder = [];
        oiShowMaster = false;

        // we want to update the instances, each time the theme changes
        themeListener = ts.addListener(updateInstances);
    }

    function destroyInst() {
        ts.removeListener(themeListener);
        themeListener = null;

        ps.destroyPanel(idIns);
        oiBox = null;

        onosInstances = {};
        onosOrder = [];
        oiShowMaster = false;
    }

    function showInsts() {
        oiBox.show();
    }

    function hideInsts() {
        oiBox.hide();
    }

    function toggleInsts(x) {
        var kev = (x === 'keyev'),
            on,
            verb;

        if (kev) {
            on = oiBox.toggle();
        } else {
            on = !!x;
            if (on) {
                showInsts();
            } else {
                hideInsts();
            }
        }
        verb = on ? 'Show' : 'Hide';
        flash.flash(verb + ' instances panel');
        return on;
    }

    // ==========================

    angular.module('ovTopo')
    .factory('TopoInstService',
        ['$log', 'PanelService', 'SvgUtilService', 'GlyphService',
            'ThemeService', 'FnService', 'FlashService',

        function (_$log_, _ps_, _sus_, _gs_, _ts_, _fs_, _flash_) {
            $log = _$log_;
            ps = _ps_;
            sus = _sus_;
            gs = _gs_;
            ts = _ts_;
            fs = _fs_;
            flash = _flash_;

            return {
                initInst: initInst,
                destroyInst: destroyInst,

                addInstance: addInstance,
                updateInstance: updateInstance,
                removeInstance: removeInstance,

                cancelAffinity: cancelAffinity,

                isVisible: function () { return oiBox.isVisible(); },
                show: showInsts,
                hide: hideInsts,
                toggle: toggleInsts,
                showMaster: function () { return oiShowMaster; }
            };
        }]);
}());
