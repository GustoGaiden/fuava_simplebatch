package com.freiheit.fuava.simplebatch.persist;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.freiheit.fuava.simplebatch.result.Result;
import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;


/**
 * @param <Input>
 * @param <Output>
 */
public class ControlFilePersistence<Input> extends SingleItemPersistence<Input, FilePersistenceOutputInfo, ControlFilePersistenceOutputInfo> {
    private static final Logger LOG = LoggerFactory.getLogger( ControlFilePersistence.class );
    
    public interface Configuration {
    	String getDownloadDirPath();
    	String getControlFileEnding();
    }

    
	private Configuration config;
	private File basedir;
    
    public ControlFilePersistence(Configuration config) {
		this.config = config;
		basedir = new File( Preconditions.checkNotNull(config.getDownloadDirPath()) );
	}
    

    @Override
    public Result<Input, ControlFilePersistenceOutputInfo> persistItem(Result<Input, FilePersistenceOutputInfo> r) {
		if (r.isFailed()) {
			return Result.<Input, ControlFilePersistenceOutputInfo>builder(r).failed();
		}
		Input input = r.getInput();
		try {		
			File f = r.getOutput().getDataFile();
			
			final File ctl = new File( basedir + "/" + String.valueOf( System.currentTimeMillis() ) + "_done" + config.getControlFileEnding() );
			LOG.info("Writing control file " + ctl);
			try ( OutputStreamWriter fos2 = new OutputStreamWriter( new FileOutputStream( ctl ), Charsets.UTF_8.name() ) ) {
				fos2.write( f.getName() );
			}
			return Result.success(input, new ControlFilePersistenceOutputInfo(ctl));

		} catch ( final Throwable t ) {
			return Result.failed(input, t);
		}
	}
}
