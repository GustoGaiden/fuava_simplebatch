/*
 * (c) Copyright 2015 freiheit.com technologies GmbH
 *
 * Created on 14.07.15 by tim.lessner@freiheit.com
 *
 * This file contains unpublished, proprietary trade secret information of
 * freiheit.com technologies GmbH. Use, transcription, duplication and
 * modification are strictly prohibited without prior written consent of
 * freiheit.com technologies GmbH.
 */

package com.freiheit.fuava.simplebatch.fsjobs.importer;

import java.io.File;

/**
 * @author tim.lessner@freiheit.com
 */
public class ControlFile {

    private final String pathToControlledFile;
    private final File file;

    public ControlFile(final String pathToControlledFile, final File file) {
        this.pathToControlledFile = pathToControlledFile;
        this.file = file;
    }

    public String getPathToControlledFile() {
        return pathToControlledFile;
    }

    public File getFile() {
        return file;
    }
}
