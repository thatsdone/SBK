/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.sbk.ram.impl;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.perl.api.LatencyRecord;
import io.perl.api.LatencyRecordWindow;
import io.perl.logger.Print;
import io.perl.api.ReportLatencies;
import io.perl.api.impl.TotalLatencyRecordWindow;
import io.sbk.grpc.LatenciesRecord;
import io.sbk.logger.SetRW;
import io.sbk.ram.RamPeriodicRecorder;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;

/**
 * Class RamTotalWindowLatencyPeriodicRecorder.
 */
final public class RamTotalWindowLatencyPeriodicRecorder extends TotalLatencyRecordWindow
        implements ReportLatencies, RamPeriodicRecorder {
    final private ReportLatencies reportLatencies;
    final private SetRW setRW;
    final private HashMap<Long, RW> table;

    /**
     * Constructor RamTotalWindowLatencyPeriodicRecorder initialize all values and pass all values to its upper class.
     *
     * @param window                LatencyRecordWindow
     * @param totalWindow           LatencyRecordWindow
     * @param windowLogger          Print
     * @param totalLogger           Print
     * @param reportLatencies       ReportLatencies
     * @param setRW                 SetRW
     */
    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public RamTotalWindowLatencyPeriodicRecorder(LatencyRecordWindow window, LatencyRecordWindow totalWindow,
                                                 Print windowLogger, Print totalLogger,
                                                 ReportLatencies reportLatencies,
                                                 SetRW setRW) {
        super(window, totalWindow, windowLogger, totalLogger);
        this.reportLatencies = reportLatencies;
        this.setRW = setRW;
        this.table = new HashMap<>();
    }

    @Override
    public void reportLatencyRecord(LatencyRecord record) {
        totalWindow.reportLatencyRecord(record);
        reportLatencies.reportLatencyRecord(record);

    }

    @Override
    public void reportLatency(long latency, long count) {
        totalWindow.reportLatency(latency, count);
        reportLatencies.reportLatency(latency, count);
    }

    /**
     * Record the latency.
     *
     * @param currentTime current time.
     * @param record      Record Latencies
     */
    public void record(long currentTime, LatenciesRecord record) {
        addLatenciesRecord(record);
        checkWindowFullAndReset(currentTime);
    }

    /**
     * adds latencies record.
     *
     * @param record NotNull LatenciesRecord
     */
    public void addLatenciesRecord(@NotNull LatenciesRecord record) {
        addRW(record.getClientID(), record.getReaders(), record.getWriters(),
                record.getMaxReaders(), record.getMaxWriters());

        window.update(record.getTotalRecords(), record.getTotalLatency(), record.getTotalBytes(),
                record.getInvalidLatencyRecords(), record.getLowerLatencyDiscardRecords(),
                record.getHigherLatencyDiscardRecords(), record.getValidLatencyRecords(), record.getMaxLatency());

        record.getLatencyMap().forEach(window::reportLatency);
    }

    /**
     * Method flush.
     *
     * @param currentTime   long
     */
    public void flush(long currentTime) {
        final RW rwStore = new RW();
        sumRW(rwStore);
        setRW.setReaders(rwStore.readers);
        setRW.setWriters(rwStore.writers);
        setRW.setMaxReaders(rwStore.maxReaders);
        setRW.setMaxWriters(rwStore.maxWriters);
        window.print(currentTime, windowLogger, this);
    }

    /**
     * print the periodic Latency Results.
     *
     * @param currentTime current time.
     */
    public void stopWindow(long currentTime) {
        flush(currentTime);
        checkTotalWindowFullAndReset(currentTime);
    }

    private void addRW(long key, int readers, int writers, int maxReaders, int maxWriters) {
        RW cur = table.get(key);
        if (cur == null) {
            cur = new RW();
            table.put(key, cur);
        }
        cur.update(readers, writers, maxReaders, maxWriters);
    }

    private void sumRW(RW ret) {
        table.forEach((k, data) -> {
            ret.readers += data.readers;
            ret.writers += data.writers;
            ret.maxReaders += data.maxReaders;
            ret.maxWriters += data.maxWriters;
        });
        table.clear();
    }

    private static class RW {
        public int readers;
        public int writers;
        public int maxReaders;
        public int maxWriters;

        public RW() {
            reset();
        }

        public void reset() {
            readers = writers = maxWriters = maxReaders = 0;
        }

        public void update(int readers, int writers, int maxReaders, int maxWriters) {
            this.readers = Math.max(this.readers, readers);
            this.writers = Math.max(this.writers, writers);
            this.maxReaders = Math.max(this.maxReaders, maxReaders);
            this.maxWriters = Math.max(this.maxWriters, maxWriters);
        }
    }

}