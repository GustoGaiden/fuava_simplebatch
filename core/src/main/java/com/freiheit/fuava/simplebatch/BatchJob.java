package com.freiheit.fuava.simplebatch;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.annotation.CheckReturnValue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.freiheit.fuava.simplebatch.fetch.FailsafeFetcherImpl;
import com.freiheit.fuava.simplebatch.fetch.Fetcher;
import com.freiheit.fuava.simplebatch.persist.Persistence;
import com.freiheit.fuava.simplebatch.process.MapBuildingFunction;
import com.freiheit.fuava.simplebatch.process.Processor;
import com.freiheit.fuava.simplebatch.process.RetryingProcessor;
import com.freiheit.fuava.simplebatch.result.DelegatingProcessingResultListener;
import com.freiheit.fuava.simplebatch.result.ProcessingResultListener;
import com.freiheit.fuava.simplebatch.result.Result;
import com.freiheit.fuava.simplebatch.result.ResultStatistics;
import com.google.common.base.Function;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;

/**
 * Downloads - or more generally, processes - data in two stages, via iterables.
 * 
 * The output of the first stage is used as input for the second stage.
 * 
 * A typical usecase would be to fetch Ids or URLs of the data to download in
 * the first stage ('source'), and then to fetch the data in the second stage
 * ('process'), persisting the data and the progress information in the third
 * stage 'sink'.
 * 
 * The downloader uses iterables and does the stage 2 downloading in batches, so
 * you can provide iterables that stream over some data source, efficiently
 * processing huge amounts of data.
 * 
 * @author Klas Kalass <klas.kalass@freiheit.com>
 *
 * @param <Input>
 *            The data fetched in stage one
 * @param <Output>
 *            The data fetched instage two
 */
public final class BatchJob<Input, Output> {
    static final Logger LOG = LoggerFactory.getLogger( BatchJob.class );

    private int processingBatchSize;
    private Fetcher<Input> fetcher;
    private Processor<Input, Output> processor;
    private Persistence<Input, Output> persistence;

	private ArrayList<ProcessingResultListener<Input, Output>> listeners = new ArrayList<ProcessingResultListener<Input, Output>>();

	public BatchJob<Input, Output> setProcessingBatchSize( int processingBatchSize ) {
        this.processingBatchSize = processingBatchSize;
        return this;
    }

    public BatchJob<Input, Output> setFetcher( Fetcher<Input> idsFetcher ) {
        this.fetcher = idsFetcher;
        return this;
    }

    /**
     * Overloaded version of {@link #setFetcher(Fetcher)}. If the Iterable throws exceptions, they will 
     * be caught and translated into result instances.
     * 
     * @param idsFetcher
     * @return
     */
    public BatchJob<Input, Output> setFetcher( Iterable<Input> idsFetcher ) {
        this.fetcher = new FailsafeFetcherImpl<Input>(Suppliers.ofInstance(idsFetcher));
        return this;
    }

    /**
     * Overloaded version of {@link #setFetcher(Fetcher)}. If the Iterable returned by the supplier throws exceptions, they will 
     * be caught and translated into result instances.
     * 
     * @param idsFetcher
     * @return
     */
    public BatchJob<Input, Output> setFetcher( Supplier<Iterable<Input>> idsFetcher ) {
        this.fetcher = new FailsafeFetcherImpl<Input>(idsFetcher);
        return this;
    }

    public BatchJob<Input, Output> setProcessor( Processor<Input, Output> byIdsFetcher ) {
        this.processor = byIdsFetcher;
        return this;
    }

    /**
     * Convenience alternative to {@link #setProcessor(Processor)} which works with a function of lists to maps, 
     * that will be re-executed with singletons in case of exceptions. Thus, your implementation may need
     * to handle transactions and rollbacks.
     * 
     */
    public BatchJob<Input, Output> setRetryableProcessor( Function<List<Input>, Map<Input, Output>> retryableFunction) {
        this.processor = new RetryingProcessor<Input, Output>(retryableFunction);
        return this;
    }

    /**
     * Convenience alternative to {@link #setProcessor(Processor)} which works with a function of lists to lists, 
     * that will be re-executed with singletons in case of exceptions. Thus, your implementation may need
     * to handle transactions and rollbacks.
     * 
	 * The Function must produce on output item for each input item and keep the order.
	 * Additionally, it must only be used for data where each input item is distinct and 
	 * no duplicates exist within one input.
     * 
     */
    public BatchJob<Input, Output> setRetryableListProcessor( Function<List<Input>, List<Output>> retryableFunction) {
        this.processor = new RetryingProcessor<Input, Output>(new MapBuildingFunction<Input, Output>(retryableFunction));
        return this;
    }

    public BatchJob<Input, Output> setPersistence( Persistence<Input, Output> writer ) {
        this.persistence = writer;
        return this;
    }
    
    public BatchJob<Input, Output> addListener(ProcessingResultListener<Input, Output> listener) {
    	this.listeners.add(listener);
    	return this;
    }

    public BatchJob<Input, Output> removeListener(ProcessingResultListener<Input, Output> listener) {
    	this.listeners.remove(listener);
    	return this;
    }

    @CheckReturnValue
    public final ResultStatistics<Input, Output> run() {

    	ResultStatistics.Builder<Input, Output> resultBuilder = ResultStatistics.builder();
    	
    	DelegatingProcessingResultListener<Input, Output> listeners = new DelegatingProcessingResultListener<Input, Output>(
    			ImmutableList.<ProcessingResultListener<Input, Output>>builder().add(resultBuilder).addAll(this.listeners).build()
			);
    	
        final Iterable<Result<?, Input>> sourceIterable = fetcher.fetchAll();

        for ( List<Result<?, Input>> sourceResults : Iterables.partition( sourceIterable, processingBatchSize ) ) {
            
            listeners.onFetchResults(sourceResults);
            
            final List<Input> processingInputs =
                    FluentIterable.from( sourceResults ).filter( Result::isSuccess ).transform( Result::getOutput ).toList();
            final Iterable<Result<Input, Output>> processingResults = processor.process( processingInputs );

            listeners.onProcessingResults(processingResults);
            
            final List<Result<Input, Output>> persistInputs = FluentIterable.from(processingResults).filter(Result::isSuccess).toList();
            Iterable<? extends Result<Input, ?>> persistResults = persistence.persist( persistInputs );
            
            listeners.onPersistResults(persistResults);
        }

        resultBuilder.setListenerDelegationFailures(listeners.hasDelegationFailures());
        
        // TODO: persist the state of the downloader (offset or downloader), so it can be
        //       provided the next time
        //idsDownloader.getWriteableState();
        return resultBuilder.build();
    }
}