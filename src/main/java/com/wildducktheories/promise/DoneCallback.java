package com.wildducktheories.promise;

/**
 * A callback that is used to receive the value of a resolved {@link Promise}.
 * @author jonseymour
 *
 * @param <P> The promise type.
 * @see Promise#done(DoneCallback)
 */
public interface DoneCallback<P> {
	/**
	 * Called to communicate the value of a resolved {@link Promise}.
	 * @param p The resolved {@link Promise} value.
	 */
	public void onDone(P p);
}
