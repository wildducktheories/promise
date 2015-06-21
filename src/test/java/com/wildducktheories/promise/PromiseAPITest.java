package com.wildducktheories.promise;

import java.util.concurrent.Callable;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

import com.wildducktheories.tasklet.Scheduler;
import com.wildducktheories.tasklet.SchedulerAPI;
import com.wildducktheories.tasklet.SchedulerNotRunningException;

public class PromiseAPITest {
	
	@After
	public void tearDown() {
		PromiseAPI.get().reset();
	}
	/**
	 * Test that a pre-resolved promise is delivered to the DoneCallback and to the CompletionCallback
	 * @throws InterruptedException 
	 */
	@Test
	public void testResolved() throws InterruptedException {
		final Object solution = new Object();
		final Object failure = new Object();
		final Object[] results = new Object[]{null, null};
		final Object[] failures = new Object[]{failure, failure};
		final Thread[] threads = new Thread[] { null };
		
		final Promise<Object, Object> promise = PromiseAPI.get().resolved(solution, Object.class, Object.class);

		promise.done(new DoneCallback<Object>() {
				
			@Override
			public void onDone(Object p) {
				results[0] = p;
				threads[0] = Thread.currentThread();
			}
		}).fail(new FailCallback<Object>() {
				
			@Override
			public void onFail(Object f) {
				failures[0] = f;
			}
		}).complete(new CompletionCallback<Object, Object>() {

			@Override
			public void onDone(Object p) {
				results[1] = p;
			}

			@Override
			public void onFail(Object f) {
				failures[1] = f;
			}
			
		});		
		Assert.assertSame("done called", solution, results[0]);
		Assert.assertSame("fail not called", failure, failures[0]);		
		Assert.assertSame("done called", solution, results[1]);
		Assert.assertSame("fail not called", failure, failures[1]);	
		Assert.assertSame("thread is same", Thread.currentThread(), threads[0]);
	}

	/**
	 * Test that a pre-rejected promise is delivered to the FailCallback and to the CompletionCallback
	 */
	@Test
	public void testRejected() {
		final Object solution = new Object();
		final Object failure = new Object();
		final Object[] results = new Object[]{solution, solution};
		final Object[] failures = new Object[]{null, null};
		final Thread[] threads = new Thread[] { null };

		final Promise<Object, Object> promise = PromiseAPI.get().rejected(failure, Object.class, Object.class);
		promise
			.done(new DoneCallback<Object>() {
			
				@Override
				public void onDone(Object p) {
					results[0] = p;
				}
				})
			.fail(new FailCallback<Object>() {
			
				@Override
				public void onFail(Object f) {
					threads[0] = Thread.currentThread();
					failures[0] = f;
				}
				})
			.complete(new CompletionCallback<Object, Object>() {
	
				@Override
				public void onDone(Object p) {
					results[1] = p;
				}
	
				@Override
				public void onFail(Object f) {
					failures[1] = f;
				}
		});
		Assert.assertSame("done not called", solution, results[0]);
		Assert.assertSame("fail called", failure, failures[0]);		
		Assert.assertSame("done not called", solution, results[1]);
		Assert.assertSame("fail called", failure, failures[1]);		
		Assert.assertSame("thread is same", Thread.currentThread(), threads[0]);
	}
	
	/**
	 * Tests when(Runnable) throws a SchedulerNotRunningException when called in an unguarded fashion.
	 */
	@Test
	public void testUnguardedWhenRunnable() {
		final boolean[] flags = new boolean[] { false };
		try {
			PromiseAPI.get().when(new Runnable() {
				public void run() {
					flags[0] = true;
				}
			});
			Assert.fail("expected exception");
		} catch (SchedulerNotRunningException e) {
		}
		Assert.assertFalse("runnable did not run", flags[0]);		
	}

	/**
	 * Tests when(Callable) throws a SchedulerNotRunningException when called in an unguarded fashion.
	 */
	@Test
	public void testUnguardedWhenCallable() {
		final boolean[] flags = new boolean[] { false };
		try {
			PromiseAPI.get().when(new Callable<Void>() {
				public Void call() {
					flags[0] = true;
					return null;
				}
			});
			Assert.fail("expected exception");
		} catch (SchedulerNotRunningException e) {
		}
		Assert.assertFalse("runnable did not run", flags[0]);		
	}
	
