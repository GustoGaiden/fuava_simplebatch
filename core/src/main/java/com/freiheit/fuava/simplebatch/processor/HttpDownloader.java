package com.freiheit.fuava.simplebatch.processor;

import java.io.InputStream;

import org.apache.http.client.HttpClient;

import com.freiheit.fuava.simplebatch.http.HttpDownloaderSettings;
import com.freiheit.fuava.simplebatch.http.HttpFetcher;
import com.freiheit.fuava.simplebatch.result.Result;
import com.google.common.base.Function;

class HttpDownloader<Input, Id, T> extends AbstractSingleItemProcessor<Input, Id, T> {

    private final HttpFetcher fetcher;
    private final HttpDownloaderSettings<Id> settings;
    private final Function<InputStream, T> converter;

    public HttpDownloader(
            final HttpClient client,
            final HttpDownloaderSettings<Id> settings,
            final Function<InputStream, T> converter
            ) {
        this.fetcher = new HttpFetcher( client );
        this.settings = settings;
        this.converter = converter;
    }

    @Override
    public Result<Input, T> processItem(Result<Input, Id> data) {
        if (data.isFailed()) {
            return Result.<Input, T>builder(data).failed();
        }
        Input input = data.getInput();
        Id id = data.getOutput();
        try {
            final T result = fetcher.fetch( converter, settings.createFetchUrl( id ), settings.getRequestHeaders() );
            return Result.success( input, result );
        } catch ( Throwable e ) {
            return Result.failed( input , e );
        }

    }
}