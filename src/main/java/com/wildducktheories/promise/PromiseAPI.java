package com.wildducktheories.promise;

import java.util.concurrent.Callable;

import com.wildducktheories.api.APIManager;
import com.wildducktheories.api.impl.AbstractAPIManagerImpl;
import com.wildducktheories.promise.impl.APIImpl;

/**
 * Provides a suite of static methods for obtaining an instance of the promise {@link API} (i.e. <code>get()</code>),
 * temporarily altering the current instance (i.e. <code>with()</code>), creating a new instance (i.e. <code>newAPI()</code>)
 * and releasing a ThreadLocal reference to an API that may have been created by a naked call to <code>get()</code> 
 * (i.e. <code>reset()<code>).
 */
public final class PromiseAPI {

	/**
	 * Keeps track of API instances on a per Thread basis.
	 */
	private static final APIManager<API> manager = new AbstractAPIManagerImpl<API>() {
		public API create() {
			return new APIImpl();
		}
	};

	private PromiseAPI() {
	}

	/**
	 * Answer the current {@link API} instance.
	 * <p>
	 * If this call is ever executed outside the scope of an enclosing <code>with()</code> call then the
	 * application MUST arrange to call <code>reset()</code> before losing a reference to the current {@link Thread}.
	 * Failure to do so may result in the {@link API}'s class loader being pinned by a Thread.
	 * @return The current instance. 
	 */
	public static API get() {
		return manager.get();
	}

	/**
	 * Release any {@link ThreadLocal} resources that may have been acquired by an unenclosed (or "naked") call
	 * to get().
	 * <p>
	 * If the application cannot guarantee the absence of unenclosed calls to get() then it MUST arrange
	 * to call this method before releasing control of a Thread.
	 */
	public static void reset() {
		manager.reset();
	}

	/**
	 * Answer a {@link Callable} that will set the current Thread's instance of {@link API} to api, then execute callable, then restore
	 * the original API reference.
	 * @param api An alternative API implementation.
	 * @param callable The Callable to execute.
	 * @return A new {@link Callable} that will invoke the specified {@link Callable} after arranging for
	 * get() to return api for the duration of execution of the specified {@link Callable}.
	 * @throws Exception Thrown by {@link Callable#call()}
	 */
	public static <P> Callable<P> with(API api, Callable<P> callable) throws Exception {
		return manager.with(api, callable);
	}

	/**
	 * Answer a {@link Runnable}' that will set the current Thread's instance of {@link API} to api, then execute runnable, then restore
	 * the original API reference.
	 * @param api An alternative API implementation.
	 * @param runnable The Runnable to execute.
	 * @return A new {@link Runnable} that will invoke the specified {@link Runnable} after arranging for
	 * get() to return api for the duration of execution of the specified {@link Runnable}.
	 */
	public static Runnable with(API api, Runnable runnable) {
		return manager.with(api, runnable);
	}
}