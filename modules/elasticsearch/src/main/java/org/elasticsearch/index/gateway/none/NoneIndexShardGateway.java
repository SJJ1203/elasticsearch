/*
 * Licensed to Elastic Search and Shay Banon under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. Elastic Search licenses this
 * file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.elasticsearch.index.gateway.none;

import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.gateway.none.NoneGateway;
import org.elasticsearch.index.gateway.IndexShardGateway;
import org.elasticsearch.index.gateway.IndexShardGatewayRecoveryException;
import org.elasticsearch.index.settings.IndexSettings;
import org.elasticsearch.index.shard.AbstractIndexShardComponent;
import org.elasticsearch.index.shard.ShardId;
import org.elasticsearch.index.shard.service.IndexShard;
import org.elasticsearch.index.shard.service.InternalIndexShard;

import java.io.IOException;

/**
 * @author kimchy (shay.banon)
 */
public class NoneIndexShardGateway extends AbstractIndexShardComponent implements IndexShardGateway {

    private final InternalIndexShard indexShard;

    private final RecoveryStatus recoveryStatus = new RecoveryStatus();

    @Inject public NoneIndexShardGateway(ShardId shardId, @IndexSettings Settings indexSettings, IndexShard indexShard) {
        super(shardId, indexSettings);
        this.indexShard = (InternalIndexShard) indexShard;
    }

    @Override public String toString() {
        return "_none_";
    }

    @Override public RecoveryStatus recoveryStatus() {
        return recoveryStatus;
    }

    @Override public RecoveryStatus recover() throws IndexShardGatewayRecoveryException {
        recoveryStatus().index().startTime(System.currentTimeMillis());
        recoveryStatus.translog().startTime(System.currentTimeMillis());
        // in the none case, we simply start the shard
        // clean the store, there should be nothing there...
        try {
            indexShard.store().deleteContent();
        } catch (IOException e) {
            logger.warn("failed to clean store before starting shard", e);
        }
        indexShard.start();
        recoveryStatus.index().took(System.currentTimeMillis() - recoveryStatus.index().startTime());
        recoveryStatus.translog().took(System.currentTimeMillis() - recoveryStatus.index().startTime());
        return recoveryStatus.updateStage(RecoveryStatus.Stage.DONE);
    }

    @Override public String type() {
        return NoneGateway.TYPE;
    }

    @Override public SnapshotStatus snapshot(Snapshot snapshot) {
        return SnapshotStatus.NA;
    }

    @Override public boolean requiresSnapshotScheduling() {
        return false;
    }

    @Override public void close(boolean delete) {
    }
}
