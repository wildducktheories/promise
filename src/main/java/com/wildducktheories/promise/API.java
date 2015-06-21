package com.wildducktheories.promise;

import java.util.concurrent.Callable;

import com.wildducktheories.tasklet.Scheduler;
import com.wildducktheories.tasklet.SchedulerNotRunningException;

/**
 * The (promise) API interface provides methods for creating {@link Deferred}
 * results, and resolved or rejected {@link Promise} instances. See:
 * <code>deferred()</code>, <code>resolved()</code>, <code>rejected()</code>.
 * <p>
 * It also provides <code>when()</code> methods for launching asynchronous
 * {@link Callable} and {@link Runnable} computations in exchange for
 * {@link Promise} instances that will later be delivered when the asynchronous
 * computations complete.
 * <p>
 * Finally, it provides two methods (<code>call()</code> and <code>run()</code>)
 * that can be used to establish the necessary execution context required by the
 * <code>when()</code> methods for use in cases where this necessary context
 * cannot otherwise be guaranteed.
 * <p>
 * All promises are delivered by code executing on the synchronous thread of 
 * the current {@link Scheduler} instance of the current {@link Thread} as determined by
 * {@link com.wildducktheories.tasklet.API#getScheduler()} at the point the {@link Promise}
 * callbacks &amp;/or filters are registered with the {@link Promise} instance.
 * 
 * @author jonseymour
 */
public interface API extends com.wildducktheories.api.API {

	/**
	 * Returns a resolved promise for a value of the specified type.
	 * @param resolution The resolution of the promise.
	 * @param promiseType The promise type.
	 * @param failureType The failure type.
	 * @return A resolved {@link Promise} with the specified resolution.
	 */
	public abstract <P, F> Promise<P, F> resolved(P resolution,
			Class<P> promiseType, Class<F> failureType);

	/**
	 * Returns a rejected promise for a value of the specified type.
	 * @param rejection The rejection of the promise.
	 * @param promiseType The promise type.
	 * @param failureType The failure type.
	 * @return A rejected {@link Promise} with the specified rejection.
	 */
	public abstract <P, F> Promise<P, F> rejected(F rejection,
			Class<P> promiseType, Class<F> failureType);

	/**
	 * Return an completable {@link Deferred} instance.
	 * @return A completable {@link Deferred} instance.
	 */
	public abstract <P, F> Deferred<P, F> deferred();

	/**
	 * Schedules the specified {@link Callable} asynchronously with respect to the synchronous thread of the 
	 * current thread's current {@link Scheduler} instance.
	 * <p>
	 * The returned {@link Promise} will be delivered on the current {@link Scheduler}'s synchronous 
	 * thread irrespective of which thread actually resolves or rejects the {@link Promise}.
	 * <p>
	 * <h2>Required Execution Context</h2>
	 * This call will fail with an unchecked {@link SchedulerNotRunningException} unless there is an active call 
	 * to {@link Scheduler#run()} for the current thread's scheduler instance on at least one thread 
	 * (not necessarily the current thread). 
	 * <p>
	 * If the caller of {@link API#when(Callable)} cannot otherwise guarantee that this condition is 
	 * satisfied, then the required condition can be established by encapsulating the call-site in an enclosing
	 * {@link #call(Callable)} which is then invoked with {@link API#call(Callable)} as in the example below:
	 * <p>
	 * <pre>
	 * PromiseAPI
	 *   .get()
	 *   .call(
	 *     new Callable&lt;P&gt;() {
	 *       public P call() {
	 *         PromiseAPI
	 *         .get()
	 *         .when(new Callable&lt;P&gt;() {
	 *           public P call() {
	 *           // body of asynchronous code
	 *           }
	 *          }) // encapsulated call-site
	 *         .done(new DoneCallback&lt;P&gt;) {
	 *            public void onDone(P p) {
	 *            // code to be executed on successful completion
	 *           }
	 *      });
	 *   }
	 * }
	 * </pre>
	 * <p>
	 * In the example above, the {@link API#call(Callable)} call will not return until all {@link Promise} instances
	 * created by the body of the outermost {@link Callable} (and code it calls) 
	 * have been completely resolved or rejected.
	 * @param callable A {@link Callable} to be executed on a thread which executes asynchronously to the
	 * the current {@link Scheduler}'s synchronous thread.
	 * @return A {@link Promise} which is delivered on the current {@link Scheduler}'s synchronous thread
	 * when the specified {@link Callable} finishes.
	 * @throws com.wildducktheories.tasklet.SchedulerNotRunningException This exception is thrown if the current {@link Scheduler}
	 * of the current {@link Thread} is not running. Read the notes about <strong>Required Execution Context</strong> (above)
	 * for advice about how to avoid this exception.
	 */
	public abstract <P> Promise<P, Exception> when(Callable<P> callable);

