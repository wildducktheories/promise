package com.wildducktheories.promise;

/**
 * An abstraction of a possibly asynchronous call that delivers its
 * result or failure via a {@link Promise}.
 * @author jonseymour
 *
 * @param <P> The type of the promised result.
 * @param <F> The failure type.
 */
public interface Call<P, F> {

	/**
	 * Return a promise for a call that may execute asynchronously.
	 * @return A promise that will deliver the call result or failure.
	 */
	public abstract Promise<P, F> call();

}