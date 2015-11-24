package com.freiheit.fuava.sftp;

import com.freiheit.fuava.sftp.util.RemoteConfiguration;
import com.freiheit.fuava.simplebatch.util.FileUtils;

/**
 * The SFTP-Server Configuration.
 *
 * @author dmitrijs.barbarins@freiheit.com
 */
public class RemoteConfigurationImpl implements RemoteConfiguration {


    private final String remoteFilesIncomingFolder;
    private final String remoteProcessingFolder;
    private final String remoteSkippedFolder;
    private final String remoteArchivedFolder;

    /**
     * Server configuration for sftp.
     * NOTE: adds current date to the archived and skipped folders.
     *
     * @param remoteFilesIncomingFolder location of files located on sftp server
     * @param remoteProcessingFolder location of files being processed on sftp server
     * @param remoteSkippedFolder location of files being skipped on sftp server
     * @param remoteArchivedFolder location of files have been downloaded successfully from sftp server
     *
     */
    public RemoteConfigurationImpl(final String remoteFilesIncomingFolder, final String remoteProcessingFolder, final String remoteSkippedFolder,
                                   final String remoteArchivedFolder) {

        this.remoteArchivedFolder = FileUtils.ensureTrailingSlash( remoteArchivedFolder );
        this.remoteFilesIncomingFolder = FileUtils.ensureTrailingSlash( remoteFilesIncomingFolder );
        this.remoteProcessingFolder = FileUtils.ensureTrailingSlash( remoteProcessingFolder );
        this.remoteSkippedFolder = FileUtils.ensureTrailingSlash( remoteSkippedFolder );

    }


    public String getIncomingFolder() {
        return remoteFilesIncomingFolder;
    }

    public String getProcessingFolder() {
        return remoteProcessingFolder;
    }

    public String getSkippedFolder() {
        return remoteSkippedFolder;
    }

    public String getArchivedFolder() {
        return remoteArchivedFolder;
    }
}
