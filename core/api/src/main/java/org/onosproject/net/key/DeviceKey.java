/*
 * Copyright 2016-present Open Networking Foundation
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

package org.onosproject.net.key;

import com.google.common.annotations.Beta;
import org.onosproject.net.AbstractAnnotated;
import org.onosproject.net.AnnotationKeys;
import org.onosproject.net.Annotations;
import org.onosproject.net.DefaultAnnotations;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static org.onosproject.net.DefaultAnnotations.builder;

/**
 * Abstraction of a device key.
 */
@Beta
public class DeviceKey extends AbstractAnnotated {

    private static final int LABEL_MAX_LENGTH = 1024;

    // device key identifier
    private final DeviceKeyId deviceKeyId;
    // label of the device key
    private String label;

    /**
     * type of the device key.
     */
    public enum Type {
        COMMUNITY_NAME, USERNAME_PASSWORD, SSL_KEY
    }

    private Type type;

    /**
     * Constructor for serialization.
     */
    DeviceKey() {
        this.deviceKeyId = null;
        this.label = null;
        this.type = null;
    }

    /**
     * Private constructor for a device key.
     *
     * @param id          device key identifier
     * @param label       optional label for this device key
     * @param type        to be assigned to this device key
     * @param annotations name/value pairs for this device key
     */
    private DeviceKey(DeviceKeyId id, String label, Type type, Annotations... annotations) {
        super(annotations);
        checkNotNull(id, "The DeviceKeyId cannot be null.");
        if (label != null) {
            checkArgument(label.length() <= LABEL_MAX_LENGTH, "label exceeds maximum length " + LABEL_MAX_LENGTH);
        }
        this.deviceKeyId = id;
        this.label = label;
        this.type = type;
    }

    /**
     * Returns the device key identifier of the device key.
     *
     * @return device key identifier
     */
    public DeviceKeyId deviceKeyId() {
        return deviceKeyId;
    }

    /**
     * Returns the label of device key.
     *
     * @return label
     */
    public String label() {
        return label;
    }

    /**
     * Returns the type of the device key.
     *
     * @return type
     */
    public Type type() {
        return type;
    }

    /**
     * Method to create a device key of type CommunityName.
     *
     * @param id    device key identifier
     * @param label optional label for this device key
     * @param name  community name for this device key
     * @return device key
     */
    public static DeviceKey createDeviceKeyUsingCommunityName(DeviceKeyId id, String label, String name) {
        DefaultAnnotations annotations = builder().set(AnnotationKeys.NAME, name).build();

        return new DeviceKey(id, label, Type.COMMUNITY_NAME, annotations);
    }

    /**
     * Returns a community name object from the device key.
     *
     * @return community name
     */
    public CommunityName asCommunityName() {

        // Validate that the device key is of type COMMUNITY_NAME.
        checkState(this.type == Type.COMMUNITY_NAME, "This device key is not a COMMUNITY_NAME type.");

        String name = annotations().value(AnnotationKeys.NAME);
        return CommunityName.communityName(name);
    }

    /**
     * Method to create a device key of type USERNAME_PASSWORD.
     *
     * @param id       device key identifier
     * @param label    optional label for this device key
     * @param username username for accessing this device
     * @param password password for accessing this device
     * @return device key
     */
    public static DeviceKey createDeviceKeyUsingUsernamePassword(DeviceKeyId id, String label,
                                                                 String username, String password) {
        DefaultAnnotations annotations = builder().set(AnnotationKeys.USERNAME, username)
                .set(AnnotationKeys.PASSWORD, password).build();

        return new DeviceKey(id, label, Type.USERNAME_PASSWORD, annotations);
    }

    /**
     * Method to create a device key of type SSL_KEY.
     *
     * @param id       device key identifier
     * @param label    optional label for this device key
     * @param username username for accessing this device
     * @param password password for accessing this device SSH key
     * @param sshkey   SSH key for accessing this device
     * @return device key
     */
    public static DeviceKey createDeviceKeyUsingSshKey(DeviceKeyId id, String label,
                                                       String username, String password, String sshkey) {
        DefaultAnnotations annotations = builder().set(AnnotationKeys.USERNAME, username)
                .set(AnnotationKeys.PASSWORD, password)
                .set(AnnotationKeys.SSHKEY, sshkey).build();

        return new DeviceKey(id, label, Type.SSL_KEY, annotations);
    }

    /**
     * Returns a username and password object from the device key.
     *
     * @return username and password
     */
    public UsernamePassword asUsernamePassword() {

        // Validate that the device key is of type USERNAME_PASSWORD.
        checkState(this.type == Type.USERNAME_PASSWORD, "This device key is not a USERNAME_PASSWORD type.");

        String username = annotations().value(AnnotationKeys.USERNAME);
        String password = annotations().value(AnnotationKeys.PASSWORD);
        return UsernamePassword.usernamePassword(username, password);
    }
}
