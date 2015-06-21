package com.wildducktheories.promise;

/**
 * A composition of the DoneCallback and FailCallback interfaces.
 * 
 * @author jonseymour
 * @param <P> The promised value.
 * @param <F> The future value.
 */
public interface CompletionCallback<P,F> 
	extends DoneCallback<P>, FailCallback<F>
{
}
