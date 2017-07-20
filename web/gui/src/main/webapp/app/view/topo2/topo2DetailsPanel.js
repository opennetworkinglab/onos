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
 ONOS GUI -- Topology View Module.
 Module that displays the details panel for selected nodes
 */

(function () {
    'use strict';

    // TODO: Topo2Panel - A view that draws the container for Summary and Details Panels
    // TODO: as well as the show/hide/toggle methods.

    // TODO: Topo2DetailsPanel should extend Topo2Panel and add methods to controller position
    // TODO: based on the visibility of Topo2Summary

    // TODO: Topo2<Device|Link|Host>Panel should only be concerned with the content displayed

    // Injected Services
    var Panel;

    // Internal State
    var detailsPanel, summaryPanel;

    // configuration
    var id = 'topo2-p-detail',
        className = 'topo2-p',
        transTime = 750,
        panelPadding = 64,
        panelSpacing = 20,
        panelOpts = {
            width: 260          // summary and detail panel width
        };

    function getInstance(_summaryPanel_) {
        if (detailsPanel) {
            return detailsPanel;
        }

        summaryPanel = _summaryPanel_;

        var options = angular.extend({}, panelOpts, {
            class: className
        });

        Panel = Panel.extend({
            up: function () {
                detailsPanel.el.el()
                    .transition()
                    .duration(transTime)
                    .style('top', panelPadding + 'px');
            },
            down: function (position, callback) {
                detailsPanel.el.el()
                    .transition()
                    .duration(transTime)
                    .style('top', position + (panelPadding + panelSpacing) + 'px')
                    .each('end', callback);
            },
            show: function () {

                var summaryInstance = summaryPanel.getInstance(),
                    position = 0;

                if (summaryInstance.isVisible()) {
                    position = summaryInstance.el.bbox().height;
                    position = position + panelSpacing;
                }

                detailsPanel.el.el()
                    .style('top', panelPadding + position + 'px');
                detailsPanel.el.show();
            }
        });

        detailsPanel = new Panel(id, options);
        detailsPanel.el.classed(className, true);

        return detailsPanel;
    }



    angular.module('ovTopo2')
    .factory('Topo2DetailsPanelService', [
        'Topo2PanelService',
        function (_ps_) {
            Panel = _ps_;

            return getInstance;
        }
    ]);
})();
