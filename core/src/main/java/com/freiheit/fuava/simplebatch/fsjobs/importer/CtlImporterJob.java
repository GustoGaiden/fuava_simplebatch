package com.freiheit.fuava.simplebatch.fsjobs.importer;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import com.freiheit.fuava.simplebatch.BatchJob;
import com.freiheit.fuava.simplebatch.fetch.Fetcher;
import com.freiheit.fuava.simplebatch.fetch.Fetchers;
import com.freiheit.fuava.simplebatch.fsjobs.downloader.CtlDownloaderJob;
import com.freiheit.fuava.simplebatch.logging.BatchStatisticsLoggingListener;
import com.freiheit.fuava.simplebatch.logging.ItemProgressLoggingListener;
import com.freiheit.fuava.simplebatch.processor.Processor;
import com.freiheit.fuava.simplebatch.processor.Processors;
import com.freiheit.fuava.simplebatch.result.ProcessingResultListener;
import com.google.common.base.Function;

/**
 * An importer that imports files from the file system, adhering to the control file protocol.
 * @author klas
 *
 * @param <ProcessedData>
 */
public class CtlImporterJob<Data>  extends BatchJob<ControlFile, Iterable<Data>> {


    public interface Configuration {
        String getControlFileEnding();
        String getDownloadDirPath();
        String getArchivedDirPath();
        String getProcessingDirPath();
        String getFailedDirPath();
    }

    public static final class ConfigurationImpl implements Configuration {

        private String downloadDirPath = CtlDownloaderJob.DEFAULT_CONFIG_DOWNLOAD_DIR_PATH;
        private String archivedDirPath = "/tmp/archive/";
        private String processingDirPath = "/tmp/processing/";
        private String failedDirPath = "/tmp/failed/";
        private String controlFileEnding = CtlDownloaderJob.DEFAULT_CONFIG_CONTROL_FILE_ENDING;

        @Override
        public String getDownloadDirPath() {
            return downloadDirPath;
        }

        public ConfigurationImpl setDownloadDirPath(String downloadDirPath) {
            this.downloadDirPath = downloadDirPath;
            return this;
        }

        @Override
        public String getArchivedDirPath() {
            return archivedDirPath;
        }

        public ConfigurationImpl setArchivedDirPath(String archivedDirPath) {
            this.archivedDirPath = archivedDirPath;
            return this;
        }

        @Override
        public String getFailedDirPath() {
            return failedDirPath;
        }

        public ConfigurationImpl setFailedDirPath(String failedDirPath) {
            this.failedDirPath = failedDirPath;
            return this;
        }

        @Override
        public String getProcessingDirPath() {
            return processingDirPath;
        }

        public ConfigurationImpl setProcessingDirPath(String processingDirPath) {
            this.processingDirPath = processingDirPath;
            return this;
        }

        @Override
        public String getControlFileEnding() {
            return controlFileEnding;
        }

        public ConfigurationImpl setControlFileEnding(String controlFileEnding) {
            this.controlFileEnding = controlFileEnding;
            return this;
        }

    }


    public static final class Builder<Data> {
        private static final String LOG_NAME_FILE_PROCESSING_ITEM = "FILE";
        private static final String LOG_NAME_FILE_PROCESSING_BATCH = "PROCESSED FILES";
        private static final String LOG_NAME_CONTENT_PROCESSING_ITEM = "CONTENT";
        private static final String LOG_NAME_CONTENT_PROCESSING_BATCH = "PROCESSED CONTENT";
        private Configuration configuration;
        private int processingBatchSize = 1000;
        private Function<InputStream, Iterable<Data>> documentReader;
        private final List<ProcessingResultListener<ControlFile, Iterable<Data>>> fileProcessingListeners = new ArrayList<>();

        private final List<ProcessingResultListener<Data, Data>> contentProcessingListeners = new ArrayList<>();
        private Processor<Data, Data, Data> contentPersistence;
        private String description;

        public Builder() {
        }

        /**
         * Controls settings like download directory, processing directory etc.
         */
        public Builder<Data> setConfiguration(Configuration configuration) {
            this.configuration = configuration;
            return this;
        }

        public Builder<Data> setDescription(String description) {
            this.description= description;
            return this;
        }


        /**
         * Controls the number of Data items which will be passed
         * to the content processing stage in one list.
         *
         * Note that your file may contain more items. The partitioning into lists is lazy.
         *
         */
        public Builder<Data> setContentBatchSize(int processingBatchSize) {
            this.processingBatchSize = processingBatchSize;
            return this;
        }


