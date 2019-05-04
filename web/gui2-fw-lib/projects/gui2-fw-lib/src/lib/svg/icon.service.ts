/*
 * Copyright 2018-present Open Networking Foundation
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
import { Injectable } from '@angular/core';
import { GlyphService } from './glyph.service';
import { LogService } from '../log.service';
import { SvgUtilService } from './svgutil.service';
import * as d3 from 'd3';

const vboxSize = 50;
const cornerSize = vboxSize / 10;
const viewBox = '0 0 ' + vboxSize + ' ' + vboxSize;

export const glyphMapping = new Map<string, string>([
    // Maps icon ID to the glyph ID it uses.
    // NOTE: icon ID maps to a CSS class for styling that icon
    ['active', 'checkMark'],
    ['inactive', 'xMark'],

    ['plus', 'plus'],
    ['minus', 'minus'],
    ['play', 'play'],
    ['stop', 'stop'],

    ['upload', 'upload'],
    ['download', 'download'],
    ['delta', 'delta'],
    ['nonzero', 'nonzero'],
    ['close', 'xClose'],

    ['m_cloud', 'm_cloud'],
    ['m_map', 'm_map'],
    ['m_selectMap', 'm_selectMap'],
    ['thatsNoMoon', 'thatsNoMoon'],
    ['m_ports', 'm_ports'],
    ['m_switch', 'm_switch'],
    ['switch', 'm_switch'],
    ['m_roadm', 'm_roadm'],
    ['roadm', 'm_roadm'],
    ['m_router', 'm_router'],
    ['router', 'm_router'],
    ['m_uiAttached', 'm_uiAttached'],
    ['m_endstation', 'm_endstation'],
    ['endstation', 'm_endstation'],
    ['m_summary', 'm_summary'],
    ['m_details', 'm_details'],
    ['m_oblique', 'm_oblique'],
    ['m_filters', 'm_filters'],
    ['m_cycleLabels', 'm_cycleLabels'],
    ['m_cycleGridDisplay', 'm_cycleGridDisplay'],
    ['m_prev', 'm_prev'],
    ['m_next', 'm_next'],
    ['m_flows', 'm_flows'],
    ['m_allTraffic', 'm_allTraffic'],
    ['m_xMark', 'm_xMark'],
    ['m_resetZoom', 'm_resetZoom'],
    ['m_eqMaster', 'm_eqMaster'],
    ['m_unknown', 'm_unknown'],
    ['m_controller', 'm_controller'],
    ['m_eqMaster', 'm_eqMaster'],
    ['m_virtual', 'm_virtual'],
    ['m_other', 'm_other'],
    ['m_bgpSpeaker', 'm_bgpSpeaker'],
    ['bgpSpeaker', 'm_bgpSpeaker'],
    ['m_otn', 'm_otn'],
    ['otn', 'm_otn'],
    ['m_terminal_device', 'm_otn'],
    ['m_ols', 'm_roadm'],
    ['m_roadm_otn', 'm_roadm_otn'],
    ['roadm_otn', 'm_roadm_otn'],
    ['m_fiberSwitch', 'm_fiberSwitch'],
    ['fiber_switch', 'm_fiberSwitch'],
    ['m_microwave', 'm_microwave'],
    ['microwave', 'm_microwave'],
    ['m_relatedIntents', 'm_relatedIntents'],
    ['m_intentTraffic', 'm_intentTraffic'],
    ['m_firewall', 'm_firewall'],
    ['m_balancer', 'm_balancer'],
    ['m_ips', 'm_ips'],
    ['m_ids', 'm_ids'],
    ['m_olt', 'm_olt'],
    ['m_onu', 'm_onu'],
    ['m_swap', 'm_swap'],
    ['m_shortestGeoPath', 'm_shortestGeoPath'],
    ['m_source', 'm_source'],
    ['m_destination', 'm_destination'],
    ['m_topo', 'm_topo'],
    ['m_shortestPath', 'm_shortestPath'],
    ['m_disjointPaths', 'm_disjointPaths'],
    ['m_region', 'm_region'],
    ['virtual', 'cord'],

    ['topo', 'topo'],
    ['bird', 'bird'],

    ['refresh', 'refresh'],
    ['query', 'query'],
    ['garbage', 'garbage'],


    ['upArrow', 'triangleUp'],
    ['downArrow', 'triangleDown'],
    ['triangleLeft', 'triangleLeft'],
    ['triangleRight', 'triangleRight'],

    ['appInactive', 'unknown'],
    ['uiAttached', 'uiAttached'],

    ['node', 'node'],
    ['devIcon_SWITCH', 'switch'],
    ['devIcon_ROADM', 'roadm'],
    ['devIcon_OTN', 'otn'],

    ['portIcon_DEFAULT', 'm_ports'],

    ['meter', 'meterTable'], // TODO: m_meter icon?

    ['deviceTable', 'switch'],
    ['flowTable', 'flowTable'],
    ['portTable', 'portTable'],
    ['groupTable', 'groupTable'],
    ['meterTable', 'meterTable'],
    ['pipeconfTable', 'pipeconfTable'],

    ['hostIcon_endstation', 'endstation'],
    ['hostIcon_router', 'router'],
    ['hostIcon_bgpSpeaker', 'bgpSpeaker'],

    // navigation menu icons...
    ['nav_apps', 'bird'],
    ['nav_settings', 'cog'],
    ['nav_cluster', 'node'],
    ['nav_processors', 'allTraffic'],
    ['nav_partitions', 'unknown'],

    ['nav_topo', 'topo'],
    ['nav_topo2', 'm_cloud'],
    ['nav_devs', 'switch'],
    ['nav_links', 'ports'],
    ['nav_hosts', 'endstation'],
    ['nav_intents', 'relatedIntents'],
    ['nav_tunnels', 'ports'], // TODO: use tunnel glyph, when available
    ['nav_yang', 'yang'],
]);

/**
 * ONOS GUI -- SVG -- Icon Service
 */
