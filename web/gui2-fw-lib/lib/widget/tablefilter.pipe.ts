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
import { Pipe, PipeTransform } from '@angular/core';
import { TableFilter } from './table.base';

/**
 * Only return the tabledata that matches filtering with some queries
 *
 * Note: the pipe is marked pure here as we need to filter on the
 * content of the filter object (it's not a primitive type)
 */
@Pipe({
  name: 'filter',
  pure: false
})
export class TableFilterPipe implements PipeTransform {

    /**
     * From an array of table items just return those that match the filter
     */
    transform(items: any[], tableDataFilter: TableFilter): any[] {
        if (!items) {
            return [];
        }
        if (!tableDataFilter.queryStr) {
            return items;
        }

        const queryStr = tableDataFilter.queryStr.toLowerCase();

        return items.filter( it => {
            if (tableDataFilter.queryBy === '$') {
                const t1 = (<any>Object).values(it);
                const t2 = (<any>Object).values(it).filter(value => {
                               return JSON.stringify(value).toLowerCase().indexOf(queryStr) !== -1;
                           });
                return (<any>Object).values(it).filter(value => {
                    return JSON.stringify(value).toLowerCase().indexOf(queryStr) !== -1;
                }).length > 0;
            } else {
                return it[tableDataFilter.queryBy].toLowerCase().includes(queryStr);
            }
        });
    }
}
