/*
 * Copyright 2015-present Open Networking Laboratory
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
package org.onosproject.incubator.net.config.basics;

import com.google.common.annotations.Beta;
import org.onosproject.incubator.net.domain.IntentDomainId;
import org.onosproject.net.config.SubjectFactory;

/**
 * Set of subject factories for potential configuration subjects.
 */
@Beta
public final class ExtraSubjectFactories {

    // Construction forbidden
    private ExtraSubjectFactories() {
    }

    public static final SubjectFactory<IntentDomainId> INTENT_DOMAIN_SUBJECT_FACTORY =
            new SubjectFactory<IntentDomainId>(IntentDomainId.class, "domains") {
                @Override
                public IntentDomainId createSubject(String key) {
                    return IntentDomainId.valueOf(key);
                }
            };

}
