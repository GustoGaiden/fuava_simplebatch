/*
 * (c) Copyright 2015 freiheit.com technologies GmbH
 *
 * Created on 15.07.15 by tim.lessner@freiheit.com
 *
 * This file contains unpublished, proprietary trade secret information of
 * freiheit.com technologies GmbH. Use, transcription, duplication and
 * modification are strictly prohibited without prior written consent of
 * freiheit.com technologies GmbH.
 */

package com.freiheit.fuava.simplebatch;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.freiheit.fuava.simplebatch.processor.Processor;
import com.freiheit.fuava.simplebatch.processor.Processors;
import com.freiheit.fuava.simplebatch.result.Result;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

/**
 * @author tim.lessner@freiheit.com
 */
@Test
public class TestComposition {

    private static class FileContentPair {
        public final File file;
        public final String content;

        public FileContentPair( final File file, final String content ) {
            this.file = file;
            this.content = content;
        }
    }

    private final File exF1 = new File( "/tmp/exF1" );
    private final File exF2 = new File( "/tmp/exF2" );
    private final File exF3 = new File( "/tmp/exF3" );

    private final String f1String = "exF1";
    private final String f2String = "exF2";
    private final String f3String = "exF3";

    final ImmutableMap<String, FileContentPair> testFileContentPairs = ImmutableMap.of(
            f1String, new FileContentPair( exF1, f1String ),
            f2String, new FileContentPair( exF2, f2String ),
            f3String, new FileContentPair( exF3, f3String )
            );

    final ImmutableList<Result<File, File>> nonExistingFiles = ImmutableList.of(
            asResult( new File( "/tmp/a/a" ) )
            );

    @BeforeClass
    public void makeTestData() throws IOException {
        for ( final FileContentPair fileContentPair : testFileContentPairs.values() ) {
            try ( FileOutputStream fos = new FileOutputStream( fileContentPair.file ) ) {
                fos.write( fileContentPair.content.getBytes() );
            }
        }
    }

    private <T> Result<T, T> asResult( final T data ) {
        return Result.success( data, data );
    }

    @Test
    public void testComposition() {
        final Processor<File, File, String> compose = makeComposedProcessorFileStringProcessor();
        final List<Result<File, File>> testFiles = testFileContentPairs
                .values()
                .stream()
                .map( fileContentPair -> fileContentPair.file )
                .map( file -> Result.success( file, file ) )
                .collect( Collectors.toList() );
        final Iterable<Result<File, String>> processed = compose.process( testFiles );

        for ( final Result r : processed ) {
            Assert.assertFalse( r.isFailed(), "Failed result did not fail!" );
            final FileContentPair fileContentPair = testFileContentPairs.get( r.getOutput() );
            Assert.assertNotNull( fileContentPair );
        }

    }

    @Test
    public void testForFailures() {
        final Processor<File, File, String> compose = makeComposedProcessorFileStringProcessor();

        final Iterable<Result<File, String>> processed = compose.process( nonExistingFiles );
        Assert.assertEquals( Arrays.asList( processed ).size(), 1 );

        for ( final Result r : processed ) {
            Assert.assertTrue( r.isFailed(), "Failed result did not fail!" );
        }
    }

    private Processor<File, File, String> makeComposedProcessorFileStringProcessor() {
        final Processor<File, File, File> prepareControlledFileProcessor = Processors.fileMover( "/tmp" );
        final Processor<File, File, String> readFilesToStringTestProcessor = makeReadFilesToStringTestProcessor();

        return Processors.compose(
                readFilesToStringTestProcessor,
                prepareControlledFileProcessor
                );
    }

    private Processor<File, File, String> makeReadFilesToStringTestProcessor() {
        return Processors.singleItemFunction( new Function<File, String>() {
            @Nullable
            @Override
            public String apply( final File input ) {
                return input.getName();
            }
        } );
    }
}
