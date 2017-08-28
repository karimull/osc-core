/*******************************************************************************
 * Copyright (c) Intel Corporation
 * Copyright (c) 2017
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package org.osc.core.broker.service.k8s;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.apache.log4j.Logger;
import org.osc.core.broker.model.entities.virtualization.VirtualizationConnector;
import org.osc.core.broker.rest.client.k8s.KubernetesClient;
import org.osc.core.broker.rest.client.k8s.KubernetesPod;
import org.osc.core.broker.rest.client.k8s.KubernetesPodApi;
import org.osc.core.broker.service.api.KubernetesServiceApi;
import org.osc.core.broker.service.exceptions.VmidcException;
import org.osgi.service.component.annotations.Component;

@Component(service = { KubernetesService.class, KubernetesServiceApi.class })
public class KubernetesService implements KubernetesServiceApi {

    private static final Logger LOG = Logger.getLogger(KubernetesExample.class);
    private VirtualizationConnector vc = new VirtualizationConnector();
    private KubernetesClient client;

    KubernetesPodApi api;

    private String constructPodsString(List<KubernetesPod> kPodList) {
        StringBuilder responseBldr = new StringBuilder();
        for(KubernetesPod pod : kPodList) {
            responseBldr.append(String.format("Pod Name: %s, Namespace: %s, uid: %s, node: %s", pod.getName(), pod.getNamespace(), pod.getUid(), pod.getNode()) + "\n");
        }
        return responseBldr.toString();
    }

    @Override
    public void createPod(String namespace, String label, String name) {
        KubernetesExample.createPod(namespace, label, name);
    }

    @Override
    public String getPodEvents(String label) throws InterruptedException {
        return KubernetesExample.getPodEvents(label);
    }

    @Override
    public void registerConnector(String ipAddress, String port) {
        KubernetesExample.registerConnector(ipAddress, port);
        this.vc.setProviderIpAddress(ipAddress);
        this.client = new KubernetesClient(this.vc);
        this.api = new KubernetesPodApi(this.client);
    }

    @Override
    public void closeConnector() {
        try {
            this.client.close();
        } catch (IOException e) {
            LOG.error("Failed to close K8 client");
        }
    }

    @Override
    public String getPodsByLabel(String label) throws VmidcException, IOException {

        String result1;

        try (final KubernetesClient localClient = new KubernetesClient(this.vc)) {
            KubernetesPodApi kPod = new KubernetesPodApi(localClient);
            List<KubernetesPod> kPodList = kPod.getPodsByLabel(label);
            if (kPodList == null) {
                return String.format("No pods found with the label %s. Null response.", label);
            } else {
                result1 = constructPodsString(kPodList);
            }
        }

        try {
            List<KubernetesPod> kPodList = this.api.getPodsByLabel(label);
            if (kPodList == null) {
                return String.format("No pods found with the label %s. Null response.", label);
            } else {
                result1.concat(constructPodsString(kPodList));
            }
        }catch (VmidcException e) {
            throw e;
        }

        return result1;
    }

    @Override
    public String getPodsByLabels(String key, String commaSeparatedLabels) {
        return KubernetesExample.getPodByLabels(key, commaSeparatedLabels);
    }

    @Override
    public String getPodsByName(String namespace, String name) {
        return KubernetesExample.getPodsByName(namespace, name);
    }

    @Override
    public String getPodById(String uid, String namespace, String name) throws VmidcException, IOException {
        String result;
        try (final KubernetesClient localClient = new KubernetesClient(this.vc)) {
            KubernetesPodApi kPod = new KubernetesPodApi(this.client);
            KubernetesPod pod = kPod.getPodById(uid, namespace, name);
            if (pod == null) {
                result = String.format("No pods found with the uid %s. Null response.", uid);
            } else {
                result = constructPodsString(Arrays.asList(pod));
            }
            return result;
        }
    }
}
