package com.freiheit.fuava.simplebatch.persist;

import java.io.File;
import java.io.FileWriter;
import java.io.OutputStreamWriter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.freiheit.fuava.simplebatch.result.Result;
import com.google.common.base.Preconditions;


/**
 * @param <Input>
 * @param <Output>
 */
public class FilePersistence<Input, Output> extends SingleItemPersistence<Input, Output, FilePersistenceOutputInfo> {
    private static final Logger LOG = LoggerFactory.getLogger( FilePersistence.class );
    public interface Configuration {
    	String getDownloadDirPath();
    }


    private PersistenceAdapter<Input, Output> adapter;
	private File basedir;
    
    public FilePersistence(Configuration configuration, PersistenceAdapter<Input, Output> adapter) {
		this.adapter = Preconditions.checkNotNull(adapter);
		this.basedir = new File( Preconditions.checkNotNull(configuration.getDownloadDirPath()) );
	}
    
    @Override
    public Result<Input, FilePersistenceOutputInfo> persistItem(Result<Input, Output> r) {
    	if (r.isFailed()) {
			return Result.<Input, FilePersistenceOutputInfo>builder(r).failed();
		}
		
    	Input input = r.getInput();
		File f = null;
		try {
			String itemDescription = adapter.getFileName(r);
			f = new File( basedir, itemDescription);
			LOG.info("Writing data file " + f  + " (exists: " + f.exists()  + ") " + trimOut(r.getOutput()));
		    try ( OutputStreamWriter fos = new FileWriter( f ) ) {
		    	adapter.write(fos, r.getOutput());
		    	fos.flush();
		    }
			if (!f.exists()) {
				return Result.failed(input, "Control file does not exist after write: " + f);
			}
			LOG.info("Wrote data file " + f);
			return Result.success(input, new FilePersistenceOutputInfo(f));

		} catch ( final Throwable t ) {
			return Result.failed(input, "Failed writing to file " + (f == null ? null : f.getAbsolutePath()), t);
		}
	}

	private String trimOut(Output output) {
		String os = output == null ? "null" : output.toString();
		return output == null? "null" : os.substring(0,  Math.min(20, os.length()));
	}
}
