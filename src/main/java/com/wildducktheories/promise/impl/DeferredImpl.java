package com.wildducktheories.promise.impl;

import java.util.LinkedList;
import java.util.List;

import com.wildducktheories.promise.CompletionCallback;
import com.wildducktheories.promise.Deferred;
import com.wildducktheories.promise.DoneCallback;
import com.wildducktheories.promise.FailCallback;
import com.wildducktheories.promise.Filter;
import com.wildducktheories.promise.Promise;
import com.wildducktheories.promise.PromiseAPI;
import com.wildducktheories.tasklet.Directive;
import com.wildducktheories.tasklet.Scheduler;
import com.wildducktheories.tasklet.SchedulerAPI;
import com.wildducktheories.tasklet.Tasklet;

/**
 * An implementation of the {@link Promise} and {@link Deferred} interfaces for a single promise.
 * 
 * @author jonseymour
 *
 * @param <P> The promised type.
 * @param <F> The failure type.
 */
public class DeferredImpl<P, F> implements Deferred<P, F>, Promise<P,F> {
	
	private enum State {
		PENDING,
		DONE,
		FAILED
	}
	
	private final List<DoneCallback<P>> doneCallbacks = new LinkedList<DoneCallback<P>>();
	private final List<FailCallback<F>> failCallbacks = new LinkedList<FailCallback<F>>();
	
	private State state = State.PENDING;
	private P resolution = null;
	private F rejection = null;	
	
	@Override
	public Deferred<P, F> resolve(P p) {
		synchronized (this) {
			if (state != State.PENDING) {
				throw new IllegalStateException("illegal state: promise already resolved or rejected");
			}
			state = State.DONE;
			resolution = p;
		}
		notifyAllDone(p);
		return this;
	}

	@Override
	public Deferred<P, F>  reject(F f) {
		synchronized (this) {
			if (state != State.PENDING) {
				throw new IllegalStateException("illegal state: promise already resolved or rejected");
			}
			state = State.FAILED;
			rejection = f;
		}
		notifyAllFail(f);
		return this;
	}

	@Override
	public Promise<P, F> promise() {
		return this;
	}
	
	@Override
	public <Q> Promise<Q, F> then(final Filter<P, Q> filter) {
		final boolean done;
		
		synchronized (this) {
			switch (state) {
			case PENDING:
				final Deferred<Q,F> filtered = PromiseAPI.get().deferred();			
				final CompletionCallback<P, F> complete = new CompletionCallback<P, F>() {
					@Override
					public void onDone(P p) {
						filtered.resolve(filter.filter(p));
					}
					
					public void onFail(F f) {
						filtered.reject(f);
					};
				};
				addCallbacks(complete, complete);
				return filtered.promise();
			case DONE:
				done = true;
				break;
			case FAILED:
				done = false;
				break;
			default:
				throw new IllegalStateException("illegal state: unhandled state: "+state);
			}
		}
		
		if (done) {
			final Deferred<Q,F> deferred = PromiseAPI.get().deferred();
			SchedulerAPI.get().getScheduler().schedule(new Tasklet() {
				@Override
				public Directive task() {
					deferred.resolve(filter.filter(resolution));
					return Directive.DONE;
				}}, Directive.SYNC);
			
			return deferred.promise();
		} else {
			return (Promise<Q,F>)(PromiseAPI.get().rejected(rejection, (Class<Q>)null, (Class<F>)null));			
		}		
	}
	
	

