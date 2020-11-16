/*
 * Copyright (C) 2020 Wigo Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.wigoai.nipa.regional.service.enginecall;

import org.moara.common.network.socket.HostAddrPort;
import org.moara.engine.console.EngineConsole;
import org.moara.open.api.client.ApiRequests;

/**
 * 리인덱스
 * 사전이나 분류모델이 변경되었을때 사용
 * @author macle
 */
public class ReIndex {
    public static void main(String[] args) {

        HostAddrPort addrPort
                = EngineConsole.getHostAddrPort("macle");
        String message
                = ApiRequests.sendToReceiveMessage(addrPort.getHostAddress(), addrPort.getPort()
                , "org.moara.keyword.api", "ReIndex", "");

        System.out.println(message);
    }
}
