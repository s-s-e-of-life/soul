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
 */

package org.dromara.soul.examples.sofa.service.impl;

import org.dromara.soul.client.sofa.common.annotation.SoulSofaClient;
import org.dromara.soul.examples.sofa.api.entity.SofaComplexTypeBean;
import org.dromara.soul.examples.sofa.api.entity.SofaSimpleTypeBean;
import org.dromara.soul.examples.sofa.api.service.SofaMultiParamService;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Sofa multi parameter service.
 *
 * @author wuudongdong
 */
@Service("sofaMultiParamService")
public class SofaMultiParamServiceImpl implements SofaMultiParamService {

    @Override
    @SoulSofaClient(path = "/findByIdsAndName", desc = "findByIdsAndName")
    public SofaSimpleTypeBean findByIdsAndName(final List<Integer> ids, final String name) {
        SofaSimpleTypeBean simpleTypeBean = new SofaSimpleTypeBean();
        simpleTypeBean.setId(ids.toString());
        simpleTypeBean.setName("hello world soul sofa param findByIdsAndName ：" + name);
        return simpleTypeBean;
    }

    @Override
    @SoulSofaClient(path = "/findByArrayIdsAndName", desc = "findByIdsAndName")
    public SofaSimpleTypeBean findByArrayIdsAndName(final Integer[] ids, final String name) {
        SofaSimpleTypeBean simpleTypeBean = new SofaSimpleTypeBean();
        simpleTypeBean.setId(Arrays.toString(ids));
        simpleTypeBean.setName("hello world soul sofa param findByArrayIdsAndName ：" + name);
        return simpleTypeBean;
    }

    @Override
    @SoulSofaClient(path = "/findByStringArray", desc = "findByStringArray")
    public SofaSimpleTypeBean findByStringArray(final String[] ids) {
        SofaSimpleTypeBean simpleTypeBean = new SofaSimpleTypeBean();
        simpleTypeBean.setId(Arrays.toString(ids));
        simpleTypeBean.setName("hello world soul sofa param findByStringArray");
        return simpleTypeBean;
    }

    @Override
    @SoulSofaClient(path = "/findByListId", desc = "findByListId")
    public SofaSimpleTypeBean findByListId(final List<String> ids) {
        SofaSimpleTypeBean simpleTypeBean = new SofaSimpleTypeBean();
        simpleTypeBean.setId(ids.toString());
        simpleTypeBean.setName("hello world soul sofa param findByListId");
        return simpleTypeBean;
    }

    @Override
    @SoulSofaClient(path = "/batchSave", desc = "batchSave")
    public SofaSimpleTypeBean batchSave(final List<SofaSimpleTypeBean> sofaTestList) {
        SofaSimpleTypeBean simpleTypeBean = new SofaSimpleTypeBean();
        simpleTypeBean.setId(sofaTestList.stream().map(SofaSimpleTypeBean::getId).collect(Collectors.joining("-")));
        simpleTypeBean.setName("hello world soul sofa param batchSave :" + sofaTestList.stream().map(SofaSimpleTypeBean::getName).collect(Collectors.joining("-")));
        return simpleTypeBean;
    }

    @Override
    @SoulSofaClient(path = "/batchSaveNameAndId", desc = "batchSaveNameAndId")
    public SofaSimpleTypeBean batchSaveNameAndId(final List<SofaSimpleTypeBean> sofaTestList, final String id, final String name) {
        SofaSimpleTypeBean simpleTypeBean = new SofaSimpleTypeBean();
        simpleTypeBean.setId(id);
        simpleTypeBean.setName("hello world soul sofa param batchSaveAndNameAndId :" + name + ":" + sofaTestList.stream().map(SofaSimpleTypeBean::getName).collect(Collectors.joining("-")));
        return simpleTypeBean;
    }

    @Override
    @SoulSofaClient(path = "/saveComplexBean", desc = "saveComplexBean")
    public SofaSimpleTypeBean saveComplexBean(final SofaComplexTypeBean sofaComplexTypeBean) {
        SofaSimpleTypeBean simpleTypeBean = new SofaSimpleTypeBean();
        simpleTypeBean.setId(sofaComplexTypeBean.getIdLists().toString());
        simpleTypeBean.setName("hello world soul sofa param saveComplexBean :" + sofaComplexTypeBean.getSofaSimpleTypeBean().getName());
        return simpleTypeBean;
    }

    @Override
    @SoulSofaClient(path = "/saveComplexBeanAndName", desc = "saveComplexBeanAndName")
    public SofaSimpleTypeBean saveComplexBeanAndName(final SofaComplexTypeBean sofaComplexTypeBean, final String name) {
        SofaSimpleTypeBean simpleTypeBean = new SofaSimpleTypeBean();
        simpleTypeBean.setId(sofaComplexTypeBean.getIdLists().toString());
        simpleTypeBean.setName("hello world soul sofa param saveComplexBeanAndName :" + sofaComplexTypeBean.getSofaSimpleTypeBean().getName() + "-" + name);
        return simpleTypeBean;
    }

    @Override
    @SoulSofaClient(path = "/saveTwoList", desc = "saveTwoList")
    public SofaSimpleTypeBean saveTwoList(final List<SofaComplexTypeBean> sofaComplexTypeBeanList, final Map<String, SofaSimpleTypeBean> sofaSimpleTypeBeanMap) {
        SofaSimpleTypeBean simpleTypeBean = new SofaSimpleTypeBean();
        simpleTypeBean.setId(sofaComplexTypeBeanList.get(0).getIdLists().toString());
        simpleTypeBean.setName("hello world soul sofa param saveTwoList :" + sofaComplexTypeBeanList.get(0).getSofaSimpleTypeBean().getName()
                + "-" + sofaSimpleTypeBeanMap.values().stream().findFirst().map(SofaSimpleTypeBean::getName).toString());
        return simpleTypeBean;
    }

}
