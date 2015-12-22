package com.freiheit.fuava.simplebatch.logging;

import java.nio.file.Paths;

import com.freiheit.fuava.simplebatch.fetch.FetchedItem;
import com.freiheit.fuava.simplebatch.fsjobs.importer.ControlFile;
import com.freiheit.fuava.simplebatch.result.ProcessingResultListener;
import com.freiheit.fuava.simplebatch.result.Result;
import com.freiheit.fuava.simplebatch.result.ResultStatistics;

public class ImportFileJsonLoggingListener implements ProcessingResultListener<ControlFile, ResultStatistics> {

    private final String downloadDir;
    private final String archivedDir;
    private final String failedDir;

    public ImportFileJsonLoggingListener( final String downloadDir, final String archivedDir, final String failedDir ) {
        this.downloadDir = downloadDir;
        this.archivedDir = archivedDir;
        this.failedDir = failedDir;
    }

    @Override
    public void onFetchResult( final Result<FetchedItem<ControlFile>, ControlFile> result ) {
        FetchedItem<ControlFile> fetchedItem = result.getInput();
        ControlFile value = fetchedItem == null ? null : fetchedItem.getValue();
        final String logFileName = value == null ? "failed_control_files.log": value.getLogFileName();
        final JsonLogger l = new JsonLogger( Paths.get( downloadDir, logFileName ) );
        l.logImportStart();
    }

    @Override
    public void onProcessingResult( final Result<FetchedItem<ControlFile>, ResultStatistics> result ) {
        final String logFileName = result.getInput().getValue().getLogFileName();
        final String dir = result.isSuccess()
            ? archivedDir
            : failedDir;
        final JsonLogger l = new JsonLogger( Paths.get( dir, logFileName ) );
        l.logImportEnd( result.isSuccess(), result.getAllMessages() );
    }
}