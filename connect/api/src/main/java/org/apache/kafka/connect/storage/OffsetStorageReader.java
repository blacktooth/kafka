/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.kafka.connect.storage;

import java.util.Collection;
import java.util.Map;

/**
 * <p>
 * OffsetStorageReader provides access to the offset storage used by sources. This can be used by
 * connectors to determine offsets to start consuming data from. This is most commonly used during
 * initialization of a task, but can also be used during runtime, e.g. when reconfiguring a task.
 * </p>
 * <p>
 * Offsets are always defined as Maps of Strings to primitive types, i.e. all types supported by
 * {@link org.apache.kafka.connect.data.Schema} other than Array, Map, and Struct.
 * </p>
 */
public interface OffsetStorageReader {
    /**
     * Get the offset for the specified partition. If the data isn't already available locally, this
     * gets it from the backing store, which may require some network round trips.
     *
     * @param partition object uniquely identifying the partition of data
     * @return object uniquely identifying the offset in the partition of data
     */
    <T> Map<String, Object> offset(Map<String, T> partition);

    /**
     * Get the offset for the specified partition. If the data isn't already available locally, this
     * gets it from the backing store, which may require some network round trips.
     * This implementation can optionally fail if there are serialization / de-serialization or data corruption
     * with the offset backing store.
     * For backward compatibility, the default implementation will not fail on errors.
     *
     * @param partition object uniquely identifying the partition of data
     * @param failOnError fail the request on error
     * @return object uniquely identifying the offset in the partition of data
     */
    default <T> Map<String, Object> offset(Map<String, T> partition, boolean failOnError) {
        return offset(partition);
    }

    /**
     * <p>
     * Get a set of offsets for the specified partition identifiers. This may be more efficient
     * than calling {@link #offset(Map)} repeatedly.
     * </p>
     * <p>
     * Note that when errors occur, this method omits the associated data and tries to return as
     * many of the requested values as possible. This allows a task that's managing many partitions to
     * still proceed with any available data. Therefore, implementations should take care to check
     * that the data is actually available in the returned response. The only case when an
     * exception will be thrown is if the entire request failed, e.g. because the underlying
     * storage was unavailable.
     * </p>
     *
     * @param partitions set of identifiers for partitions of data
     * @return a map of partition identifiers to decoded offsets
     */
    <T> Map<Map<String, T>, Map<String, Object>> offsets(Collection<Map<String, T>> partitions);

    /**
     * <p>
     * Get a set of offsets for the specified partition identifiers. This may be more efficient
     * than calling {@link #offset(Map)} repeatedly.
     * </p>
     * <p>
     *     This is the implementation of offsets that can optionally fail when there is an error serializing / de-serializing or
     *     if data is corrupted in the underlying offset backing store.
     *     Note: To be backward compatible, there is default implementation provided that doesn't fail.
     * </p>
     *
     * @param partitions set of identifiers for partitions of data
     * @param failOnError Fail the request on error
     * @return a map of partition identifiers to decoded offsets
     */
    default <T> Map<Map<String, T>, Map<String, Object>> offsets(Collection<Map<String, T>> partitions, boolean failOnError) {
        return offsets(partitions);
    }
}
