/*
 * Copyright 2017-present Open Networking Foundation
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
package org.onosproject.store.service;

import com.google.common.annotations.Beta;

/**
 * Cross-version storage/coordination service.
 * <p>
 * This is a special type of {@link PrimitiveService} that differs semantically from {@link StorageService} in that
 * it supports cross-version backward/forward compatible storage. During upgrades, when nodes are running different
 * versions of the software, this service guarantees that cross-version compatibility will be maintained and provides
 * shared compatible primitives for coordinating across versions. Users must ensure that all objects stored in
 * primitives created via this service are stored using a serialization format that is backward/forward compatible,
 * e.g. using {@link org.onlab.util.KryoNamespace.Builder#setCompatible(boolean)}.
 *
 * @see org.onlab.util.KryoNamespace.Builder#setCompatible(boolean)
 * @see StorageService
 */
@Beta
public interface CoordinationService extends PrimitiveService {
}
