package com.wildducktheories.promise;

/**
 * A filter used to transform a value of type P into a value of type Q.
 * 
 * @author jonseymour
 *
 * @param <P>
 *            The type to be transformed.
 * @param <Q>
 *            The transformed type.
 * @see Promise#then(Filter).
 */
public interface Filter<P, Q> {
	/**
	 * @param p
	 *            A value of type P.
	 * @return A transformed value of type Q.
	 */
	Q filter(P p);
}
