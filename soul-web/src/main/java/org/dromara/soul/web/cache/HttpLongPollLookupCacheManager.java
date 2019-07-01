/*
 *   Licensed to the Apache Software Foundation (ASF) under one or more
 *   contributor license agreements.  See the NOTICE file distributed with
 *   this work for additional information regarding copyright ownership.
 *   The ASF licenses this file to You under the Apache License, Version 2.0
 *   (the "License"); you may not use this file except in compliance with
 *   the License.  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package org.dromara.soul.web.cache;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import org.apache.commons.lang3.StringUtils;
import org.dromara.soul.common.constant.HttpConstants;
import org.dromara.soul.common.dto.AppAuthData;
import org.dromara.soul.common.dto.ConfigData;
import org.dromara.soul.common.dto.PluginData;
import org.dromara.soul.common.dto.RuleData;
import org.dromara.soul.common.dto.SelectorData;
import org.dromara.soul.common.enums.ConfigGroupEnum;
import org.dromara.soul.common.exception.SoulException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.OkHttp3ClientHttpRequestFactory;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * HTTP long polling implementation.
 * @author huangxiaofeng
 * @since 2.0.0
 * @date 2019/6/15 18:42
 */
public class HttpLongPollLookupCacheManager extends AbstractLookupCacheManager implements InitializingBean, DisposableBean {

    /**
     * server list, default: http://127.0.0.1:8082
     */
    private List<String> serverList = Arrays.asList("http://localhost:8082");

    /**
     * default: 10s
     */
    private Duration connectionTimeout = Duration.ofSeconds( 10 );

    /**
     * only use for http long polling, init by {@link #afterPropertiesSet()}
     */
    private RestTemplate httpClient;

    private ExecutorService executor;

    private static final Logger logger = LoggerFactory.getLogger(HttpLongPollLookupCacheManager.class);

    public void setConnectionTimeout(Duration connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }

    public void setServerList(String serverList) {
        this.serverList = Arrays.asList(StringUtils.split(serverList, ","));
    }

    private static final AtomicBoolean RUNNING = new AtomicBoolean(false);

    @Override
    public void afterPropertiesSet() throws Exception {

        OkHttp3ClientHttpRequestFactory factory = new OkHttp3ClientHttpRequestFactory();
        factory.setConnectTimeout( (int)this.connectionTimeout.toMillis() );
        factory.setReadTimeout( (int)HttpConstants.CLIENT_POLLING_READ_TIMEOUT );
        this.httpClient = new RestTemplate(factory);

        // It could be initialized multiple times, so you need to control that.
        if (RUNNING.compareAndSet(false, true)) {

            // fetch all group configs.
            this.fetchGroupConfig(ConfigGroupEnum.values());

            // one thread for listener, another one for fetch config data.
            this.executor = Executors.newFixedThreadPool(2, (runnable) -> {
                Thread t = new Thread(runnable);
                t.setDaemon(true);
                t.setName("soul-http-long-polling");
                return t;
            });

            // start long polling.
            this.executor.execute(new HttpLongPollingTask());
        } else {
            logger.info("soul http long polling was started, executor=[{}]", executor);
        }

    }

    private void fetchGroupConfig(ConfigGroupEnum... groups) throws SoulException {
        StringBuilder params = new StringBuilder();
        for (ConfigGroupEnum groupKey : groups) {
            params.append("groupKey")
                    .append("=")
                    .append(groupKey.name())
                    .append("&");
        }

        SoulException ex = null;
        for (String server : serverList) {
            String url = server + "?" + StringUtils.removeEnd(params.toString(), "&");
            logger.info("request configs: [{}]", url);
            try {
                String json = this.httpClient.getForObject(url, String.class);
                logger.info("get configs: [{}]", json);
                updateCacheWithJson(json);
                return;
            } catch (Exception e) {
                logger.warn("request configs fail, server:[{}]", server);
                ex = new SoulException("Init cache error, serverList:" + this.serverList, e);
                // try next server, if have another one.
            }
        }

        if ( ex != null ) {
            throw ex;
        }
    }

    /**
     * If the data for some nodes has not changed, the node is null.
     * @param json {@linkplain org.dromara.soul.common.result.SoulResult}
     */
    private void updateCacheWithJson(String json) {

        JSONObject jsonObject = JSONObject.parseObject(json);
        JSONObject data = jsonObject.getJSONObject("data");

        // appAuth
        JSONObject configData = data.getJSONObject(ConfigGroupEnum.APP_AUTH.name());
        if ( configData != null ) {
            ConfigData<AppAuthData> result = configData.toJavaObject(new TypeReference<ConfigData<AppAuthData>>(){});
            this.flushAllAppAuth( result );
        }

        // plugin
        configData = data.getJSONObject(ConfigGroupEnum.PLUGIN.name());
        if ( configData != null ) {
            ConfigData<PluginData> result = configData.toJavaObject(new TypeReference<ConfigData<PluginData>>(){});
            this.flushAllPlugin( result );
        }

        // rule
        configData = data.getJSONObject(ConfigGroupEnum.RULE.name());
        if ( configData != null ) {
            ConfigData<RuleData> result = configData.toJavaObject(new TypeReference<ConfigData<RuleData>>(){});
            this.flushAllRule( result );
        }

        // selector
        configData = data.getJSONObject(ConfigGroupEnum.SELECTOR.name());
        if ( configData != null ) {
            ConfigData<SelectorData> result = configData.toJavaObject(new TypeReference<ConfigData<SelectorData>>(){});
            this.flushAllSelector( result );
        }

    }

    protected void doLongPolling() {

        Map<String, String> params = new HashMap<>();
        for (ConfigGroupEnum group : ConfigGroupEnum.values()) {
            ConfigData<?> cacheConfig = GROUP_CACHE.get(group);
            params.put(group.name(), String.join(",", cacheConfig.getMd5(), String.valueOf(cacheConfig.getLastModifyTime())));
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        HttpEntity httpEntity = new HttpEntity(params, headers);

        SoulException ex = null;
        for (String server : serverList) {

            String url = server + "/listener";
            logger.info("request listener configs: [{}]", url);

            try {

                String json = this.httpClient.postForObject(url, httpEntity, String.class);
                logger.info("listener result: [{}]", json);
                JSONArray groupJson = JSONObject.parseObject(json).getJSONArray("data");

                if ( groupJson != null ) {
                    // fetch group config async.
                    List<ConfigGroupEnum> changedGroups = groupJson.toJavaList(ConfigGroupEnum.class);
                    executor.execute(() -> fetchGroupConfig(changedGroups.toArray(new ConfigGroupEnum[0])));
                }
                return;

            } catch (RestClientException e) {
                logger.warn("listener configs fail, server:[{}]", server);
                ex = new SoulException("Init cache error, serverList:" + this.serverList, e);
                // try next server, if have another one.
            }
        }

        throw ex;
    }

    class HttpLongPollingTask implements Runnable {
        @Override
        public void run() {
            while (RUNNING.get()) {
                try {
                    doLongPolling();
                } catch (Exception e) {
                    // just logger
                    logger.error(e.getMessage(), e);
                }
            }
            logger.warn("Stop http long polling.");
        }
    }

    @Override
    public void destroy() throws Exception {
        RUNNING.set(false);
        if ( executor != null ) {
            executor.shutdownNow();
            // help gc
            executor = null;
        }
    }

}