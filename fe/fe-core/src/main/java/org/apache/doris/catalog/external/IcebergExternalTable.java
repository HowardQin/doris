// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

package org.apache.doris.catalog.external;

import org.apache.doris.catalog.Column;
import org.apache.doris.catalog.HiveMetaStoreClientHelper;
import org.apache.doris.datasource.iceberg.IcebergExternalCatalog;
import org.apache.doris.external.iceberg.util.IcebergUtils;
import org.apache.doris.statistics.AnalysisInfo;
import org.apache.doris.statistics.BaseAnalysisTask;
import org.apache.doris.statistics.ColumnStatistic;
import org.apache.doris.statistics.ExternalAnalysisTask;
import org.apache.doris.statistics.util.StatisticsUtil;
import org.apache.doris.thrift.THiveTable;
import org.apache.doris.thrift.TIcebergTable;
import org.apache.doris.thrift.TTableDescriptor;
import org.apache.doris.thrift.TTableType;

import java.util.HashMap;
import java.util.List;
import java.util.Optional;

public class IcebergExternalTable extends ExternalTable {
    public IcebergExternalTable(long id, String name, String dbName, IcebergExternalCatalog catalog) {
        super(id, name, catalog, dbName, TableType.ICEBERG_EXTERNAL_TABLE);
    }

    public String getIcebergCatalogType() {
        return ((IcebergExternalCatalog) catalog).getIcebergCatalogType();
    }

    protected synchronized void makeSureInitialized() {
        super.makeSureInitialized();
        if (!objectCreated) {
            objectCreated = true;
        }
    }

    @Override
    public List<Column> initSchema() {
        makeSureInitialized();
        return IcebergUtils.getSchema(catalog, dbName, name);
    }

    @Override
    public TTableDescriptor toThrift() {
        List<Column> schema = getFullSchema();
        if (getIcebergCatalogType().equals("hms")) {
            THiveTable tHiveTable = new THiveTable(dbName, name, new HashMap<>());
            TTableDescriptor tTableDescriptor = new TTableDescriptor(getId(), TTableType.HIVE_TABLE, schema.size(), 0,
                    getName(), dbName);
            tTableDescriptor.setHiveTable(tHiveTable);
            return tTableDescriptor;
        } else {
            TIcebergTable icebergTable = new TIcebergTable(dbName, name, new HashMap<>());
            TTableDescriptor tTableDescriptor = new TTableDescriptor(getId(), TTableType.ICEBERG_TABLE,
                    schema.size(), 0, getName(), dbName);
            tTableDescriptor.setIcebergTable(icebergTable);
            return tTableDescriptor;
        }
    }

    @Override
    public Optional<ColumnStatistic> getColumnStatistic(String colName) {
        makeSureInitialized();
        return HiveMetaStoreClientHelper.ugiDoAs(catalog.getConfiguration(),
                () -> StatisticsUtil.getIcebergColumnStats(colName,
                        ((IcebergExternalCatalog) catalog).getIcebergTable(dbName, name)));
    }

    @Override
    public BaseAnalysisTask createAnalysisTask(AnalysisInfo info) {
        makeSureInitialized();
        return new ExternalAnalysisTask(info);
    }
}
