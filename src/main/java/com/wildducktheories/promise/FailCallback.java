package com.wildducktheories.promise;

/**
 * A callback that is used to receive the value of a rejected {@link Promise}.
 * @author jonseymour
 *
 * @param <F> The failure type.
 * @see Promise#fail(FailCallback)
 */
public interface FailCallback<F> {
	/**
	 * Called to receive the failure associated with a rejected promise.
	 * @param f The rejection value.
	 */
	void onFail(F f);
}
