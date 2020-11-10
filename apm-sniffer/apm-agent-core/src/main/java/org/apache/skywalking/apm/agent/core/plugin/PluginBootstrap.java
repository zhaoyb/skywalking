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

package org.apache.skywalking.apm.agent.core.plugin;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import org.apache.skywalking.apm.agent.core.boot.AgentPackageNotFoundException;
import org.apache.skywalking.apm.agent.core.plugin.loader.AgentClassLoader;
import org.apache.skywalking.apm.agent.core.logging.api.ILog;
import org.apache.skywalking.apm.agent.core.logging.api.LogManager;

/**
 * Plugins finder. Use {@link PluginResourcesResolver} to find all plugins, and ask {@link PluginCfg} to load all plugin
 * definitions.
 */
public class PluginBootstrap {
    private static final ILog logger = LogManager.getLogger(PluginBootstrap.class);

    /**
     * load all plugins.
     *
     * 加载所有插件
     *
     * @return plugin definition list.
     */
    public List<AbstractClassEnhancePluginDefine> loadPlugins() throws AgentPackageNotFoundException {
        AgentClassLoader.initDefaultLoader();

        //plugin 资源解析
        PluginResourcesResolver resolver = new PluginResourcesResolver();
        // 加载所有的 kywalking-plugin.def文件
        List<URL> resources = resolver.getResources();

        if (resources == null || resources.size() == 0) {
            logger.info("no plugin files (skywalking-plugin.def) found, continue to start application.");
            return new ArrayList<AbstractClassEnhancePluginDefine>();
        }

        // 解析 kywalking-plugin.def文件， 放入List<PluginDefine>
        for (URL pluginUrl : resources) {
            try {
                PluginCfg.INSTANCE.load(pluginUrl.openStream());
            } catch (Throwable t) {
                logger.error(t, "plugin file [{}] init failure.", pluginUrl);
            }
        }

        // 所有插件配置
        List<PluginDefine> pluginClassList = PluginCfg.INSTANCE.getPluginClassList();

        // 类增强插件
        List<AbstractClassEnhancePluginDefine> plugins = new ArrayList<AbstractClassEnhancePluginDefine>();
        for (PluginDefine pluginDefine : pluginClassList) {
            try {
                logger.debug("loading plugin class {}.", pluginDefine.getDefineClass());
                // 将插件配置类，全部通过反射实例化， 转为AbstractClassEnhancePluginDefine， 这件插件本身也是继承于 AbstractClassEnhancePluginDefine
                AbstractClassEnhancePluginDefine plugin = (AbstractClassEnhancePluginDefine) Class.forName(pluginDefine.getDefineClass(), true, AgentClassLoader
                    .getDefault()).newInstance();
                // 实例放入集合
                plugins.add(plugin);
            } catch (Throwable t) {
                logger.error(t, "load plugin [{}] failure.", pluginDefine.getDefineClass());
            }
        }

        plugins.addAll(DynamicPluginLoader.INSTANCE.load(AgentClassLoader.getDefault()));

        return plugins;

    }

}
