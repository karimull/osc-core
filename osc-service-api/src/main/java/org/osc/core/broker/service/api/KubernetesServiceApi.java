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
package org.osc.core.broker.service.api;

import java.io.IOException;

import org.osc.core.broker.service.exceptions.VmidcException;

public interface KubernetesServiceApi {

	String getPodsByLabel(String label) throws VmidcException, IOException;

	String getPodsByLabels(String key, String commaSeparatedLabels);

	String getPodsByName(String namespace, String name);

	void createPod(String namespace, String label, String name);

	String getPodEvents(String label) throws InterruptedException;

	void registerConnector(String ipAddress, String port);

	String getPodById(String uid, String namespace, String name) throws VmidcException, IOException;

	void closeConnector();
}
