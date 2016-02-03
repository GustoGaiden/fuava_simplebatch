/**
 * Copyright 2015 freiheit.com technologies gmbh
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.freiheit.fuava.simplebatch.fetch;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.Map;

import org.apache.http.client.HttpClient;

import com.freiheit.fuava.simplebatch.http.HttpFetcher;
import com.freiheit.fuava.simplebatch.http.HttpFetcherImpl;
import com.freiheit.fuava.simplebatch.http.HttpPagingFetcher;
import com.freiheit.fuava.simplebatch.http.PagingRequestSettings;
import com.google.common.base.Function;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.Ordering;

public class Fetchers {

    /**
     * Uses the given iterable to provide the data that will be passed on to the
     * processing stage, treating each item as successful.
     */
    public static final <OriginalInput> Fetcher<OriginalInput> iterable( final Iterable<OriginalInput> iterable ) {
        return new SuppliedIterableFetcher<OriginalInput>( Suppliers.ofInstance( iterable ) );
    }

    /**
     * Uses the given fetcher to get an iterable that provides the data that
     * will be passed on to the processing stage, treating each item as
     * successful.
     */
    public static final <OriginalInput> Fetcher<OriginalInput> supplied( final Supplier<Iterable<OriginalInput>> supplier ) {
        return new SuppliedIterableFetcher<OriginalInput>( supplier );
    }

    /**
     * A Fetcher that uses an http request to retrieve the data to process.
     */
    public static <OriginalInput> Fetcher<OriginalInput> httpFetcher(
            final HttpClient client,
            final String uri,
            final Map<String, String> headers,
            final Function<InputStream, Iterable<OriginalInput>> converter
            ) {
        return httpFetcher( new HttpFetcherImpl( client ), uri, headers, converter );
    }

    public static <OriginalInput> Fetcher<OriginalInput> httpFetcher(
            final HttpFetcher httpFetcher,
            final String uri,
            final Map<String, String> headers,
            final Function<InputStream, Iterable<OriginalInput>> converter
            ) {
        return new SuppliedIterableFetcher<OriginalInput>( new Supplier<Iterable<OriginalInput>>() {

            @Override
            public Iterable<OriginalInput> get() {
                return httpFetcher.fetch( converter, uri, headers );
            }
        } );
    }

    /**
     * Creates a Fetcher which fetches the Items lazily via http, always
     * requesting a page of the data and transparently continuing to the next
     * page.
     */
    public static <OriginalInput> Fetcher<OriginalInput> httpPagingFetcher(
            final HttpClient client,
            final PagingRequestSettings<Iterable<OriginalInput>> settings,
            final Function<? super InputStream, Iterable<OriginalInput>> converter,
            final int initialFrom,
            final int pageSize
            ) {
        return httpPagingFetcher( new HttpFetcherImpl( client ), settings, converter, initialFrom, pageSize );

    }

    /**
     * Creates a Fetcher which fetches the Items lazily via http, always
     * requesting a page of the data and transparently continuing to the next
     * page.
     */
    public static <OriginalInput> Fetcher<OriginalInput> httpPagingFetcher(
            final HttpFetcher fetcher,
            final PagingRequestSettings<Iterable<OriginalInput>> settings,
            final Function<? super InputStream, Iterable<OriginalInput>> converter,
            final int initialFrom,
            final int pageSize
            ) {
        return new HttpPagingFetcher<OriginalInput>( fetcher, settings, converter, initialFrom, pageSize );

    }


    /**
     * Iterates over all files in the given directories that end with the
     * specified fileEnding and converts them with the given function.
     */
    public static <OriginalInput> Fetcher<OriginalInput> folderFetcher( final Function<Path, OriginalInput> fileFunction, final DownloadDir dir, final DownloadDir... moreDirs ) {
        return new DirectoryFileFetcher<OriginalInput>( fileFunction, DirectoryFileFetcher.ORDERING_FILE_BY_PATH, dir, moreDirs );
    }

    /**
     * Iterates over all files in the given directories that end with the
     * specified fileEnding and converts them with the given function.
     */
    public static <OriginalInput> Fetcher<OriginalInput> folderFetcher( final Function<Path, OriginalInput> fileFunction, final Ordering<Path> fileOrdering, final DownloadDir dir, final DownloadDir... moreDirs ) {
        return new DirectoryFileFetcher<OriginalInput>( fileFunction, fileOrdering, dir, moreDirs );
    }

}
