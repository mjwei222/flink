/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.flink.table.gateway.api.results;

import org.apache.flink.annotation.PublicEvolving;
import org.apache.flink.table.gateway.api.operation.OperationStatus;
import org.apache.flink.table.gateway.api.operation.OperationType;

import javax.annotation.Nullable;

import java.util.Objects;
import java.util.Optional;

/** Information of the {@code Operation}. */
@PublicEvolving
public class OperationInfo {

    private final OperationStatus status;
    private final OperationType type;
    @Nullable private final Exception exception;

    public OperationInfo(OperationStatus status, OperationType type) {
        this(status, type, null);
    }

    public OperationInfo(
            OperationStatus status, OperationType type, @Nullable Exception exception) {
        this.status = status;
        this.type = type;
        this.exception = exception;
    }

    public OperationType getType() {
        return type;
    }

    public OperationStatus getStatus() {
        return status;
    }

    public Optional<Exception> getException() {
        return Optional.ofNullable(exception);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof OperationInfo)) {
            return false;
        }
        OperationInfo that = (OperationInfo) o;
        return status == that.status && type == that.type && exception == that.exception;
    }

    @Override
    public int hashCode() {
        return Objects.hash(status, type, exception);
    }
}
