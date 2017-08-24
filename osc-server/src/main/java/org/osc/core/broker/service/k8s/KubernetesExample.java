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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodBuilder;
import io.fabric8.kubernetes.api.model.PodList;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.Watch;
import io.fabric8.kubernetes.client.Watcher;

public class KubernetesExample {

    private static final Logger LOG = Logger.getLogger(KubernetesExample.class);
    private static String IP_ADDRESS;
    private static String PORT;

    public static String getPodsByLabel(String label) {
        LOG.info("Getting pods by label");
        KubernetesConnection connection = new KubernetesConnection(IP_ADDRESS, PORT);
        StringBuilder responseBldr = new StringBuilder();

        try (final KubernetesClient client = connection.getConnection()) {
            PodList pods = client.pods().withLabel(label).list();
            if (pods == null) {
                return String.format("No pods found with the label %s. Null response.", label);
            }
            if (pods.getItems().isEmpty()) {
                return String.format("No pods found with the label %s. Empty list.", label);
            }

            for(Pod pod : pods.getItems()) {
                responseBldr.append(String.format("Pod Name: %s, Namespace: %s, labels: %s, id: %s", pod.getMetadata().getName(), pod.getMetadata().getNamespace(), pod.getMetadata().getLabels(), pod.getMetadata().getUid()) + "\n");
            }

        } catch (KubernetesClientException e) {
            throw e;
        }

        return responseBldr.toString();
    }

    public static String getPodByLabels(String key, String commaSeparatedLabels) {
        LOG.info("Getting pod by labels");
        KubernetesConnection connection = new KubernetesConnection(IP_ADDRESS, PORT);
        StringBuilder responseBldr = new StringBuilder();
        String[] labels = commaSeparatedLabels.split(",");

        try (final KubernetesClient client = connection.getConnection()) {
            PodList pods = client.pods().withLabelIn(key, labels).list();
            if (pods == null) {
                return String.format("No pods found with the key %s and labels %s. Null response.", key, commaSeparatedLabels);
            }
            if (pods.getItems().isEmpty()) {
                return String.format("No pods found with the key %s and labels %s. Empty list.", key, commaSeparatedLabels);
            }

            for(Pod pod : pods.getItems()) {
                responseBldr.append(String.format("Pod Name: %s, Namespace: %s, labels: %s, id: %s", pod.getMetadata().getName(), pod.getMetadata().getNamespace(), pod.getMetadata().getLabels(), pod.getMetadata().getUid()) + "\n");
            }

        } catch (KubernetesClientException e) {
            throw e;
        }

        return responseBldr.toString();
    }

    public static String getPodsByName(String namespace, String name) {
        LOG.info("Getting pod by name");
        KubernetesConnection connection = new KubernetesConnection(IP_ADDRESS, PORT);
        String result = "";

        try (final KubernetesClient client = connection.getConnection()) {
            Pod pod = client.pods().inNamespace(namespace).withName(name).get();

            if (pod == null) {
                result = String.format("No pods found with the name %s. Null response.", name);
            } else {
                result = String.format("Pod Name: %s, labels: %s, id: %s", pod.getMetadata().getName(), pod.getMetadata().getLabels(), pod.getMetadata().getUid());
            }

        } catch (KubernetesClientException e) {
            throw e;
        }

        return result;
    }

    public static void createPod(String namespace, String label, String name) {
        LOG.info(String.format("Creating a pod with the name %s and label %s", name, label));
        KubernetesConnection connection = new KubernetesConnection(IP_ADDRESS, PORT);

        String[] labelTuple = label.split("=");
        String labelName = labelTuple[0];
        String labelValue = labelTuple[1];

        try (final KubernetesClient client = connection.getConnection()) {
            Pod newPod = new PodBuilder()
                    .withKind("Pod")
                    .withNewMetadata()
                    .withName(name).addToLabels(labelName, labelValue)
                    .endMetadata()
                    .withNewSpec()
                    .addNewContainer()
                    .withName(name).withImage("fedora/apache")
                    .endContainer()
                    .endSpec()
                    .build();
            Pod result = client.pods().inNamespace(namespace).create(newPod);

            if (result == null) {
                LOG.error("The pod creation returned a null result.");
                return;
            }

            LOG.info(String.format("The creation of the pod %s returned the id %s", name, result.getMetadata().getUid()));

        } catch (KubernetesClientException e) {
            throw e;
        }
    }

    public static void registerConnector(String ipAddress, String port) {
        LOG.info(String.format("Registering the k8s connector ip address %s, port %s", ipAddress, port));
        IP_ADDRESS = ipAddress;
        PORT = port;
    }

    public static String getPodEvents(String label) throws InterruptedException {
        LOG.info("Testing watcher with k8s");
        KubernetesConnection connection = new KubernetesConnection(IP_ADDRESS, PORT);
        StringBuilder responseBldr = new StringBuilder();

        final CountDownLatch closeLatch = new CountDownLatch(1);
        try (final KubernetesClient client = connection.getConnection()) {
            try (Watch watch = client.pods().withLabel(label).watch(new Watcher<Pod>() {

                @Override
                public void eventReceived(Action action, Pod resource) {
                    String eventEntry = String.format("Pod Event: %s, Name: %s, labels: %s, status: %s", action, resource.getMetadata().getName(), resource.getMetadata().getLabels(), resource.getStatus());
                    responseBldr.append(eventEntry + "\n");
                    LOG.warn(eventEntry);
                }

                @Override
                public void onClose(KubernetesClientException e) {
                    String eventEntry = "Closing watcher";
                    responseBldr.append(eventEntry + "\n");
                    LOG.warn(eventEntry);

                    if (e != null) {
                        LOG.error(String.format("Error when closing watcher: %s", e));
                        closeLatch.countDown();
                    }
                }
            })) {

                // Watcher live for 1 min.
                closeLatch.await(1, TimeUnit.MINUTES);
            } catch (KubernetesClientException | InterruptedException e) {
                LOG.error(String.format("Could not watch resources: %s", e));
            }
        } catch (Exception e) {
            e.printStackTrace();
            LOG.error(String.format("Could not obtain client: %s", e));

            Throwable[] suppressed = e.getSuppressed();
            if (suppressed != null) {
                for (Throwable t : suppressed) {
                    LOG.error(String.format("Could not obtain client suppressed exception: %s", t));
                }
            }
        }

        return responseBldr.toString();
    }
}