	/**
	 * Schedules the specified {@link Runnable} asynchronously with respect to the synchronous thread of the 
	 * current thread's current {@link Scheduler} instance.
	 * <p>
	 * The returned {@link Promise} will be delivered on the current {@link Scheduler}'s synchronous 
	 * thread irrespective of which thread actually resolves or rejects the {@link Promise}.
	 * <p>
	 * <h2>Required Execution Context</h2>
	 * This call will fail with an unchecked {@link SchedulerNotRunningException} unless there is an active call 
	 * to {@link Scheduler#run()} for the current thread's scheduler instance on at least one thread 
	 * (not necessarily the current thread). 
	 * <p>
	 * If the caller of {@link API#when(Runnable)} cannot otherwise guarantee that this condition is 
	 * satisfied, then the required condition can be established by encapsulating the call-site in an enclosing
	 * {@link #run(Runnable)} which is then invoked with {@link API#run(Runnable)} as in the example below:
	 * <p>
	 * <pre>
	 * PromiseAPI.get().run(new Runnable() {
	 *   public void run() {
	 *     PromiseAPI
	 *       .get()
	 *       .when(new Runnable() {
	 *         public void run() {
	 *           // body of asynchronous code
	 *         }
	 *      }) // encapsulated call-site
	 *      .done(new DoneCallback<Void>) {
	 *         public void onDone(Void p) {
	 *          // code to be executed on successful completion
	 *         }
	 *      });
	 *   }
	 * }
	 * </pre>
	 * <p>
	 * In the example above, the {@link API#run(Runnable)} call will not return until all {@link Promise} instances
	 * created by the body of the outermost {@link Runnable} (and code it calls) 
	 * have been completely resolved or rejected.
	 * @param runnable A {@link Runnable} A runnable to be executed on a thread which will execute asynchronously to the
	 * the current {@link Scheduler}'s synchronous thread.
	 * @return A {@link Promise} which is delivered on the current {@link Scheduler}'s synchronous thread
	 * when the specified {@link Runnable} finishes.
	 * @throws com.wildducktheories.tasklet.SchedulerNotRunningException This exception is thrown if the current {@link Scheduler}
	 * of the current {@link Thread} is not running. Read the notes about <strong>Required Execution Context</strong> (above)
	 * for advice about how to avoid this exception.
	 */
	public abstract Promise<Void, RuntimeException> when(Runnable runnable);

	/**
	 * Releases thread local resources that MAY be allocated by use of {@link PromiseAPI} methods outside
	 * of an active {@link API#call(Callable)} and {@link API#run(Runnable)} call.
	 */
	public abstract void reset();

	/**
	 * Calls the specified {@link Callable} on the current thread and will then wait until all {@link Promise} instances 
	 * created during execution of the {@link Callable} have completed. 
	 * <p>
	 * All {@link Promise} instances created as the result of executing the specified {@link Callable} 
	 * will be delivered on the current {@link Thread}, irrespective of which {@link Thread}
	 * resolves or rejects them. This property minimises the amount of locking that
	 * needs to be performed on resources managed exclusively by the current thread.
	 * <p>
	 * A call to this method may be required to establish an execution context required by the
	 * {@link API#when(Callable)} method.
	 * <p>
	 * Failure to establish such a context prior to calling {@link API#when(Callable)} will cause that 
	 * method to throw an unchecked {@link SchedulerNotRunningException}.
	 * <p>
	 * @param callable The {@link Callable} to be executed on the current thread.
	 * @throws Exception Any exception thrown by the specified {@link Callable}.
	 */
	public abstract <P> P call(Callable<P> callable) throws Exception;

	/**
	 * Runs the specified {@link Runnable} on the current thread and will then wait until all {@link Promise} instances 
	 * created during execution of the {@link Runnable} have completed. 
	 * <p>
	 * All {@link Promise} instances created as the result of executing the specified {@link Runnable} 
	 * will be delivered on the current {@link Thread}, irrespective of which {@link Thread}
	 * resolves or rejects them. This property minimises the amount of locking that
	 * needs to be performed on resources managed exclusively by the current thread.
	 * <p>
	 * A call to this method may be required to establish an execution context required by the
	 * {@link API#when(Runnable)} method.
	 * <p>
	 * Failure to establish such a context prior to calling {@link API#when(Runnable)} will cause that 
	 * method to throw an unchecked {@link SchedulerNotRunningException}.
	 * <p>
	 * @param runnable The {@link Runnable} to be executed on the current thread.
	 * @throws Exception Any exception thrown by the specified {@link Callable}.
	 */
	public abstract void run(Runnable runnable);

}