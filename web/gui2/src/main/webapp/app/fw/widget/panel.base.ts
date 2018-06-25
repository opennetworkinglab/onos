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
import { FnService } from '../util/fn.service';
import { LoadingService } from '../layer/loading.service';
import { LogService } from '../../log.service';
import { WebSocketService } from '../remote/websocket.service';


const noop = (): any => undefined;

/**
 ********* Static functions *********
 */
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


/**
 * Base model of panel view - implemented by Panel components
 */
export interface PanelBase {
    showPanel(cb: any): void;
    hidePanel(cb: any): void;
    togglePanel(cb: any): void;
    emptyPanel(): void;
    appendPanel(what: any): void;
    panelWidth(w: number): number;
    panelHeight(h: number): number;
    panelBBox(): string;
    panelIsVisible(): boolean;
    classed(cls: any, bool: boolean): boolean;
    panelEl(): any;
}

/**
 * ONOS GUI -- Widget -- Panel Base class
 *
 * Replacing the panel service in the old implementation
 */
export abstract class PanelBaseImpl implements PanelBase {

    protected on: boolean;
    protected el: any;

    constructor(
        protected fs: FnService,
        protected ls: LoadingService,
        protected log: LogService,
        protected wss: WebSocketService,
        protected settings: any
    ) {
//        this.log.debug('Panel base class constructed');
    }

    showPanel(cb) {
        const endCb = this.fs.isF(cb) || noop;
        this.on = true;
        this.el.transition().duration(this.settings.xtnTime)
            .each('end', endCb)
            .style(this.settings.edge, pxShow(this))
            .style('opacity', 1);
    }

    hidePanel(cb) {
        const endCb = this.fs.isF(cb) || noop;
        const endOpacity = this.settings.fade ? 0 : 1;
        this.on = false;
        this.el.transition().duration(this.settings.xtnTime)
            .each('end', endCb)
            .style(this.settings.edge, pxHide(this))
            .style('opacity', endOpacity);
    }

    togglePanel(cb): boolean {
        if (this.on) {
            this.hidePanel(cb);
        } else {
            this.showPanel(cb);
        }
        return this.on;
    }

    emptyPanel(): string {
        return this.el.text('');
    }

    appendPanel(what) {
        return this.el.append(what);
    }

    panelWidth(w: number): number {
        if (w === undefined) {
            return widthVal(this);
        }
        this.el.style('width', w + 'px');
    }

    panelHeight(h: number): number {
        if (h === undefined) {
            return heightVal(this);
        }
        this.el.style('height', h + 'px');
    }

    panelBBox(): string {
        return this.el.node().getBoundingClientRect();
    }

    panelIsVisible(): boolean {
        return this.on;
    }

    classed(cls, bool): boolean {
        return this.el.classed(cls, bool);
    }

    panelEl() {
        return this.el;
    }


    /**
     * A dummy implementation of the lionFn until the response is received and the LION
     * bundle is received from the WebSocket
     */
    dummyLion(key: string): string {
        return '%' + key + '%';
    }
}
