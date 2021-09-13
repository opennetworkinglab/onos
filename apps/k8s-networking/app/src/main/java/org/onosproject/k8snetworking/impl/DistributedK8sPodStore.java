/*
 * Copyright 2019-present Open Networking Foundation
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
package org.onosproject.k8snetworking.impl;

import com.google.common.collect.ImmutableSet;
import io.fabric8.kubernetes.api.model.Affinity;
import io.fabric8.kubernetes.api.model.Capabilities;
import io.fabric8.kubernetes.api.model.ConfigMapEnvSource;
import io.fabric8.kubernetes.api.model.ConfigMapKeySelector;
import io.fabric8.kubernetes.api.model.ConfigMapVolumeSource;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerPort;
import io.fabric8.kubernetes.api.model.ContainerState;
import io.fabric8.kubernetes.api.model.ContainerStateRunning;
import io.fabric8.kubernetes.api.model.ContainerStateTerminated;
import io.fabric8.kubernetes.api.model.ContainerStateWaiting;
import io.fabric8.kubernetes.api.model.ContainerStatus;
import io.fabric8.kubernetes.api.model.EmptyDirVolumeSource;
import io.fabric8.kubernetes.api.model.EnvFromSource;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.EnvVarSource;
import io.fabric8.kubernetes.api.model.ExecAction;
import io.fabric8.kubernetes.api.model.FieldsV1;
import io.fabric8.kubernetes.api.model.HTTPGetAction;
import io.fabric8.kubernetes.api.model.HTTPHeader;
import io.fabric8.kubernetes.api.model.HostPathVolumeSource;
import io.fabric8.kubernetes.api.model.IntOrString;
import io.fabric8.kubernetes.api.model.KeyToPath;
import io.fabric8.kubernetes.api.model.LabelSelector;
import io.fabric8.kubernetes.api.model.LabelSelectorRequirement;
import io.fabric8.kubernetes.api.model.Lifecycle;
import io.fabric8.kubernetes.api.model.ManagedFieldsEntry;
import io.fabric8.kubernetes.api.model.NodeAffinity;
import io.fabric8.kubernetes.api.model.NodeSelector;
import io.fabric8.kubernetes.api.model.NodeSelectorRequirement;
import io.fabric8.kubernetes.api.model.NodeSelectorTerm;
import io.fabric8.kubernetes.api.model.ObjectFieldSelector;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.OwnerReference;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaimVolumeSource;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodAffinity;
import io.fabric8.kubernetes.api.model.PodAffinityTerm;
import io.fabric8.kubernetes.api.model.PodAntiAffinity;
import io.fabric8.kubernetes.api.model.PodCondition;
import io.fabric8.kubernetes.api.model.PodIP;
import io.fabric8.kubernetes.api.model.PodSecurityContext;
import io.fabric8.kubernetes.api.model.PodSpec;
import io.fabric8.kubernetes.api.model.PodStatus;
import io.fabric8.kubernetes.api.model.PreferredSchedulingTerm;
import io.fabric8.kubernetes.api.model.Probe;
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.api.model.ResourceFieldSelector;
import io.fabric8.kubernetes.api.model.ResourceRequirements;
import io.fabric8.kubernetes.api.model.SELinuxOptions;
import io.fabric8.kubernetes.api.model.SeccompProfile;
import io.fabric8.kubernetes.api.model.SecretEnvSource;
import io.fabric8.kubernetes.api.model.SecretKeySelector;
import io.fabric8.kubernetes.api.model.SecretVolumeSource;
import io.fabric8.kubernetes.api.model.SecurityContext;
import io.fabric8.kubernetes.api.model.TCPSocketAction;
import io.fabric8.kubernetes.api.model.Toleration;
import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.api.model.VolumeDevice;
import io.fabric8.kubernetes.api.model.VolumeMount;
import io.fabric8.kubernetes.api.model.WeightedPodAffinityTerm;
import org.onlab.util.KryoNamespace;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.k8snetworking.api.K8sPodEvent;
import org.onosproject.k8snetworking.api.K8sPodStore;
import org.onosproject.k8snetworking.api.K8sPodStoreDelegate;
import org.onosproject.store.AbstractStore;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.ConsistentMap;
import org.onosproject.store.service.MapEvent;
import org.onosproject.store.service.MapEventListener;
import org.onosproject.store.service.Serializer;
import org.onosproject.store.service.StorageService;
import org.onosproject.store.service.Versioned;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.onlab.util.Tools.groupedThreads;
import static org.onosproject.k8snetworking.api.K8sPodEvent.Type.K8S_POD_ANNOTATION_ADDED;
import static org.onosproject.k8snetworking.api.K8sPodEvent.Type.K8S_POD_COMPLETED;
import static org.onosproject.k8snetworking.api.K8sPodEvent.Type.K8S_POD_CRASH_LOOP_BACK_OFF;
import static org.onosproject.k8snetworking.api.K8sPodEvent.Type.K8S_POD_CREATED;
import static org.onosproject.k8snetworking.api.K8sPodEvent.Type.K8S_POD_FAILED;
import static org.onosproject.k8snetworking.api.K8sPodEvent.Type.K8S_POD_PENDING;
import static org.onosproject.k8snetworking.api.K8sPodEvent.Type.K8S_POD_REMOVED;
import static org.onosproject.k8snetworking.api.K8sPodEvent.Type.K8S_POD_RUNNING;
import static org.onosproject.k8snetworking.api.K8sPodEvent.Type.K8S_POD_SUCCEEDED;
import static org.onosproject.k8snetworking.api.K8sPodEvent.Type.K8S_POD_UNKNOWN;
import static org.onosproject.k8snetworking.api.K8sPodEvent.Type.K8S_POD_UPDATED;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Implementation of kubernetes pod store using consistent map.
 */
