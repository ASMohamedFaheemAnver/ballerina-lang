/*
*  Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*  WSO2 Inc. licenses this file to you under the Apache License,
*  Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License.
*  You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
*  Unless required by applicable law or agreed to in writing,
*  software distributed under the License is distributed on an
*  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
*  KIND, either express or implied.  See the License for the
*  specific language governing permissions and limitations
*  under the License.
*/
package org.ballerinalang.connector.impl;

import org.ballerinalang.connector.api.BallerinaConnectorException;
import org.ballerinalang.connector.api.ConnectorFuture;
import org.ballerinalang.connector.api.ConnectorFutureListener;
import org.ballerinalang.services.ErrorHandlerUtils;
import org.ballerinalang.util.exceptions.BallerinaException;

import java.util.ArrayList;
import java.util.List;

/**
 * {@code BConnectorFuture} This is the implementation for the {@code ConnectorFuture} API.
 *
 * @since 0.94
 */
public class BServerConnectorFuture implements ConnectorFuture {
    private List<ConnectorFutureListener> connectorFutureListeners = new ArrayList<>();

    private BallerinaException exception;
    private boolean success = false;

    @Override
    public void registerConnectorFutureListener(ConnectorFutureListener futureListener) {
        if (futureListener != null) {
            connectorFutureListeners.add(futureListener);
            if (exception != null) {
                futureListener.notifyFailure(new BallerinaConnectorException(exception.getMessage(), exception));
                success = false; //double check this.
            }
            if (success) {
                futureListener.notifySuccess();
            }
        }
    }

    public void notifySuccess() {
        //if future listeners already exist, notify right away. if not store until listener registration.
        if (!connectorFutureListeners.isEmpty()) {
            connectorFutureListeners.forEach(ConnectorFutureListener::notifySuccess);
        } else {
            this.success = true;
        }
    }

    public void notifyFailure(BallerinaException exception) {
        //if future listeners already exist, notify right away. if not store until listener registration.
        if (!connectorFutureListeners.isEmpty()) {
            connectorFutureListeners.forEach(listener ->
                    listener.notifyFailure(new BallerinaConnectorException(exception.getMessage(), exception)));
        } else {
            ErrorHandlerUtils.printError(exception);
            this.exception = exception;
        }
    }
}
