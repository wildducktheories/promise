package com.wildducktheories.promise.impl;

import java.util.concurrent.Callable;

import com.wildducktheories.promise.API;
import com.wildducktheories.promise.Deferred;
import com.wildducktheories.promise.Promise;
import com.wildducktheories.tasklet.Directive;
import com.wildducktheories.tasklet.SchedulerAPI;
import com.wildducktheories.tasklet.Tasklet;

/**
 * An implementation of the promise {@link API} interface.
 * @author jonseymour
 */
public final class APIImpl implements API {
	
	/* (non-Javadoc)
	 * @see com.wildducktheories.promise.impl.API#resolved(P, java.lang.Class, java.lang.Class)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public final <P,F> Promise<P, F> resolved(P resolution, Class<P> promiseType, Class<F> failureType) {
		return ((Deferred<P,F>)deferred()).resolve(resolution).promise();
	}
	
	/* (non-Javadoc)
	 * @see com.wildducktheories.promise.impl.API#rejected(F, java.lang.Class, java.lang.Class)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public final <P,F> Promise<P, F> rejected(F rejection, Class<P> promiseType, Class<F> failureType) {
		return ((Deferred<P,F>)deferred()).reject(rejection).promise();
	}
	
	/* (non-Javadoc)
	 * @see com.wildducktheories.promise.impl.API#deferred()
	 */
	@Override
	public <P, F> Deferred<P, F> deferred() {
		return new DeferredImpl<P,F>();
	}
	
	/* (non-Javadoc)
	 * @see com.wildducktheories.promise.impl.API#when(java.util.concurrent.Callable)
	 */
	@Override
	public <P> Promise<P, Exception> when(final Callable<P> callable) {
		final Deferred<P, Exception> deferred = deferred();
		SchedulerAPI.get().getScheduler().schedule(new Tasklet() {
			@Override
			public Directive task() {
				try {
					deferred.resolve(callable.call());
				} catch (Exception e) {
					deferred.reject(e);
				}
				return Directive.DONE;
			}
		}, Directive.ASYNC);
		return deferred.promise();
	}

	/* (non-Javadoc)
	 * @see com.wildducktheories.promise.impl.API#when(java.lang.Runnable)
	 */
	@Override
	public Promise<Void, RuntimeException> when(final Runnable runnable) {
		final Deferred<Void, RuntimeException> deferred = deferred();
		SchedulerAPI.get().getScheduler().schedule(new Tasklet() {
			@Override
			public Directive task() {
				try {
					runnable.run();
					deferred.resolve(null);
				} catch (RuntimeException e) {
					deferred.reject(e);
				}
				return Directive.DONE;
			}
		}, Directive.ASYNC);
		return deferred.promise();
	}
	
	/* (non-Javadoc)
	 * @see com.wildducktheories.promise.impl.API#reset()
	 */
	@Override
	public void reset() {
		SchedulerAPI.reset();
	}
	
	/* (non-Javadoc)
	 * @see com.wildducktheories.promise.impl.API#call(java.util.concurrent.Callable)
	 */
	@Override
	@SuppressWarnings("unchecked")
	public <P> P call(final Callable<P> callable) 
		throws Exception 
	{
		final Object[] results = new Object[] {null};
		final Exception[] failures = new Exception[] {null};
		SchedulerAPI.get()
			.newScheduler()
			.schedule(new Tasklet() {
				@Override
				public Directive task() {
					try {
						results[0]=callable.call();
					} catch (Exception e) {
						failures[0] = e;
					}
					return Directive.DONE;
				}
			}, Directive.SYNC)
			.run();
		if (failures[0] == null) {
			return (P)results[0];
		} else {
			throw failures[0];
		}
	}

	/* (non-Javadoc)
	 * @see com.wildducktheories.promise.impl.API#run(java.lang.Runnable)
	 */
	@Override
	public void run(final Runnable runnable)
	{
		final RuntimeException[] failures = new RuntimeException[] {null};
		SchedulerAPI.get()
			.newScheduler()
			.schedule(new Tasklet() {
				@Override
				public Directive task() {
					try {
						runnable.run();
					} catch (RuntimeException e) {
						failures[0] = e;
					}
					return Directive.DONE;
				}
			}, Directive.SYNC)
			.run();
		if (failures[0] == null) {
			return;
		} else {
			throw failures[0];
		}
	}

}