@Component(immediate = true, service = K8sPodStore.class)
public class DistributedK8sPodStore
        extends AbstractStore<K8sPodEvent, K8sPodStoreDelegate>
        implements K8sPodStore {

    private final Logger log = getLogger(getClass());

    private static final String ERR_NOT_FOUND = " does not exist";
    private static final String ERR_DUPLICATE = " already exists";
    private static final String APP_ID = "org.onosproject.k8snetwork";

    private static final String PENDING = "Pending";
    private static final String RUNNING = "Running";
    private static final String SUCCEEDED = "Succeeded";
    private static final String FAILED = "Failed";
    private static final String UNKNOWN = "Unknown";
    private static final String COMPLETED = "Completed";
    private static final String CRASH_LOOP_BACK_OFF = "CrashLoopBackOff";

    private static final KryoNamespace
            SERIALIZER_K8S_POD = KryoNamespace.newBuilder()
            .register(KryoNamespaces.API)
            .register(Pod.class)
            .register(ObjectMeta.class)
            .register(PodSpec.class)
            .register(PodStatus.class)
            .register(PodCondition.class)
            .register(PodIP.class)
            .register(Container.class)
            .register(EnvVar.class)
            .register(EnvVarSource.class)
            .register(EnvFromSource.class)
            .register(ConfigMapEnvSource.class)
            .register(ConfigMapKeySelector.class)
            .register(ObjectFieldSelector.class)
            .register(ResourceFieldSelector.class)
            .register(SecretKeySelector.class)
            .register(Lifecycle.class)
            .register(SecretEnvSource.class)
            .register(ContainerStatus.class)
            .register(ContainerState.class)
            .register(ContainerStateRunning.class)
            .register(ContainerStateTerminated.class)
            .register(ContainerStateWaiting.class)
            .register(OwnerReference.class)
            .register(Probe.class)
            .register(ExecAction.class)
            .register(HTTPGetAction.class)
            .register(HTTPHeader.class)
            .register(TCPSocketAction.class)
            .register(ContainerPort.class)
            .register(ResourceRequirements.class)
            .register(SecurityContext.class)
            .register(PodSecurityContext.class)
            .register(SELinuxOptions.class)
            .register(Volume.class)
            .register(VolumeDevice.class)
            .register(VolumeMount.class)
            .register(IntOrString.class)
            .register(Toleration.class)
            .register(PersistentVolumeClaimVolumeSource.class)
            .register(SecretVolumeSource.class)
            .register(EmptyDirVolumeSource.class)
            .register(Quantity.class)
            .register(Capabilities.class)
            .register(ConfigMapVolumeSource.class)
            .register(KeyToPath.class)
            .register(HostPathVolumeSource.class)
            .register(Affinity.class)
            .register(NodeAffinity.class)
            .register(NodeSelector.class)
            .register(NodeSelectorTerm.class)
            .register(NodeSelectorRequirement.class)
            .register(PreferredSchedulingTerm.class)
            .register(SeccompProfile.class)
            .register(PodAffinity.class)
            .register(WeightedPodAffinityTerm.class)
            .register(PodAffinityTerm.class)
            .register(LabelSelector.class)
            .register(LabelSelectorRequirement.class)
            .register(PodAntiAffinity.class)
            .register(ManagedFieldsEntry.class)
            .register(FieldsV1.class)
            .register(LinkedHashMap.class)
            .register(Collection.class)
            .build();

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected StorageService storageService;

    private final ExecutorService eventExecutor = newSingleThreadExecutor(
            groupedThreads(this.getClass().getSimpleName(), "event-handler", log));

    private final MapEventListener<String, Pod> podMapListener = new K8sPodMapListener();

    private ConsistentMap<String, Pod> podStore;

    @Activate
    protected void activate() {
        ApplicationId appId = coreService.registerApplication(APP_ID);
        podStore = storageService.<String, Pod>consistentMapBuilder()
                .withSerializer(Serializer.using(SERIALIZER_K8S_POD))
                .withName("k8s-pod-store")
                .withApplicationId(appId)
                .build();

        podStore.addListener(podMapListener);
        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        podStore.removeListener(podMapListener);
        eventExecutor.shutdown();
        log.info("Stopped");
    }

    @Override
    public void createPod(Pod pod) {
        podStore.compute(pod.getMetadata().getUid(), (uid, existing) -> {
            final String error = pod.getMetadata().getUid() + ERR_DUPLICATE;
            checkArgument(existing == null, error);
            return pod;
        });
    }

    @Override
    public void updatePod(Pod pod) {
        podStore.compute(pod.getMetadata().getUid(), (uid, existing) -> {
            final String error = pod.getMetadata().getUid() + ERR_NOT_FOUND;
            checkArgument(existing != null, error);
            return pod;
        });
    }

    @Override
    public Pod removePod(String uid) {
        Versioned<Pod> pod = podStore.remove(uid);
        if (pod == null) {
            final String error = uid + ERR_NOT_FOUND;
            throw new IllegalArgumentException(error);
        }
        return pod.value();
    }

    @Override
    public Pod pod(String uid) {
        return podStore.asJavaMap().get(uid);
    }

    @Override
    public Set<Pod> pods() {
        return ImmutableSet.copyOf(podStore.asJavaMap().values());
    }

    @Override
    public void clear() {
        podStore.clear();
    }

    private class K8sPodMapListener implements MapEventListener<String, Pod> {

        @Override
        public void event(MapEvent<String, Pod> event) {

            switch (event.type()) {
                case INSERT:
                    log.debug("Kubernetes pod created {}", event.newValue());
                    eventExecutor.execute(() ->
                            notifyDelegate(new K8sPodEvent(
                                    K8S_POD_CREATED, event.newValue().value())));
                    break;
                case UPDATE:
                    log.debug("Kubernetes pod updated {}", event.newValue());
                    eventExecutor.execute(() -> processUpdate(event));
                    break;
                case REMOVE:
                    log.debug("Kubernetes pod removed {}", event.oldValue());
                    eventExecutor.execute(() ->
                            notifyDelegate(new K8sPodEvent(
                                    K8S_POD_REMOVED, event.oldValue().value())));
                    break;
                default:
                    // do nothing
                    break;
            }
        }

        private void processUpdate(MapEvent<String, Pod> event) {
            notifyDelegate(new K8sPodEvent(
                    K8S_POD_UPDATED, event.newValue().value()));

            String oldPhase = event.oldValue().value().getStatus().getPhase();
            String newPhase = event.newValue().value().getStatus().getPhase();

            if (!PENDING.equals(oldPhase) && PENDING.equals(newPhase)) {
                notifyDelegate(new K8sPodEvent(
                        K8S_POD_PENDING, event.newValue().value()));
            }

            if (!RUNNING.equals(oldPhase) && RUNNING.equals(newPhase)) {
                notifyDelegate(new K8sPodEvent(
                        K8S_POD_RUNNING, event.newValue().value()));
            }

            if (!SUCCEEDED.equals(oldPhase) && SUCCEEDED.equals(newPhase)) {
                notifyDelegate(new K8sPodEvent(
                        K8S_POD_SUCCEEDED, event.newValue().value()));
            }

            if (!FAILED.equals(oldPhase) && FAILED.equals(newPhase)) {
                notifyDelegate(new K8sPodEvent(
                        K8S_POD_FAILED, event.newValue().value()));
            }

            if (!UNKNOWN.equals(oldPhase) && UNKNOWN.equals(newPhase)) {
                notifyDelegate(new K8sPodEvent(
                        K8S_POD_UNKNOWN, event.newValue().value()));
            }

            if (!COMPLETED.equals(oldPhase) && COMPLETED.equals(newPhase)) {
                notifyDelegate(new K8sPodEvent(
                        K8S_POD_COMPLETED, event.newValue().value()));
            }

            if (!CRASH_LOOP_BACK_OFF.equals(oldPhase) && CRASH_LOOP_BACK_OFF.equals(newPhase)) {
                notifyDelegate(new K8sPodEvent(
                        K8S_POD_CRASH_LOOP_BACK_OFF, event.newValue().value()));
            }

            Map<String, String> oldAnnot = event.oldValue().value().getMetadata().getAnnotations();
            Map<String, String> newAnnot = event.newValue().value().getMetadata().getAnnotations();

            if (oldAnnot == null && newAnnot != null) {
                notifyDelegate(new K8sPodEvent(
                        K8S_POD_ANNOTATION_ADDED, event.newValue().value()));
            }
        }
    }
}
