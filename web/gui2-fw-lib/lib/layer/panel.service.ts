/*
 *  Copyright 2018-present Open Networking Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
import {Injectable} from '@angular/core';
import {FnService} from '../util/fn.service';
import {LogService} from '../log.service';
import {ThemeService} from '../util/theme.service';
import {WebSocketService} from '../remote/websocket.service';
import * as d3 from 'd3';

let fs;

const defaultSettings = {
    edge: 'right',
    width: 200,
    margin: 20,
    hideMargin: 20,
    xtnTime: 750,
    fade: true,
};

let panels,
    panelLayer;

function init() {
    panelLayer = d3.select('div#floatpanels');
    panelLayer.text('');
    panels = {};
}

// helpers for panel
function noop() {
}

function margin(p: any) {
    return p.settings.margin;
}

function hideMargin(p: any) {
    return p.settings.hideMargin;
}

function noPx(p: any, what: any) {
    return Number(p.el.style(what).replace(/px$/, ''));
}

function widthVal(p: any) {
    return noPx(p, 'width');
}

function heightVal(p: any) {
    return noPx(p, 'height');
}

function pxShow(p: any) {
    return margin(p) + 'px';
}

function pxHide(p: any) {
    return (-hideMargin(p) - widthVal(p) - (noPx(p, 'padding') * 2)) + 'px';
}

function makePanel(id: any, settings: any) {
    const p = {
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

    function showPanel(cb: any) {
        const endCb = fs.isF(cb) || noop;
        p.on = true;
        p.el.transition().duration(p.settings.xtnTime)
            .style(p.settings.edge, pxShow(p))
            .style('opacity', 1);
    }

    function hidePanel(cb: any) {
        const endCb = fs.isF(cb) || noop,
            endOpacity = p.settings.fade ? 0 : 1;
        p.on = false;
        p.el.transition().duration(p.settings.xtnTime)
            .style(p.settings.edge, pxHide(p))
            .style('opacity', endOpacity);
    }

    function togglePanel(cb: any) {
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

    function appendPanel(what: any) {
        return p.el.append(what);
    }

    function panelWidth(w: any) {
        if (w === undefined) {
            return widthVal(p);
        }
        p.el.style('width', w + 'px');
    }

    function panelHeight(h: any) {
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

    function classed(cls: any, bool: any) {
        return p.el.classed(cls, bool);
    }

    function panelEl() {
        return p.el;
    }

    return api;
}

function removePanel(id: any) {
    panelLayer.select('#' + id).remove();
    delete panels[id];
}

@Injectable({
    providedIn: 'root',
})
export class PanelService {
    constructor(private funcs: FnService,
                private log: LogService,
                private ts: ThemeService,
                private wss: WebSocketService) {
        fs = this.funcs;
        init();
    }

    createPanel(id: any, opts: any) {
        const settings = (<any>Object).assign({}, defaultSettings, opts);
        if (!id) {
            this.log.warn('createPanel: no ID given');
            return null;
        }
        if (panels[id]) {
            this.log.warn('Panel with ID "' + id + '" already exists');
            return null;
        }
        if (fs.debugOn('widget')) {
            this.log.debug('creating panel:', id, settings);
        }
        return makePanel(id, settings);
    }

    destroyPanel(id: any) {
        if (panels[id]) {
            if (fs.debugOn('widget')) {
                this.log.debug('destroying panel:', id);
            }
            removePanel(id);
        } else {
            if (fs.debugOn('widget')) {
                this.log.debug('no panel to destroy:', id);
            }
        }
    }
}
