package com.wildducktheories.promise;

import com.wildducktheories.tasklet.Scheduler;


/**
 * Represents a promise to deliver a result at some future time. 
 * <p>
 * Promises are delivered on the synchronous {@link Thread} of the current {@link Thread}'s {@link Scheduler}
 * as determined by the return value of <code>SchedulerAPI.get().getScheduler()</code> at the time the callback or filter
 * is registered irrespective of which {@link Thread} resolves or rejects the promise.
 * <p>
 * @author jonseymour
 *
 * @param <P> The promised type.
 * @param <F> The failure type.
 */
public interface Promise<P, F> {
	/**
	 * Uses a {@link Filter} to transform a value of type P into a value of type Q and then return a
	 * promise for that value.
	 * <p>
	 * @param filter A filter that transforms a value of type P into a value of type Q.
	 * @return A {@link Promise} for a value of type Q.
	 */
	public <Q> Promise<Q,F> then(Filter<P,Q> filter);
	
	/**
	 * Uses a {@link Filter} to transform a value of type P into a promise for a value of type Q. 
	 * @param filter A filter that transforms a value of type P into a promise of type Q.
	 * @return A {@link Promise} for a value of type Q.
	 */
	public <Q> Promise<Q,F> thenPromise(Filter<P,Promise<Q,F>> filter);
	
	/**
	 * Register a {@link DoneCallback} that is called if and when the receiving {@link Promise} is resolved.
	 * <p>
	 * @param callback The callback to be invoked if and when the receiving {@link Promise} is resolved.
	 * @return The receiver.
	 */
	public Promise<P,F> done(DoneCallback<P> callback);
	
	
	/**
	 * Register a {@link FailCallback} that is called when the receiving promise is rejected.
	 * <p>
	 * @param callback The callback to be invoked if and when the receiving {@link Promise} is rejected.
	 * @return The receiver.
	 */
	public Promise<P,F> fail(FailCallback<F> callback);
	
	/**
	 * Register a {@link CompletionCallback} that is called when the receiving promise is resolved or rejected.
	 * <p>
	 * @param callback The callback to be invoked when the receiving {@link Promise} is resolved or rejected.
	 * @return The receiver.
	 */
	public Promise<P,F> complete(CompletionCallback<P, F> callback);
	
}
