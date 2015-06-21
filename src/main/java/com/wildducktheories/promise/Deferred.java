package com.wildducktheories.promise;

/**
 * Represents a means of resolving or rejecting a deferred 
 * calculation. To obtain the result, a caller can call <code>promise()</code> to
 * ask for a {@link Promise} to which a {@link DoneCallback}, {@link FailCallback} or {@link Filter} can be 
 * attached.
 * 
 * @author jonseymour
 *
 * @param <P> The promised type.
 * @param <F> The failure type.
 */
public interface Deferred<P,F> {
	/**
	 * Resolve the promise associated with the receiver.
	 * @param p The value of the promise.
	 * @return The receiver.
	 * @throws IllegalStateException If the promise has already been resolved or rejected.
	 */
	public Deferred<P,F> resolve(P p);

	/**
	 * Reject the promise associated with the receiver.
	 * @param f The value of the rejection.
	 * @return The receiver.
	 * @throws IllegalStateException If the promise has already been resolved or rejected.
	 */
	public Deferred<P,F> reject(F f);
	
	/**
	 * @return The {@link Promise} associated with the receiver. Callbacks and
	 * filters can be attached to the {@link Promise} which will receive the promised
	 * result or failure.
	 */
	public Promise<P,F> promise();
}
