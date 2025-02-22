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

package org.apache.flink.runtime.io.network.partition.hybrid;

import org.apache.flink.runtime.io.network.buffer.Buffer;

import javax.annotation.Nullable;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.apache.flink.util.Preconditions.checkState;

/**
 * This class maintains the buffer's reference count and its status for hybrid shuffle mode.
 *
 * <p>Each buffer has three status: {@link #released}, {@link #spillStarted}, {@link #consumed}.
 *
 * <ul>
 *   <li>{@link #released} indicates that buffer has been released from the memory data manager, and
 *       can no longer be spilled or consumed.
 *   <li>{@link #spillStarted} indicates that spilling of the buffer has started, either completed
 *       or not.
 *   <li>{@link #consumed} indicates that buffer has been consumed by the downstream.
 * </ul>
 *
 * <p>Reference count of the buffer is maintained as follows: *
 *
 * <ul>
 *   <li>+1 when the buffer is obtained by memory data manager (from the buffer pool), and -1 when
 *       it is released from memory data manager.
 *   <li>+1 when spilling of the buffer is tarted, and -1 when it is completed.
 *   <li>+1 when the buffer is being consumed, and -1 when consuming is completed (by the
 *       downstream).
 * </ul>
 *
 * <p>Note: This class is not thread-safe.
 */
public class HsBufferContext {
    private final Buffer buffer;

    private final BufferIndexAndChannel bufferIndexAndChannel;

    // --------------------------
    //      Buffer Status
    // --------------------------
    private boolean released;

    private boolean spillStarted;

    private boolean consumed;

    @Nullable private CompletableFuture<Void> spilledFuture;

    public HsBufferContext(Buffer buffer, int bufferIndex, int subpartitionId) {
        this.bufferIndexAndChannel = new BufferIndexAndChannel(bufferIndex, subpartitionId);
        this.buffer = buffer;
    }

    public Buffer getBuffer() {
        return buffer;
    }

    public BufferIndexAndChannel getBufferIndexAndChannel() {
        return bufferIndexAndChannel;
    }

    public boolean isReleased() {
        return released;
    }

    public boolean isSpillStarted() {
        return spillStarted;
    }

    public boolean isConsumed() {
        return consumed;
    }

    public Optional<CompletableFuture<Void>> getSpilledFuture() {
        return Optional.ofNullable(spilledFuture);
    }

    public void release() {
        checkState(!released, "Release buffer repeatedly is unexpected.");
        released = true;
        // decrease ref count when buffer is released from memory.
        buffer.recycleBuffer();
    }

    public void startSpilling(CompletableFuture<Void> spilledFuture) {
        checkState(!released, "Buffer is already released.");
        checkState(
                !spillStarted && this.spilledFuture == null,
                "Spill buffer repeatedly is unexpected.");
        spillStarted = true;
        this.spilledFuture = spilledFuture;
        // increase ref count when buffer is decided to spill.
        buffer.retainBuffer();
        // decrease ref count when buffer spilling is finished.
        spilledFuture.thenRun(buffer::recycleBuffer);
    }

    public void consumed() {
        checkState(!released, "Buffer is already released.");
        checkState(!consumed, "Consume buffer repeatedly is unexpected.");
        consumed = true;
        // increase ref count when buffer is consumed, will be decreased when downstream finish
        // consuming.
        buffer.retainBuffer();
    }
}
