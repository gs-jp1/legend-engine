// Copyright 2022 Goldman Sachs
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.finos.legend.engine.persistence.components.util;

import org.finos.legend.engine.persistence.components.logicalplan.LogicalPlan;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Selection;
import org.finos.legend.engine.persistence.components.logicalplan.operations.Insert;
import org.finos.legend.engine.persistence.components.logicalplan.values.BatchEndTimestamp;
import org.finos.legend.engine.persistence.components.logicalplan.values.BatchIdValue;
import org.finos.legend.engine.persistence.components.logicalplan.values.BatchStartTimestamp;
import org.finos.legend.engine.persistence.components.logicalplan.values.BulkLoadBatchStatusValue;
import org.finos.legend.engine.persistence.components.logicalplan.values.StringValue;
import org.finos.legend.engine.persistence.components.logicalplan.values.Value;
import org.finos.legend.engine.persistence.components.relational.SqlPlan;
import org.finos.legend.engine.persistence.components.relational.ansi.AnsiSqlSink;
import org.finos.legend.engine.persistence.components.relational.ansi.optimizer.UpperCaseOptimizer;
import org.finos.legend.engine.persistence.components.relational.transformer.RelationalTransformer;
import org.finos.legend.engine.persistence.components.transformer.TransformOptions;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

