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
package com.freiheit.fuava.simplebatch.fsjobs.importer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

import com.google.common.base.Charsets;
import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

/**
 * @author tim.lessner@freiheit.com
 */
public class ReadControlFileFunction implements Function<Path, ControlFile> {

    private final Path baseDir;

    public ReadControlFileFunction( final Path baseDir ) {
        this.baseDir = baseDir;
    }

    private ControlFile parseNewFormat( final Path path, final BufferedReader br ) throws IOException {
        final Properties prop = new Properties();
        prop.load( br );
        final String controlledFileName = prop.getProperty( "file" );
        final String logFileName = Preconditions.checkNotNull(prop.getProperty( "log" ), "Log file property is mandatory" );
        final String status = prop.getProperty( "status" );
        final boolean downloadFailed = "DOWNLOAD_FAILED".equals( status );
        return new ControlFile( 
                this.baseDir, 
                Strings.isNullOrEmpty( controlledFileName ) ? Paths.get( "" ) : Paths.get( controlledFileName ) , 
                Paths.get( logFileName ), 
                path, 
                downloadFailed 
            );
    }

    @Override
    public ControlFile apply( final Path path ) {
        try {
            final File file = path.toFile();
            final Reader in = new InputStreamReader( new FileInputStream( file ), Charsets.UTF_8 );
            try ( BufferedReader br = new BufferedReader( in ) ) {
                final String firstLine = br.readLine();
                if ( Strings.isNullOrEmpty( firstLine ) ) {
                    throw new IllegalArgumentException( "The Control-File " + file + " has no content" );
                } else if ( firstLine.startsWith( "#!VERSION=1" ) ) {
                    return parseNewFormat( path, br );
                } else {
                    final Path controlledFileName = Paths.get( firstLine );
                    final Path logFileName = Paths.get( controlledFileName + ".log" );
                    return new ControlFile( this.baseDir, controlledFileName, logFileName, path );
                }
            }
        } catch ( final IOException e ) {
            throw new RuntimeException( e );
        }
    }
}