        /**
         * The given function is used for each file to convert the contents of that file (accessed by an {@link InputStream})
         * into an iterable.
         *
         * Note that your Iterable will be processed lazily by creating partitions of size {@link #setContentBatchSize(int)}.
         * After that, it will be passed to the persistence configured in {@link #setContentProcessor(Processor)}.
         *
         */
        public Builder<Data> setFileInputStreamReader(Function<InputStream, Iterable<Data>> documentReader) {
            this.documentReader = documentReader;
            return this;
        }

        /**
         * Controls how to process (or where to store) a batch of the data read from your file.
         *
         * The size of the list which is passed to this persistence is controlled by {@link #setContentBatchSize(int)}
         */
        public Builder<Data> setContentProcessor(Processor<Data, Data, Data> persistence) {
            contentPersistence = persistence;
            return this;
        }


        public Builder<Data> addFileProcessingListener(ProcessingResultListener<ControlFile, Iterable<Data>> listener) {
            fileProcessingListeners.add(listener);
            return this;
        }


        public Builder<Data> addContentProcessingListener(ProcessingResultListener<Data, Data> listener) {
            contentProcessingListeners.add(listener);
            return this;
        }

        public CtlImporterJob<Data> build() {
            fileProcessingListeners.add( new BatchStatisticsLoggingListener<ControlFile, Iterable<Data>>(LOG_NAME_FILE_PROCESSING_BATCH) );
            fileProcessingListeners.add( new ItemProgressLoggingListener<ControlFile, Iterable<Data>>(LOG_NAME_FILE_PROCESSING_ITEM) );

            contentProcessingListeners.add( new BatchStatisticsLoggingListener<Data, Data>(LOG_NAME_CONTENT_PROCESSING_BATCH) );
            contentProcessingListeners.add( new ItemProgressLoggingListener<Data, Data>(LOG_NAME_CONTENT_PROCESSING_ITEM) );

            final BatchJob.Builder<Data, Data> builder = BatchJob.<Data, Data>builder()
                    .setProcessingBatchSize(processingBatchSize)
                    .setPersistence(contentPersistence);

            for (ProcessingResultListener<Data, Data> l: contentProcessingListeners) {
                builder.addListener(l);
            }



            final Function<File, Iterable<Data>> fileProcessorFunction = new FileToInputStreamFunction<>(this.documentReader);
            final Processor<ControlFile, File, Iterable<Data>> fileProcessor = Processors.singleItemFunction(fileProcessorFunction);
            final Processor<ControlFile, ControlFile, File> controlledFileMover = Processors.controlledFileMover(configuration.getProcessingDirPath());
            final Processor<ControlFile, ControlFile, Iterable<Data>> fileProcessorAndMover = Processors.compose(fileProcessor, controlledFileMover);

            final Processor<ControlFile, Iterable<Data>, Iterable<Data>> innerJobProcessor = Processors.runBatchJobProcessor(
                    new Function<ControlFile, String>() {

                        @Override
                        public String apply( ControlFile ctl) {
                            return ctl.getControlledFileName();
                        }
                    }, builder
                    );

            Processor<ControlFile, ControlFile, Iterable<Data>> processfileAndMove = Processors.compose(innerJobProcessor, fileProcessorAndMover);

            final Processor<ControlFile, Iterable<Data>, Iterable<Data>> fileMovingPersistence = new FileMovingPersistence<Iterable<Data>>(
                    configuration.getProcessingDirPath(),
                    configuration.getArchivedDirPath(),
                    configuration.getFailedDirPath()
                    );
            Processor<ControlFile, ControlFile, Iterable<Data>> processor = Processors.compose(fileMovingPersistence, processfileAndMove);

            return new CtlImporterJob<Data>(
                    description,
                    1 /*process one file at a time, no use for batching*/,
                    Fetchers.folderFetcher(this.configuration.getDownloadDirPath(), this.configuration.getControlFileEnding(), new ReadControlFileFunction(this.configuration.getDownloadDirPath())),
                    processor,
                    fileProcessingListeners);
        }
    }



    private CtlImporterJob(
            String description,
            int processingBatchSize,
            Fetcher<ControlFile> fetcher,
            Processor<ControlFile, ControlFile, Iterable<Data>> processor,
            List<ProcessingResultListener<ControlFile, Iterable<Data>>> listeners) {
        super(description, processingBatchSize, fetcher, processor, listeners);
    }


}
