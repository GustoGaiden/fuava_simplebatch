package com.freiheit.fuava.simplebatch.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import com.google.common.base.Strings;

public class FileUtils {

    public static final String PLACEHOLDER_DATE = "%(DATE)";

    public static void deleteDirectoryRecursively( final File dir ) throws IOException {
        Files.walkFileTree( Paths.get( dir.toURI() ), new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile( final Path file, final BasicFileAttributes attrs ) throws IOException {
                Files.delete( file );
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory( final Path dir, final IOException exc ) throws IOException {
                Files.delete( dir );
                return FileVisitResult.CONTINUE;
            }

        } );
    }

    public static String ensureTrailingSlash( final String path ) {
        return path.endsWith( File.separator )
            ? path
            : path + File.separator;
    }

    /**
     * Replaces placeholder in path.
     *
     * Currently available placeholder:
     * %(DATE) - is replaced with current date in BASIC_ISO_DATE format, f.e. YYYYMMDD
     *
     * @param path
     * @return
     */
    public static String substitutePlaceholder( final String path ) {
        return ensureTrailingSlash( path.replace( PLACEHOLDER_DATE, LocalDate.now().format( DateTimeFormatter.BASIC_ISO_DATE ) ) );
    }

}
