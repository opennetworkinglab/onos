/*
 * Copyright 2018-present Open Networking Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the 'License');
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an 'AS IS' BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import { TableFilterPipe } from './tablefilter.pipe';
import { TableFilter } from './table.base';

describe('TableFilterPipe', () => {

    const pipe = new TableFilterPipe();
    const items: any[] = new Array();
    // Array item 0
    items.push({
        id: 'abc',
        title: 'def',
        origin: 'ghi'
    });
    // Array item 1
    items.push({
        id: 'pqr',
        title: 'stu',
        origin: 'vwx'
    });
    // Array item 2
    items.push({
        id: 'dog',
        title: 'mouse',
        origin: 'cat'
    });


    it('create an instance', () => {
        expect(pipe).toBeTruthy();
    });

    it('expect it to handle empty search', () => {
        const filteredItems: any[] =
            pipe.transform(items, <TableFilter>{queryStr: '', queryBy: 'title'});
        expect(filteredItems).toEqual(items);
    });

    it('expect it to handle empty items', () => {
        const filteredItems: any[] =
            pipe.transform(new Array(), <TableFilter>{queryStr: 'de', queryBy: 'title'});
        expect(filteredItems).toEqual(new Array());
    });


    it('expect it to match 0 by title', () => {
        const filteredItems: any[] =
            pipe.transform(items, <TableFilter>{queryStr: 'de', queryBy: 'title'});
        expect(filteredItems).toEqual(items.slice(0, 1));
    });

    it('expect it to match 1 by title', () => {
        const filteredItems: any[] =
            pipe.transform(items, <TableFilter>{queryStr: 'st', queryBy: 'title'});
        expect(filteredItems).toEqual(items.slice(1, 2));
    });

    it('expect it to match 1 by uppercase title', () => {
        const filteredItems: any[] =
            pipe.transform(items, <TableFilter>{queryStr: 'sT', queryBy: 'title'});
        expect(filteredItems).toEqual(items.slice(1, 2));
    });

    it('expect it to not match by title', () => {
        const filteredItems: any[] =
            pipe.transform(items, <TableFilter>{queryStr: 'pq', queryBy: 'title'});
        expect(filteredItems.length).toEqual(0);
    });

    it('expect it to match 1 by all fields', () => {
        const filteredItems: any[] =
            pipe.transform(items, <TableFilter>{queryStr: 'pq', queryBy: '$'});
        expect(filteredItems).toEqual(items.slice(1, 2));
    });

    it('expect it to not match by all fields', () => {
        const filteredItems: any[] =
            pipe.transform(items, <TableFilter>{queryStr: 'yz', queryBy: '$'});
        expect(filteredItems.length).toEqual(0);
    });

    /**
     * Check that items one and two contain a 't' - title=stu and origin=cat
     */
    it('expect it to match 1,2 by all fields', () => {
        const filteredItems: any[] =
            pipe.transform(items, <TableFilter>{queryStr: 't', queryBy: '$'});
        expect(filteredItems).toEqual(items.slice(1));
    });
});
