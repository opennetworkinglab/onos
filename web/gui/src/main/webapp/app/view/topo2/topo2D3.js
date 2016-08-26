/*
* Copyright 2016-present Open Networking Laboratory
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
ONOS GUI -- Topology Layout Module.
Module that contains the d3.force.layout logic
*/

(function () {
    'use strict';

    var sus, is, ts;

    // internal state
    var deviceLabelIndex = 0,
    hostLabelIndex = 0;

    // configuration
    var devIconDim = 36,
        labelPad = 4,
        hostRadius = 14,
        badgeConfig = {
            radius: 12,
            yoff: 5,
            gdelta: 10
        },
        halfDevIcon = devIconDim / 2,
        devBadgeOff = { dx: -halfDevIcon, dy: -halfDevIcon },
        hostBadgeOff = { dx: -hostRadius, dy: -hostRadius },
        status = {
            i: 'badgeInfo',
            w: 'badgeWarn',
            e: 'badgeError'
        };

    // note: these are the device icon colors without affinity (no master)
    var dColTheme = {
        light: {
            online: '#444444',
            offline: '#cccccc'
        },
        dark: {
            // TODO: theme
            online: '#444444',
            offline: '#cccccc'
        }
    };

    function init() {}

    function renderBadge(node, bdg, boff) {
        var bsel,
            bcr = badgeConfig.radius,
            bcgd = badgeConfig.gdelta;

        node.select('g.badge').remove();

        bsel = node.append('g')
            .classed('badge', true)
            .classed(badgeStatus(bdg), true)
            .attr('transform', sus.translate(boff.dx, boff.dy));

        bsel.append('circle')
            .attr('r', bcr);

        if (bdg.txt) {
            bsel.append('text')
                .attr('dy', badgeConfig.yoff)
                .attr('text-anchor', 'middle')
                .text(bdg.txt);
        } else if (bdg.gid) {
            bsel.append('use')
                .attr({
                    width: bcgd * 2,
                    height: bcgd * 2,
                    transform: sus.translate(-bcgd, -bcgd),
                    'xlink:href': '#' + bdg.gid
                });
        }
    }

    // TODO: Move to Device Model when working on the Exit Devices
    function updateDeviceRendering(d) {
        var node = d.el,
            bdg = d.badge,
            label = trimLabel(deviceLabel(d)),
            labelWidth;

        node.select('text').text(label);
        labelWidth = label ? computeLabelWidth(node) : 0;

        node.select('rect')
            .transition()
            .attr(iconBox(devIconDim, labelWidth));

        if (bdg) {
            renderBadge(node, bdg, devBadgeOff);
        }
    }

    function nodeEnter(node) {
        node.onEnter(this, node);
    }

    function hostLabel(d) {
        return d.get('id');

        // var idx = (hostLabelIndex < d.get('labels').length) ? hostLabelIndex : 0;
        // return d.labels[idx];
    }

    function hostEnter(d) {
        var node = d3.select(this),
            gid = d.get('type') || 'unknown',
            textDy = hostRadius + 10;

        d.el = node;
        // sus.visible(node, api.showHosts());

        is.addHostIcon(node, hostRadius, gid);

        node.append('text')
            .text(hostLabel)
            .attr('dy', textDy)
            .attr('text-anchor', 'middle');
    }

    function linkEntering(link) {
        link.onEnter(this);
    }

    angular.module('ovTopo2')
    .factory('Topo2D3Service',
    ['SvgUtilService', 'IconService', 'ThemeService',

        function (_sus_, _is_, _ts_) {
            sus = _sus_;
            is = _is_;
            ts = _ts_;

            return {
                init: init,
                nodeEnter: nodeEnter,
                hostEnter: hostEnter,
                linkEntering: linkEntering
            }
        }
    ]
);
})();