public abstract class MetadataUtilsTest
{
    private final StringValue tableName = StringValue.of("main");
    private final ZonedDateTime executionZonedDateTime = ZonedDateTime.of(2000, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
    protected final TransformOptions transformOptions = TransformOptions.builder().executionTimestampClock(Clock.fixed(executionZonedDateTime.toInstant(), ZoneOffset.UTC)).build();

    protected abstract MetadataDataset metadataDataset();

    protected abstract String lowerCaseTableName();

    protected String upperCaseTableName()
    {
        return lowerCaseTableName().toUpperCase();
    }

    @Test
    public void testGetBatchId()
    {
        MetadataUtils store = new MetadataUtils(metadataDataset());
        BatchIdValue selectValue = store.getBatchId(tableName);
        LogicalPlan logicalPlan = LogicalPlan.builder().addOps(selectValue.selection()).build();
        RelationalTransformer transformer = new RelationalTransformer(AnsiSqlSink.get(), transformOptions);
        SqlPlan physicalPlan = transformer.generatePhysicalPlan(logicalPlan);
        List<String> list = physicalPlan.getSqlList();
        String expectedSql = "SELECT COALESCE(MAX("  + lowerCaseTableName() + ".\"table_batch_id\"),0)+1 FROM " + lowerCaseTableName() + " as "  + lowerCaseTableName() + " WHERE UPPER("  + lowerCaseTableName() + ".\"table_name\") = 'MAIN'";
        Assertions.assertEquals(expectedSql, list.get(0));
    }

    @Test
    public void testGetBatchIdWithBatchPattern()
    {
        MetadataUtils store = new MetadataUtils(metadataDataset());
        BatchIdValue selectValue = store.getBatchId(tableName);
        Selection selection = Selection.builder().addFields(selectValue).build();
        LogicalPlan logicalPlan = LogicalPlan.builder().addOps(selection).build();
        TransformOptions transformOptionsWithBatchPattern = transformOptions.withBatchIdPattern("{BATCH_ID_PATTERN}");
        RelationalTransformer transformer = new RelationalTransformer(AnsiSqlSink.get(), transformOptionsWithBatchPattern);
        SqlPlan physicalPlan = transformer.generatePhysicalPlan(logicalPlan);
        List<String> list = physicalPlan.getSqlList();
        String expectedSql = "SELECT {BATCH_ID_PATTERN}";
        Assertions.assertEquals(expectedSql, list.get(0));
    }

    @Test
    public void testGetBatchIdWithUpperCase()
    {
        MetadataUtils store = new MetadataUtils(metadataDataset());
        BatchIdValue selectValue = store.getBatchId(tableName);
        LogicalPlan logicalPlan = LogicalPlan.builder().addOps(selectValue.selection()).build();
        RelationalTransformer transformer = new RelationalTransformer(AnsiSqlSink.get(), transformOptions.withOptimizers(new UpperCaseOptimizer()));
        SqlPlan physicalPlan = transformer.generatePhysicalPlan(logicalPlan);
        List<String> list = physicalPlan.getSqlList();
        String expectedSql = "SELECT COALESCE(MAX("  + lowerCaseTableName() + ".\"TABLE_BATCH_ID\"),0)+1 FROM " + upperCaseTableName() + " as "  + lowerCaseTableName() + " WHERE UPPER("  + lowerCaseTableName() + ".\"TABLE_NAME\") = 'MAIN'";
        Assertions.assertEquals(expectedSql, list.get(0));
    }

    @Test
    public void testGetPrevBatchId()
    {
        MetadataUtils store = new MetadataUtils(metadataDataset());
        Value selectValue = store.getPrevBatchId(tableName);
        Selection selection = Selection.builder().addFields(selectValue).build();
        LogicalPlan logicalPlan = LogicalPlan.builder().addOps(selection).build();
        RelationalTransformer transformer = new RelationalTransformer(AnsiSqlSink.get(), transformOptions);
        SqlPlan physicalPlan = transformer.generatePhysicalPlan(logicalPlan);
        List<String> list = physicalPlan.getSqlList();
        String expectedSql = "SELECT (SELECT COALESCE(MAX("  + lowerCaseTableName() + ".\"table_batch_id\"),0)+1 FROM " + lowerCaseTableName() + " as " + lowerCaseTableName() + " WHERE UPPER("  + lowerCaseTableName() + ".\"table_name\") = 'MAIN')-1";
        Assertions.assertEquals(expectedSql, list.get(0));
    }

    @Test
    public void testGetPrevBatchIdWithBatchIdPattern()
    {
        MetadataUtils store = new MetadataUtils(metadataDataset());
        Value selectValue = store.getPrevBatchId(tableName);
        Selection selection = Selection.builder().addFields(selectValue).build();
        LogicalPlan logicalPlan = LogicalPlan.builder().addOps(selection).build();
        TransformOptions transformOptionsWithBatchPattern = transformOptions.withBatchIdPattern("{BATCH_ID_PATTERN}");
        RelationalTransformer transformer = new RelationalTransformer(AnsiSqlSink.get(), transformOptionsWithBatchPattern);
        SqlPlan physicalPlan = transformer.generatePhysicalPlan(logicalPlan);
        List<String> list = physicalPlan.getSqlList();
        String expectedSql = "SELECT {BATCH_ID_PATTERN}-1";
        Assertions.assertEquals(expectedSql, list.get(0));
    }

    @Test
    public void testGetPrevBatchIdWithUpperCase()
    {
        MetadataUtils store = new MetadataUtils(metadataDataset());
        Value selectValue = store.getPrevBatchId(tableName);
        Selection selection = Selection.builder().addFields(selectValue).build();
        LogicalPlan logicalPlan = LogicalPlan.builder().addOps(selection).build();
        RelationalTransformer transformer = new RelationalTransformer(AnsiSqlSink.get(), transformOptions.withOptimizers(new UpperCaseOptimizer()));
        SqlPlan physicalPlan = transformer.generatePhysicalPlan(logicalPlan);
        List<String> list = physicalPlan.getSqlList();
        String expectedSql = "SELECT (SELECT COALESCE(MAX(" + lowerCaseTableName() + ".\"TABLE_BATCH_ID\"),0)+1 FROM " + upperCaseTableName() + " as "  + lowerCaseTableName() + " WHERE UPPER("  + lowerCaseTableName() + ".\"TABLE_NAME\") = 'MAIN')-1";
        Assertions.assertEquals(expectedSql, list.get(0));
    }

    @Test
    public void testInsertMetaStore()
    {
        MetadataUtils store = new MetadataUtils(metadataDataset());
        Insert operation = store.insertMetaData(tableName, BatchStartTimestamp.INSTANCE, BatchEndTimestamp.INSTANCE, StringValue.of(MetadataUtils.MetaTableStatus.DONE.toString()));
        RelationalTransformer transformer = new RelationalTransformer(AnsiSqlSink.get(), transformOptions);
        LogicalPlan logicalPlan = LogicalPlan.builder().addOps(operation).build();
        SqlPlan physicalPlan = transformer.generatePhysicalPlan(logicalPlan);
        List<String> list = physicalPlan.getSqlList();
        String expectedSql = "INSERT INTO " + lowerCaseTableName() + " (\"table_name\", \"table_batch_id\", \"batch_start_ts_utc\", \"batch_end_ts_utc\", \"batch_status\") (SELECT 'main',(SELECT COALESCE(MAX(" + lowerCaseTableName() + ".\"table_batch_id\"),0)+1 FROM " + lowerCaseTableName() + " as " + lowerCaseTableName() + " WHERE UPPER("  + lowerCaseTableName() + ".\"table_name\") = 'MAIN'),'2000-01-01 00:00:00.000000',CURRENT_TIMESTAMP(),'DONE')";
        Assertions.assertEquals(expectedSql, list.get(0));
    }

    @Test
    public void testInsertMetaStoreWithUpperCase()
    {
        MetadataUtils store = new MetadataUtils(metadataDataset());
        Insert operation = store.insertMetaData(tableName, BatchStartTimestamp.INSTANCE, BatchEndTimestamp.INSTANCE, StringValue.of(MetadataUtils.MetaTableStatus.DONE.toString()));
        LogicalPlan logicalPlan = LogicalPlan.builder().addOps(operation).build();

        RelationalTransformer transformer = new RelationalTransformer(AnsiSqlSink.get(), transformOptions.withOptimizers(new UpperCaseOptimizer()));
        SqlPlan physicalPlan = transformer.generatePhysicalPlan(logicalPlan);

        List<String> list = physicalPlan.getSqlList();
        String expectedSql = "INSERT INTO " + upperCaseTableName() + " (\"TABLE_NAME\", \"TABLE_BATCH_ID\", \"BATCH_START_TS_UTC\", \"BATCH_END_TS_UTC\", \"BATCH_STATUS\") (SELECT 'main',(SELECT COALESCE(MAX(" + lowerCaseTableName() + ".\"TABLE_BATCH_ID\"),0)+1 FROM " + upperCaseTableName() + " as " + lowerCaseTableName() + " WHERE UPPER(" + lowerCaseTableName() + ".\"TABLE_NAME\") = 'MAIN'),'2000-01-01 00:00:00.000000',CURRENT_TIMESTAMP(),'DONE')";
        Assertions.assertEquals(expectedSql, list.get(0));
    }

    @Test
    public void testGetMaxStagingFilters()
    {
        MetadataUtils store = new MetadataUtils(metadataDataset());
        Selection selection = store.getLatestStagingFilters(tableName);
        LogicalPlan logicalPlan = LogicalPlan.builder().addOps(selection).build();
        RelationalTransformer transformer = new RelationalTransformer(AnsiSqlSink.get(), transformOptions.withOptimizers(new UpperCaseOptimizer()));
        SqlPlan physicalPlan = transformer.generatePhysicalPlan(logicalPlan);
        List<String> list = physicalPlan.getSqlList();
        String expectedSql = "SELECT " + lowerCaseTableName() + ".\"BATCH_SOURCE_INFO\" FROM " + upperCaseTableName() + " as " + lowerCaseTableName() + " " +
                "WHERE (UPPER(" + lowerCaseTableName() + ".\"TABLE_NAME\") = 'MAIN') AND " +
                "(" + lowerCaseTableName() + ".\"TABLE_BATCH_ID\" = (SELECT MAX(" + lowerCaseTableName() + ".\"TABLE_BATCH_ID\") " +
                "FROM " + upperCaseTableName() + " as " + lowerCaseTableName() + " WHERE " +
                "UPPER(" + lowerCaseTableName() + ".\"TABLE_NAME\") = 'MAIN')) LIMIT 1";
        Assertions.assertEquals(expectedSql, list.get(0));

    }

    @Test
    public void testInsertMetadataWithAdditionalInfo()
    {
        MetadataUtils store = new MetadataUtils(metadataDataset());
        StringValue bulkLoadTableName = StringValue.of("appeng_log_table_name");
        StringValue batchSourceInfoValue = StringValue.of("my_batch_source_info");
        StringValue additionalMetadataValue = StringValue.of("my_additional_metadata");
        Insert operation = store.insertMetaData(bulkLoadTableName, BatchStartTimestamp.INSTANCE, BatchEndTimestamp.INSTANCE, BulkLoadBatchStatusValue.INSTANCE, Optional.of(batchSourceInfoValue), Optional.of(additionalMetadataValue));

        RelationalTransformer transformer = new RelationalTransformer(AnsiSqlSink.get(), transformOptions.withBulkLoadBatchStatusPattern("<BATCH_STATUS_PATTERN>"));
        LogicalPlan logicalPlan = LogicalPlan.builder().addOps(operation).build();
        SqlPlan physicalPlan = transformer.generatePhysicalPlan(logicalPlan);
        List<String> list = physicalPlan.getSqlList();
        String expectedSql = "INSERT INTO " + lowerCaseTableName() +
            " (\"table_name\", \"table_batch_id\", \"batch_start_ts_utc\", \"batch_end_ts_utc\", \"batch_status\", \"batch_source_info\", \"additional_metadata\")" +
            " (SELECT 'appeng_log_table_name',(SELECT COALESCE(MAX(" + lowerCaseTableName() + ".\"table_batch_id\"),0)+1 FROM " + lowerCaseTableName() + " as " + lowerCaseTableName() + " WHERE UPPER(" + lowerCaseTableName() + ".\"table_name\") = 'APPENG_LOG_TABLE_NAME'),'2000-01-01 00:00:00.000000',CURRENT_TIMESTAMP(),'<BATCH_STATUS_PATTERN>',PARSE_JSON('my_batch_source_info'),PARSE_JSON('my_additional_metadata'))";;
        Assertions.assertEquals(expectedSql, list.get(0));
    }

    @Test
    public void testInsertMetadataWithAdditionalInfoInUpperCase()
    {
        MetadataUtils store = new MetadataUtils(metadataDataset());
        StringValue bulkLoadTableName = StringValue.of("BULK_LOAD_TABLE_NAME");
        StringValue batchSourceInfoValue = StringValue.of("my_batch_source_info");
        StringValue additionalMetadataValue = StringValue.of("my_additional_metadata");
        Insert operation = store.insertMetaData(bulkLoadTableName, BatchStartTimestamp.INSTANCE, BatchEndTimestamp.INSTANCE, BulkLoadBatchStatusValue.INSTANCE, Optional.of(batchSourceInfoValue), Optional.of(additionalMetadataValue));

        RelationalTransformer transformer = new RelationalTransformer(AnsiSqlSink.get(), transformOptions.withBulkLoadBatchStatusPattern("<BATCH_STATUS_PATTERN>").withOptimizers(new UpperCaseOptimizer()));
        LogicalPlan logicalPlan = LogicalPlan.builder().addOps(operation).build();
        SqlPlan physicalPlan = transformer.generatePhysicalPlan(logicalPlan);
        List<String> list = physicalPlan.getSqlList();
        String expectedSql = "INSERT INTO " + upperCaseTableName() +
            " (\"TABLE_NAME\", \"TABLE_BATCH_ID\", \"BATCH_START_TS_UTC\", \"BATCH_END_TS_UTC\", \"BATCH_STATUS\", \"BATCH_SOURCE_INFO\", \"ADDITIONAL_METADATA\")" +
            " (SELECT 'BULK_LOAD_TABLE_NAME',(SELECT COALESCE(MAX(" + lowerCaseTableName() + ".\"TABLE_BATCH_ID\"),0)+1 FROM " + upperCaseTableName() + " as " + lowerCaseTableName() + " WHERE UPPER(" + lowerCaseTableName() + ".\"TABLE_NAME\") = 'BULK_LOAD_TABLE_NAME'),'2000-01-01 00:00:00.000000',CURRENT_TIMESTAMP(),'<BATCH_STATUS_PATTERN>',PARSE_JSON('my_batch_source_info'),PARSE_JSON('my_additional_metadata'))";
        Assertions.assertEquals(expectedSql, list.get(0));
    }
}