	@Test
	public void testGuardedWhenRunnable() throws InterruptedException {
		final boolean[] flags = new boolean[] { false };
		final Thread[] threads = new Thread[] { null, null };
		final API api = PromiseAPI.get();
		
		api.run(new Runnable() {
				@Override
				public void run() {
					api.when(new Runnable() {
							public void run() {
								flags[0] = true;
								threads[0] = Thread.currentThread();
							}
						})
						.done(new DoneCallback<Void>() {
							@Override
							public void onDone(Void p) {
								threads[1] = Thread.currentThread();
							}
						});
				}} );
		Assert.assertTrue("runnable did run", flags[0]);		
		Assert.assertNotSame("Runs on different thread", Thread.currentThread(), threads[0]);
		Assert.assertSame("Runs on same thread", Thread.currentThread(), threads[1]);
	}
	
	@Test
	public void testGuardedWhenRunnableWithNoDone() throws InterruptedException {
		final boolean[] flags = new boolean[] { false };
		final Thread[] threads = new Thread[] { null };
		final API api = PromiseAPI.get();
		api.run(new Runnable() {
			@Override
			public void run() {
				api.when(new Runnable() {
						public void run() {
							flags[0] = true;
							threads[0] = Thread.currentThread();
						}
					});
			}});
		Assert.assertTrue("runnable did run", flags[0]);		
		Assert.assertNotSame("Runs on different thread", Thread.currentThread(), threads[0]);
	}
	
	@Test
	public void testGuardedWhenRunnableThrowsException() throws InterruptedException {
		final boolean[] flags = new boolean[] { false };
		final Thread[] threads = new Thread[] { null, null };
		final Exception[] exceptions = new Exception[] { null };
		final API api = PromiseAPI.get();
		api.run(new Runnable() {
			@Override
			public void run() {
				api.when(new Runnable() {
						public void run() {
							threads[0] = Thread.currentThread();
							throw new RuntimeException("failed");
						}
					})
					.done(
						new DoneCallback<Void>() {
							
							@Override
							public void onDone(Void p) {
								flags[0] = true;
							}
						}
					)
					.fail(new FailCallback<RuntimeException>() {
						
						@Override
						public void onFail(RuntimeException f) {
							exceptions[0] = f;
							threads[1] = Thread.currentThread();							
						}
					});
			}});
		Assert.assertFalse("done callback did not run", flags[0]);
		Assert.assertNotNull("fail receives exception", exceptions[0]);
		Assert.assertNotSame("when runs on different thread", Thread.currentThread(), threads[0]);
		Assert.assertSame("fail runs on current thread", Thread.currentThread(), threads[1]);
	}
	
	@Test
	public void testSchedulerAssignments() {
		final Scheduler[] schedulers = new Scheduler[] { null, null, null, null, null };
		schedulers[0] = SchedulerAPI.get().getScheduler();
		final API api = PromiseAPI.get();
		api.run(new Runnable() {
			@Override
			public void run() {
				schedulers[1] = SchedulerAPI.get().getScheduler();
				api
					.when(new Runnable() {
						public void run() {
							schedulers[2] = SchedulerAPI.get().getScheduler();
						}
					}).done(
						new DoneCallback<Void>() {
							
							@Override
							public void onDone(Void p) {
								schedulers[3] = SchedulerAPI.get().getScheduler();
							}
						}
					);
			}});
		schedulers[4] =  SchedulerAPI.get().getScheduler();
		SchedulerAPI.reset();
		
		Assert.assertNotNull("Original scheduler is not null", schedulers[0]);
		Assert.assertNotNull("run scheduler is not null", schedulers[1]);
		Assert.assertNotNull("when scheduler is not null", schedulers[2]);
		Assert.assertNotNull("done scheduler is not null", schedulers[3]);
		
		Assert.assertNotSame("original and run scheduler are different", schedulers[0], schedulers[1]);
		Assert.assertSame("run and when scheduler are the same", schedulers[1], schedulers[2]);
		Assert.assertSame("run and done scheduler are the same", schedulers[1], schedulers[3]);
		Assert.assertSame("original and final scheduler are the same", schedulers[0], schedulers[4]);
		
	}
	
