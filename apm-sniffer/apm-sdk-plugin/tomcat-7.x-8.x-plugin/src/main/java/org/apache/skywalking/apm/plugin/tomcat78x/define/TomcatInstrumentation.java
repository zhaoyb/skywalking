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

package org.apache.skywalking.apm.plugin.tomcat78x.define;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.matcher.ElementMatcher;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.InstanceMethodsInterceptPoint;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.ClassInstanceMethodsEnhancePluginDefine;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.ConstructorInterceptPoint;
import org.apache.skywalking.apm.agent.core.plugin.match.ClassMatch;

import static net.bytebuddy.matcher.ElementMatchers.named;
import static org.apache.skywalking.apm.agent.core.plugin.match.NameMatch.byName;

public class TomcatInstrumentation extends ClassInstanceMethodsEnhancePluginDefine {

    /**
     *
     * 增强类
     * Enhance class.
     */
    private static final String ENHANCE_CLASS = "org.apache.catalina.core.StandardHostValve";

    /**
     * 正常调用拦截类
     *
     * The intercept class for "invoke" method in the class "org.apache.catalina.core.StandardWrapperValve"
     */
    private static final String INVOKE_INTERCEPT_CLASS = "org.apache.skywalking.apm.plugin.tomcat78x.TomcatInvokeInterceptor";

    /**
     * 异常拦截类
     *
     * The intercept class for "exception" method in the class "org.apache.catalina.core.StandardWrapperValve"
     */
    private static final String EXCEPTION_INTERCEPT_CLASS = "org.apache.skywalking.apm.plugin.tomcat78x.TomcatExceptionInterceptor";

    /**
     *
     * 类名
     *
     * @return
     */
    @Override
    protected ClassMatch enhanceClass() {
        return byName(ENHANCE_CLASS);
    }

    /**
     * 构造函数增强
     *
     * @return
     */
    @Override
    public ConstructorInterceptPoint[] getConstructorsInterceptPoints() {
        return null;
    }

    /**
     * 方法增强
     *
     * @return
     */
    @Override
    public InstanceMethodsInterceptPoint[] getInstanceMethodsInterceptPoints() {
        return new InstanceMethodsInterceptPoint[] {
                // 拦截器A
            new InstanceMethodsInterceptPoint() {
                // 要拦截的方法名
                @Override
                public ElementMatcher<MethodDescription> getMethodsMatcher() {
                    return named("invoke");
                }

                // 适用的切面方法
                @Override
                public String getMethodsInterceptor() {
                    return INVOKE_INTERCEPT_CLASS;
                }

                @Override
                public boolean isOverrideArgs() {
                    return false;
                }
            },
                // 拦截器B
            new InstanceMethodsInterceptPoint() {
                @Override
                public ElementMatcher<MethodDescription> getMethodsMatcher() {
                    return named("throwable");
                }

                @Override
                public String getMethodsInterceptor() {
                    return EXCEPTION_INTERCEPT_CLASS;
                }

                @Override
                public boolean isOverrideArgs() {
                    return false;
                }
            }
        };
    }
}