@Injectable({
  providedIn: 'root',
})
export class IconService {

    constructor(
        private gs: GlyphService,
        private log: LogService,
        private sus: SvgUtilService
    ) {

        this.log.debug('IconService constructed');
    }

    ensureIconLibDefs() {
        const body = d3.select('body');
        let svg = body.select('svg#IconLibDefs');
        if (svg.empty()) {
            svg = body.append('svg').attr('id', 'IconLibDefs');
            svg.append('defs');
        }
        return svg.select('defs');
    }

    /**
     * Load an icon
     *
     * @param div A D3 selection of the '&lt;div&gt;' element into which icon should load
     * @param glyphId Identifies the glyph to use
     * @param size The dimension of icon in pixels. Defaults to 20.
     * @param installGlyph If truthy, will cause the glyph to be added to
     *      well-known defs element. Defaults to false.
     * @param svgClass The CSS class used to identify the SVG layer.
     *      Defaults to 'embeddedIcon'.
     */
    loadIcon(div, glyphId: string = 'unknown', size: number = 20, installGlyph: boolean = true, svgClass: string = 'embeddedIcon') {
        const dim = size || 20;
        const svgCls = svgClass || 'embeddedIcon';
        const gid = glyphId || 'unknown';
        let g;
        let svgIcon: any;

        if (installGlyph) {
            this.gs.loadDefs(this.ensureIconLibDefs(), [gid], true);
        }
        this.log.warn('loadEmbeddedIcon. install done');

        svgIcon = div
            .append('svg')
            .attr('class', svgCls)
            .attr('width', dim)
            .attr('height', dim)
            .attr('viewBox', viewBox);

        g = svgIcon.append('g')
            .attr('class', 'icon');

        g.append('rect')
            .attr('width', vboxSize)
            .attr('height', vboxSize)
            .attr('rx', cornerSize);

        g.append('use')
            .attr('width', vboxSize)
            .attr('height', vboxSize)
            .attr('class', 'glyph')
            .attr('xlink:href', '#' + gid);
    }

    /**
     * Load an icon by class.
     * @param div A D3 selection of the <DIV> element into which icon should load
     * @param iconCls The CSS class used to identify the icon
     * @param size The dimension of icon in pixels. Defaults to 20.
     * @param installGlyph If truthy, will cause the glyph to be added to
     *      well-known defs element. Defaults to false.
     * @param svgClass The CSS class used to identify the SVG layer.
     *      Defaults to 'embeddedIcon'.
     */
    loadIconByClass(div, iconCls: string, size: number, installGlyph: boolean, svgClass= 'embeddedIcon') {
        this.loadIcon(div, glyphMapping.get(iconCls), size, installGlyph, svgClass);
        div.select('svg g').classed(iconCls, true);
    }

    /**
     * Load an embedded icon.
     */
    loadEmbeddedIcon(div, iconCls: string, size: number) {
        this.loadIconByClass(div, iconCls, size, true);
    }

    /**
     * Load an icon only to the svg defs collection
     *
     * Note: This is added for use with IconComponent, where the icon's
     * svg element is defined in the component template (and not built
     * inline using d3 manipulation
     *
     * @param iconCls The icon class as a string
     */
    loadIconDef(iconCls: string): void {
        let glyphName: string = glyphMapping.get(iconCls);
        if (!glyphName) {
            glyphName = iconCls;
        }
        this.gs.loadDefs(this.ensureIconLibDefs(), [glyphName], true, [iconCls]);
        this.log.debug('icon definition', glyphName, 'added to defs as', iconCls);
    }


    /**
     * Add a device icon
     *
     * Adds a device glyph to the specified element.
     * Returns the D3 selection of the glyph (use) element.
     */
    addDeviceIcon(elem, glyphId, iconDim) {
        const gid = this.gs.glyphDefined(glyphId) ? glyphId : 'query';
        return elem.append('use').attr({
            'xlink:href': '#' + gid,
            width: iconDim,
            height: iconDim,
        });
    }

    addHostIcon(elem, radius, glyphId) {
        const dim = radius * 1.5;
        const xlate = -dim / 2;
        const g = elem.append('g')
                .attr('class', 'svgIcon hostIcon');

        g.append('circle').attr('r', radius);

        g.append('use').attr({
            'xlink:href': '#' + glyphId,
            width: dim,
            height: dim,
            transform: this.sus.translate([xlate], xlate),
        });
        return g;
    }

    registerIconMapping(iconId, glyphId) {
        if (glyphMapping[iconId]) {
            this.log.warn('Icon with id', iconId, 'already mapped. Ignoring.');
        } else {
            // map icon-->glyph
            glyphMapping[iconId] = glyphId;
            // make sure definition is installed
            this.gs.loadDefs(this.ensureIconLibDefs(), [glyphId], true);
        }
    }
}