	@Test
	public void testGuardedWhenCallable() throws Exception {
		final boolean[] flags = new boolean[] { false, false };
		final Thread[] threads = new Thread[] { null, null };
		final API api = PromiseAPI.get();
		boolean result = api.call(new Callable<Boolean>() {
			@Override
			public Boolean call() {
				api
					.when(new Callable<Boolean>() {
						public Boolean call() {
							flags[0] = true;
							threads[0] = Thread.currentThread();
							return false;
						}
					})
					.done(new DoneCallback<Boolean>() {
						@Override
						public void onDone(Boolean p) {
							threads[1] = Thread.currentThread();
						}
					})
					.fail(new FailCallback<Exception>() {
						
						@Override
						public void onFail(Exception f) {
							flags[1] = false;
						}
					});
				return true;
			}} );
		Assert.assertTrue("result is true", result);
		Assert.assertTrue("done did run", flags[0]);		
		Assert.assertFalse("fail not run", flags[1]);		
		Assert.assertNotSame("Runs on different thread", Thread.currentThread(), threads[0]);
		Assert.assertSame("Runs on same thread", Thread.currentThread(), threads[1]);
	}
	
	/**
	 * Test that repeated resolutions are an error
	 */
	@Test
	public void testRepeatedResolutionsAreAnError() {
		Deferred<Boolean, Exception> deferred = PromiseAPI.get().deferred();
		deferred.resolve(true);
		try {
			deferred.resolve(false);
			Assert.fail("Expected an exception");
		} catch (IllegalStateException e) {
		}
	}
	/**
	 * Test that repeated rejections are an error
	 */
	@Test
	public void testRepeatedRejectionsAreAnError() {
		Deferred<Boolean, Boolean> deferred = PromiseAPI.get().deferred();
		deferred.reject(true);
		try {
			deferred.reject(true);
			Assert.fail("Expected an exception");
		} catch (IllegalStateException e) {
		}
	}
	
	/**
	 * Test that then() transforms a value.
	 */
	@Test
	public void testThenAfterResolution() {
		final boolean[] done = new boolean[] {false};
		PromiseAPI
			.get()
			.resolved(false, Boolean.class, Exception.class)
			.then(
				new Filter<Boolean, Boolean>() {
					@Override
					public Boolean filter(Boolean p) {
						return !p;
					}
				}
			).done(new DoneCallback<Boolean>() {				
				@Override
				public void onDone(Boolean p) {
					done[0] = p;
				}
			});
	   Assert.assertTrue("done[0]", done[0]);	
	}
	/**
	 * Test that then() transforms a value.
	 */
	@Test
	public void testThenBeforeResolution() {
		final boolean[] done = new boolean[] {false};
		final Deferred<Boolean, Exception> deferred = PromiseAPI
			.get()
			.deferred();
		
			deferred
				.promise()
				.then(
					new Filter<Boolean, Boolean>() {
						@Override
						public Boolean filter(Boolean p) {
							return !p;
						}
					})
				.done(new DoneCallback<Boolean>() {				
						@Override
						public void onDone(Boolean p) {
							done[0] = p;
						}
					});
			
			deferred.resolve(false);
			Assert.assertTrue("done[0]", done[0]);	
	}
	/**
	 * Test that then() transforms a value.
	 */
	@Test
	public void testThenAfterRejection() {
		final Exception[] fail = new Exception[] {null};
		final Exception rejection = new Exception();
		PromiseAPI
			.get()
			.rejected(rejection, Boolean.class, Exception.class)
			.then(
				new Filter<Boolean, Boolean>() {
					@Override
					public Boolean filter(Boolean p) {
						return !p;
					}
				}
			).fail(new FailCallback<Exception>() {				
				@Override
				public void onFail(Exception f) {
					fail[0] = f;
				}
			});
	   Assert.assertSame("rejection == fail[0]", rejection, fail[0]);	
	}

	/**
	 * Test that then() transforms a value.
	 */
	@Test
	public void testThenBeforeRejection() {
		final Exception[] fail = new Exception[] {null};
		final Exception rejection = new Exception();
		final Deferred<Boolean, Exception> deferred = PromiseAPI.get().deferred();
		
		deferred
			.promise()
			.then(
					new Filter<Boolean, Boolean>() {
						@Override
						public Boolean filter(Boolean p) {
							return !p;
						}
					}
				).fail(new FailCallback<Exception>() {				
					@Override
					public void onFail(Exception f) {
						fail[0] = f;
					}
				});
		
		deferred.reject(rejection);
	   Assert.assertSame("rejection == fail[0]", rejection, fail[0]);	
	}
}