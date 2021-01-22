package com.actiontech.dble.statistic.sql.handler;

import com.actiontech.dble.services.manager.information.ManagerTableUtil;
import com.actiontech.dble.statistic.sql.StatisticEvent;
import com.actiontech.dble.statistic.sql.StatisticManager;
import com.actiontech.dble.statistic.sql.entry.FrontendInfo;
import com.actiontech.dble.statistic.sql.entry.StatisticEntry;
import com.actiontech.dble.statistic.sql.entry.StatisticFrontendSqlEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class AssociateTablesByEntryByUserCalcHandler implements StatisticDataHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(AssociateTablesByEntryByUserCalcHandler.class);

    Map<String, Record> records = new LinkedHashMap<>(1024);
    int entryId = 0;

    @Override
    public void onEvent(StatisticEvent statisticEvent, long l, boolean b) throws Exception {
        // LOGGER.info("consuming:{}", event.getEntry().toString());
        check();
        handle(statisticEvent.getEntry());
    }

    public void check() {
        synchronized (records) {
            int removeIndex;
            if ((removeIndex = records.values().size() - StatisticManager.getInstance().getStatisticTableSize()) > 0) {
                Iterator<String> iterator = records.keySet().iterator();
                while (removeIndex-- > 0) {
                    iterator.next();
                    iterator.remove();
                }
            }
        }
    }

    public void handle(StatisticEntry entry) {
        if (entry instanceof StatisticFrontendSqlEntry) {
            StatisticFrontendSqlEntry fEntry = ((StatisticFrontendSqlEntry) entry);
            if (fEntry.getSqlType() == 7) {
                List<String> tableList = ManagerTableUtil.getTables(fEntry.getSchema(), fEntry.getSql());
                if (!tableList.isEmpty()) {
                    String tables = String.join(",", tableList);
                    String key = fEntry.getFrontend().getUser() + "-" + tables;
                    Record currRecord;
                    boolean isNew = true;
                    if (isNew = ((currRecord = records.get(key)) == null)) {
                        currRecord = new Record(++entryId, fEntry.getFrontend(), tables);
                    }
                    currRecord.addSelect(fEntry.getExaminedRows().longValue(), fEntry.getRows(), fEntry.getDuration());
                    if (isNew) {
                        records.put(key, currRecord);
                    }
                }
            }
        }
    }

    @Override
    public Map<String, Record> getList() {
        return new HashMap<>(records);
    }

    @Override
    public void clear() {
        synchronized (records) {
            records.clear();
            entryId = 0;
        }
    }

    public static class Record {
        int entry;
        String user;
        String tables;

        int selectCount = 0;
        long selectRows = 0;
        long selectExaminedRowsRows = 0L;
        long selectTime = 0L;
        long lastUpdateTime = 0L;

        public Record(int entry, FrontendInfo frontend, String tables) {
            this.entry = entry;
            user = frontend.getUser();
            this.tables = tables;
        }

        public void addSelect(long examinedRowsRow, long row, long time) {
            selectCount += 1;
            selectExaminedRowsRows += examinedRowsRow;
            selectRows += row;
            selectTime += time;
            lastUpdateTime = System.currentTimeMillis();
        }


        public int getEntry() {
            return entry;
        }

        public String getUser() {
            return user;
        }

        public String getTables() {
            return tables;
        }

        public int getSelectCount() {
            return selectCount;
        }

        public long getSelectRows() {
            return selectRows;
        }

        public long getSelectExaminedRowsRows() {
            return selectExaminedRowsRows;
        }

        public long getSelectTime() {
            return selectTime;
        }

        public long getLastUpdateTime() {
            return lastUpdateTime;
        }
    }
}