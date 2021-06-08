/*
 * Copyright (C) 2021 Wigo Inc.
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

package com.wigoai.nipa.regional.service.channel;

import org.moara.common.annotation.Priority;
import org.moara.sync.Synchronizer;

/**
 * 채널 관리자
 * @author macle
 */
@Priority(seq = Integer.MAX_VALUE - 5)
public class ChannelManager implements Synchronizer {


    public ChannelManager(){

    }

    @Override
    public void sync() {

    }
}
