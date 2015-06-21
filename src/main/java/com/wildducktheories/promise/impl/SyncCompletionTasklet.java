package com.wildducktheories.promise.impl;

import com.wildducktheories.promise.DoneCallback;
import com.wildducktheories.promise.FailCallback;
import com.wildducktheories.tasklet.Directive;
import com.wildducktheories.tasklet.Rescheduler;
import com.wildducktheories.tasklet.Scheduler;
import com.wildducktheories.tasklet.Tasklet;

/**
 * A tasklet that ensures that the promise result is always delivered on the
 * scheduler's main thread rather than the resolver's thread.
 *  
 * @param <P> The promised type.
 * @param <F> The failure type.
 */
final class SyncCompletionTasklet<P, F>
	implements Tasklet, DoneCallback<P>, FailCallback<F>
{
	private final Rescheduler rescheduler;
	private final DoneCallback<P> doneCallback;
	private final FailCallback<F> failCallback;
	
	public SyncCompletionTasklet(Scheduler scheduler,
			DoneCallback<P> doneCallback, FailCallback<F> failCallback) {
		super();
		this.rescheduler = scheduler.suspend(this);
		this.doneCallback = doneCallback;
		this.failCallback = failCallback;
	}

	private P result;
	private F failure;
	private boolean isFailure = false;
	
	public void onDone(P p) {
		isFailure = false;
		result = p;
		rescheduler.resume(Directive.SYNC);
	}
	
	public void onFail(F f) {
		isFailure = true;
		failure = f;
		rescheduler.resume(Directive.SYNC);
	}
	
	public Directive task() {
		if (isFailure) {
			if (failCallback != null) {
				failCallback.onFail(failure);
			}
		} else {
			if (doneCallback != null) {
				doneCallback.onDone(result);
			}
		}
		return Directive.DONE;
	}

}