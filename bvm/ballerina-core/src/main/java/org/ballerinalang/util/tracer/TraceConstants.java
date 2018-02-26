/*
 * Copyright (c)  2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.ballerinalang.util.tracer;

/**
 * {@code TraceConstants} define tracer constants.
 *
 * @since 0.96.1
 */
public class TraceConstants {

    static final String TRACER_MANAGER_CLASS = "org.ballerina.tracing.core.OpenTracerFactory";

    public static final String TRACE_PROPERTY_INVOCATION_ID = "invocationId";

    public static final String TRACE_PROPERTY_RESOURCE = "trace_resource";

    public static final String TRACE_PROPERTY_SERVICE = "trace_service";

    public static final String TRACE_PROPERTY_REQUEST = "trace_request";

    public static final String TRACE_PREFIX = "trace___";

}
