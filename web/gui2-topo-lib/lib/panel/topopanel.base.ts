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
import {
    FnService,
    LogService,
    PanelBaseImpl
} from 'org_onosproject_onos/web/gui2-fw-lib/public_api';

/**
 * Base model of panel view - implemented by Topology Panel components
 */
export abstract class TopoPanelBaseImpl extends PanelBaseImpl {

    protected header: any;
    protected body: any;
    protected footer: any;

    protected constructor(
        protected fs: FnService,
        protected log: LogService,
        protected id: string
    ) {
        super(fs, log);
    }

    protected init(el: any) {
        this.header = el.append('div').classed('header', true);
        this.body = el.append('div').classed('body', true);
        this.footer = el.append('div').classed('footer', true);
    }

    /**
     * Decode lists of props sent back through Web Socket
     *
     * Means that panels do not have to know property names in advance
     * Driven by PropertyPanel on Server side
     */
    listProps(el, data) {
        let sepLast: boolean = false;

        // note: track whether we end with a separator or not...
        data.propOrder.forEach((p) => {
            if (p === '-') {
                this.addSep(el);
                sepLast = true;
            } else {
                this.addProp(el, data.propLabels[p], data.propValues[p]);
                sepLast = false;
            }
        });
        return sepLast;
    }

    addProp(el, label, value) {
        const tr = el.append('tr');
        let lab;

        if (typeof label === 'string') {
            lab = label.replace(/_/g, ' ');
        } else {
            lab = label;
        }

        function addCell(cls, txt) {
            tr.append('td').attr('class', cls).text(txt);
        }

        addCell('label', lab);
        addCell('value', value);
    }

    addSep(el) {
        el.append('tr').append('td').attr('colspan', 2).append('hr');
    }

    appendToHeader(x) {
        return this.header.append(x);
    }

    appendToBody(x) {
        return this.body.append(x);
    }

    appendToFooter(x) {
        return this.footer.append(x);
    }

    emptyRegions() {
        this.header.selectAll('*').remove();
        this.body.selectAll('*').remove();
        this.footer.selectAll('*').remove();
    }

}
