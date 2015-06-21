	package com.wildducktheories.promise.impl;
	
	import com.wildducktheories.promise.Call;
	import com.wildducktheories.promise.Deferred;
	import com.wildducktheories.promise.Promise;
	import com.wildducktheories.promise.PromiseAPI;
	import com.wildducktheories.tasklet.Directive;
	import com.wildducktheories.tasklet.Scheduler;
	import com.wildducktheories.tasklet.SchedulerAPI;
	import com.wildducktheories.tasklet.Tasklet;
	
	/**
	 * An implementation of Call which breaks the call process into 3 normal phases:
	 * <ul>
	 * <li>build
	 * <li>send
	 * <li>receive
	 * </ul>
	 * <p>and one abnormal phase:
	 * <ul>
	 * <li>handle
	 * </ul>
	 * <p>
	 * The <b>build</b> phase executes in the current thread and is responsible for transforming
	 * state accessible to the current thread into a call request.
	 * <p>
	 * The <b>send</b> phase executes in a thread which is possibly asynchrononous to the current thread
	 * (according to SchedulerAPI.getScheduler().isAsync()) and returns a response from that call
	 * or throws an {@link Exception}.
	 * <p>
	 * The <b>receive</b> phase executes in the current {@link Scheduler}'s main thread and 
	 * is responsible for integrating the response of type S into the state
	 * of the {@link Scheduler}'s main thread and/or transforming the
	 * result into the promised type, P.
	 * <p>
	 * The <b>handle</b> phase executes if either of the <b>build</b>, <b>send</b> or <b>receive</b> phase throws an Exception.
	 * It's responsibility is to translate the caught exception to an instance of the {@link Promise}'s failure type.
	 * 
	 * @author jonseymour
	 *
	 * @param <P> The promised type.
	 * @param <F> The failure type.
	 * @param <R> The call request type.
	 * @param <S> The call response type.
	 */
	public abstract class AbstractCallImpl<P,F,R,S> implements Call<P, F>
	{
		/* (non-Javadoc)
		 * @see com.wildducktheories.promise.rpc.impl.RPC#call()
		 */
		@Override
		public final Promise<P, F> call() {
			final Deferred<P, F> deferred = PromiseAPI.get().deferred();
			
			try {
				final R r = build();
				SchedulerAPI.get().getScheduler().schedule(new Tasklet() {
					private boolean sent = false;
					private S s;
					private Exception e;
					@Override
					public Directive task() {
						try {
							if (!sent) {
								sent = true;
								s = send(r);
								return Directive.SYNC;
							} else {
								if (e == null) {
									final P p = receive(s);
									deferred.resolve(p);
								} else {
									deferred.reject(handle(e));
								}
								return Directive.DONE;
							}
						} catch (Exception e) {
							this.e = e;
							return Directive.SYNC;
						}
					}			
				}, Directive.ASYNC);
				return deferred.promise();
			} catch (Exception e) {
				return deferred.reject(handle(e)).promise();
			}
		}
	
		/**
		 * Build a request of type R in the caller's thread.
		 * @return A request of type R.
		 * @throws Exception
		 */
		protected abstract R build() throws Exception;
		
		/**
		 * Send an Call request of type R in a possibly asynchronous thread 
		 * and return a result of type S.
		 * @param r The request type.
		 * @return The remote result type.
		 * @throws Exception
		 */
		protected abstract S send(R r) throws Exception;
		
		/**
		 * Receive a result of type S and return a result of the promised type, P.
		 * @param s The Call response type.
		 * @return A result of type P.
		 * @throws Exception
		 */
		protected abstract P receive(S s) throws Exception;
		
		/**
		 * Transform an exception into a failure of type F.
		 * @param e An exception.
		 * @return A failure of type F.
		 */
		protected abstract F handle(Exception e);
		
	}