	@Override
	public <Q> Promise<Q, F> thenPromise(final Filter<P, Promise<Q, F>> filter) {
		final Deferred<Q,F> deferred = PromiseAPI.get().deferred();
		boolean done = false;
		synchronized (this) {
			switch(state) {
			case PENDING:
				final CompletionCallback<P, F> complete = new CompletionCallback<P, F>() {
					@Override
					public void onDone(P p) {
						filter
							.filter(p)
							.done(new DoneCallback<Q>(){

								@Override
								public void onDone(Q q) {
									deferred.resolve(q);
								}
							})
							.fail(new FailCallback<F>() {

								@Override
								public void onFail(F f) {
									deferred.reject(f);
								}
							});
					}
					
					public void onFail(F f) {
						deferred.reject(f);
					};
				};
				addCallbacks(complete, complete);
				return deferred.promise();
			case DONE:
				done = true;
				break;
			case FAILED:
				break;
			default:
				throw new IllegalStateException("illegal state: "+state);
			}
		}
		if (done) {
			SchedulerAPI.get().getScheduler().schedule(new Tasklet() {
				@Override
				public Directive task() {
					filter.filter(resolution).done(new DoneCallback<Q>(){

						@Override
						public void onDone(Q q) {
							deferred.resolve(q);
						}
					})
					.fail(new FailCallback<F>() {

						@Override
						public void onFail(F f) {
							deferred.reject(f);
						}
					});
					return Directive.DONE;
				}}, Directive.SYNC);	
			return deferred.promise();
		}
		return (Promise<Q,F>)(PromiseAPI.get().rejected(rejection, (Class<Q>)null, (Class<F>)null));			
	}

	@Override
	public Promise<P, F> done(final DoneCallback<P> callback) {
		synchronized (this) {
			switch (state) {
			case PENDING:
				final DoneCallback<P> cb = callback;
				addCallbacks(cb, null);
				break;
			case DONE:
				notifyDone(new SyncCompletionTasklet<>(SchedulerAPI.get().getScheduler(), callback, null), resolution);
			default:
			}
		}
		return this;
	}

	@Override
	public Promise<P, F> fail(final FailCallback<F> callback) {
		synchronized(this) {
			switch (state) {
			case PENDING:
				addCallbacks(null, callback);
				break;
			case FAILED:
				notifyFail(new SyncCompletionTasklet<>(SchedulerAPI.get().getScheduler(), null, callback), rejection);
				break;
			default:
			}
		}
		return this;
	}

	@Override
	public Promise<P, F> complete(CompletionCallback<P, F> callback) {
		
		synchronized (this) {
			switch (state) {
			case PENDING:
				final DoneCallback<P> cb = callback;
				addCallbacks(cb, null);
				addCallbacks(null, callback);
				break;
			case DONE:
				notifyDone(callback, resolution);
				break;
			case FAILED:
				notifyFail(callback, rejection);
				break;
			default:
			}
		}
		
		return this;
	}
	
	/**
	 * Add callbacks to the receiver. If the current thread is running with a scheduler, then make sure that 
	 * the promise delivery occurs with the same concurrency w.r.t. the scheduler as the registration and also
	 * that the scheduler is aware that there is a promise pending delivery.
	 * 
	 * @param cbp
	 * @param cbf
	 */
	private void addCallbacks(final DoneCallback<P> cbp, final FailCallback<F> cbf) {
		final Scheduler scheduler = SchedulerAPI.get().getScheduler();
		final SyncCompletionTasklet<P, F> tasklet = 
			new SyncCompletionTasklet<P, F>(scheduler, cbp, cbf);
		
		// Dequeue the tasklet to tell the scheduler that there is a pending resolution event.
		
		scheduler.schedule(tasklet, Directive.WAIT);
		doneCallbacks.add(tasklet);
		failCallbacks.add(tasklet);
	}
	
	private void notifyAllDone(P p) {
		for (DoneCallback<P> cb : doneCallbacks) {
			notifyDone(cb, p);
		}
		doneCallbacks.clear();
		failCallbacks.clear();
	}

	private void notifyAllFail(F f) {
		for (FailCallback<F> cb : failCallbacks) {
			notifyFail(cb, f);
		}
		doneCallbacks.clear();
		failCallbacks.clear();
	}

	private void notifyDone(final DoneCallback<P> cb, final P p) {
		try {
			cb.onDone(p);
		} catch (RuntimeException r) {
			handleRuntimeException(r);
		}			
	}
	
	private void notifyFail(final FailCallback<F> cb, final F f) {
		try {
			cb.onFail(f);
		} catch (RuntimeException r) {
			handleRuntimeException(r);
		}
	}
	
	protected void handleRuntimeException(RuntimeException r) {
		r.printStackTrace(System.err);
	}
}
