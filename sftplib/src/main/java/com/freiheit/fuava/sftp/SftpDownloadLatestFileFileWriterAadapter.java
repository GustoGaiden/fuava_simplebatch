package com.freiheit.fuava.sftp;

import com.freiheit.fuava.sftp.util.ConvertUtil;
import com.freiheit.fuava.simplebatch.fetch.FetchedItem;
import com.freiheit.fuava.simplebatch.processor.FileOutputStreamAdapter;
import com.freiheit.fuava.simplebatch.result.Result;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicLong;

/**
 * The file writer adapter streams data and writes it to the predefined downloading directory.
 *
 * @author Thomas Ostendorf (thomas.ostendorf@freiheit.com)
 */
public class SftpDownloadLatestFileFileWriterAadapter implements FileOutputStreamAdapter<FetchedItem<SftpFilename>, InputStream> {
    private final String prefix = "" + System.currentTimeMillis();
    private final AtomicLong counter = new AtomicLong();


    @Override
    public String getFileName( final Result<FetchedItem<SftpFilename>, InputStream> result ) {
        final String filename = result.getInput().getValue().getFilename();
        return prefix + "_" + filename + "_" + counter.incrementAndGet();
    }
    /**
     *
     * @param outputStream data written from the sftp server
     * @param inputStream data from the sftp server
     * @throws IOException
     */
    @Override
    public void writeToStream( final OutputStream outputStream, final InputStream inputStream ) throws IOException {
        ConvertUtil.copyLarge( inputStream, outputStream );
    }


}
