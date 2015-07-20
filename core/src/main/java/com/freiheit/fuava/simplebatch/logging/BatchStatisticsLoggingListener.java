/*
 * (c) Copyright 2015 freiheit.com technologies GmbH
 *
 * Created on 16.07.15 by tim.lessner@freiheit.com
 *
 * This file contains unpublished, proprietary trade secret information of
 * freiheit.com technologies GmbH. Use, transcription, duplication and
 * modification are strictly prohibited without prior written consent of
 * freiheit.com technologies GmbH.
 */

package com.freiheit.fuava.simplebatch.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.freiheit.fuava.simplebatch.fetch.FetchedItem;
import com.freiheit.fuava.simplebatch.result.ProcessingResultListener;
import com.freiheit.fuava.simplebatch.result.Result;
import com.google.common.collect.FluentIterable;

/**
 * Logs the {@link ResultBatchStat} of processing a single batch.
 *
 * @author tim.lessner@freiheit.com
 */
public class BatchStatisticsLoggingListener<Input, Output> implements ProcessingResultListener<Input, Output> {
    private final Logger log;

    public BatchStatisticsLoggingListener( final String logFileName ) {
        log = LoggerFactory.getLogger( logFileName );
    }

    @Override
    public void onFetchResults( final Iterable<Result<FetchedItem<Input>, Input>> result ) {
        final int failed = FluentIterable.from( result ).filter( Result::isFailed ).size();
        final int success = FluentIterable.from( result ).filter( Result::isSuccess ).size();
        log.info( ResultBatchStat.of( Event.FETCH, failed, success ) );
    }

    @Override
    public void onProcessingResults( final Iterable<? extends Result<FetchedItem<Input>, Output>> iterable ) {
        final int failed = FluentIterable.from( iterable ).filter( Result::isFailed ).size();
        final int success = FluentIterable.from( iterable ).filter( Result::isSuccess ).size();
        log.info( ResultBatchStat.of( Event.PERSIST, failed, success ) );

    }
}
