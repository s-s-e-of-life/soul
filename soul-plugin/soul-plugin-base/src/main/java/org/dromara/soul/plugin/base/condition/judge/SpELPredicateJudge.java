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

package org.dromara.soul.plugin.base.condition.judge;

import org.dromara.soul.common.dto.ConditionData;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;

/**
 * SpEL predicate judge.
 */
public class SpELPredicateJudge implements PredicateJudge {
    
    private static final ExpressionParser EXPRESSION_PARSER = new SpelExpressionParser();
    
    @Override
    public Boolean judge(final ConditionData conditionData, final String realData) {
        Expression expression = EXPRESSION_PARSER.parseExpression(conditionData.getParamValue().replace('#' + conditionData.getParamName(), realData));
        return expression.getValue(Boolean.class);
    }
}
