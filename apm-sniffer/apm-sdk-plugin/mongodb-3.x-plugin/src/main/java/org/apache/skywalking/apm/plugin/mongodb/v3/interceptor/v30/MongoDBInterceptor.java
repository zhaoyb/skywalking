/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.apache.skywalking.apm.plugin.mongodb.v3.interceptor.v30;

import com.mongodb.connection.Cluster;
import org.apache.skywalking.apm.agent.core.context.ContextManager;
import org.apache.skywalking.apm.agent.core.context.trace.AbstractSpan;
import org.apache.skywalking.apm.agent.core.logging.api.ILog;
import org.apache.skywalking.apm.agent.core.logging.api.LogManager;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.InstanceConstructorInterceptor;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.InstanceMethodsAroundInterceptor;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.MethodInterceptResult;
import org.apache.skywalking.apm.plugin.mongodb.v3.support.MongoRemotePeerHelper;
import org.apache.skywalking.apm.plugin.mongodb.v3.support.MongoSpanHelper;

import java.lang.reflect.Method;

/**
 * Intercept method of {@code com.mongodb.Mongo#execute(ReadOperation, ReadPreference)} or {@code
 * com.mongodb.Mongo#execute(WriteOperation)}. record the MongoDB host, operation name and the key of the operation.
 * <p>
 * only supported: 3.0.x-3.5.x
 */
@SuppressWarnings({
    "deprecation",
    "Duplicates"
})
public class MongoDBInterceptor implements InstanceMethodsAroundInterceptor, InstanceConstructorInterceptor {

    private static final ILog logger = LogManager.getLogger(MongoDBInterceptor.class);

    /**
     *
     * 调用构造函数时调用
     *
     * @param objInst
     * @param allArguments
     */
    @Override
    public void onConstruct(EnhancedInstance objInst, Object[] allArguments) {
        Cluster cluster = (Cluster) allArguments[0];
        String peers = MongoRemotePeerHelper.getRemotePeer(cluster);
        objInst.setSkyWalkingDynamicField(peers);
    }

    /**
     * 调用方法前调用
     *
     * @param objInst
     * @param method
     * @param allArguments
     * @param argumentsTypes
     * @param result change this result, if you want to truncate the method.
     */
    @Override
    public void beforeMethod(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes,
        MethodInterceptResult result) {
        String executeMethod = allArguments[0].getClass().getSimpleName();
        String remotePeer = (String) objInst.getSkyWalkingDynamicField();
        if (logger.isDebugEnable()) {
            logger.debug("Mongo execute: [executeMethod: {}, remotePeer: {}]", executeMethod, remotePeer);
        }
        MongoSpanHelper.createExitSpan(executeMethod, remotePeer, allArguments[0]);
    }

    /**
     *
     * 调用方法后 调用
     *
     * @param objInst
     * @param method
     * @param allArguments
     * @param argumentsTypes
     * @param ret the method's original return value. May be null if the method triggers an exception.
     * @return
     */
    @Override
    public Object afterMethod(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes,
        Object ret) {
        ContextManager.stopSpan();
        return ret;
    }

    // 方法异常时 调用
    @Override
    public void handleMethodException(EnhancedInstance objInst, Method method, Object[] allArguments,
        Class<?>[] argumentsTypes, Throwable t) {
        AbstractSpan activeSpan = ContextManager.activeSpan();
        activeSpan.errorOccurred();
        activeSpan.log(t);
    }
}
