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

import org.osc.core.broker.service.response.BaseJobResponse;
import org.osc.sdk.manager.element.MgrChangeNotification;

// this interface decouples the WebSocketClient from the osc-rest-server.
public interface ManagerApi {
    BaseJobResponse triggerMcSyncService(String username, String ipAddress,
            MgrChangeNotification notification) throws Exception;
}
