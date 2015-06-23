package com.wildducktheories.promise.impl;

import org.junit.Assert;
import org.junit.Test;

import com.wildducktheories.promise.DoneCallback;
import com.wildducktheories.promise.FailCallback;
import com.wildducktheories.promise.Promise;
import com.wildducktheories.promise.PromiseAPI;

public class AbstractCallImplTest {
	
	public static class TestCall extends AbstractCallImpl<Integer,Exception,Integer,Integer>
	{
		private int r;

		public TestCall(int r) {
			super();
			this.r = r;
		}

		@Override
		protected Integer build() throws Exception {
			return r;
		}

		@Override
		protected Integer send(Integer r) throws Exception {
			return r*2;
		}

		@Override
		protected Integer receive(Integer s) throws Exception {
			return s*2;
		}

		@Override
		protected Exception handle(Exception e) {
			return e;
		}
		
	}

	@Test
	public void testBasic() 
	{
		final Integer[] result = new Integer[] { null };
		final Exception[] exceptions = new Exception [] { null };
		final TestCall call = new TestCall(1);
		PromiseAPI.get().run(new Runnable() {
			public void run() {
				Promise<Integer, Exception> promise = call.call();
				promise
					.done(new DoneCallback<Integer>() {
						
						@Override
						public void onDone(Integer p) {
							result[0] = p;
						}
						})
					.fail(new FailCallback<Exception>() {
						@Override
						public void onFail(Exception f) {
							exceptions[0] = f;
						}
						});
				Assert.assertNotNull("result[0]", result[0]);
				Assert.assertEquals("result == 4", 4, (int) result[0]);
				
			}
		});
	}
	
	@Test
	public void testSendThrows() 
	{
		final Exception failure = new Exception();
		final Integer[] result = new Integer[] { null };
		final Exception[] exceptions = new Exception [] { null };
		final TestCall call = new TestCall(1) {
			protected Integer send(Integer r) throws Exception {
				throw failure;
			}
		};
		PromiseAPI.get().run(new Runnable() {
			public void run() {
				Promise<Integer, Exception> promise = call.call();
				promise
					.done(new DoneCallback<Integer>() {
						
						@Override
						public void onDone(Integer p) {
							result[0] = p;
						}
						})
					.fail(new FailCallback<Exception>() {
						@Override
						public void onFail(Exception f) {
							exceptions[0] = f;
						}
						});
				Assert.assertNull("result[0]", result[0]);
				Assert.assertSame("failure == exceptions[0]", failure, exceptions[0]);
				
			}
		});
	}
	
	@Test
	public void testBuildThrows() 
	{
		final Exception failure = new Exception();
		final Integer[] result = new Integer[] { null };
		final Exception[] exceptions = new Exception [] { null };
		final TestCall call = new TestCall(1) {
			protected Integer build() throws Exception {
				throw failure;
			}
		};
		PromiseAPI.get().run(new Runnable() {
			public void run() {
				Promise<Integer, Exception> promise = call.call();
				promise
					.done(new DoneCallback<Integer>() {
						
						@Override
						public void onDone(Integer p) {
							result[0] = p;
						}
						})
					.fail(new FailCallback<Exception>() {
						@Override
						public void onFail(Exception f) {
							exceptions[0] = f;
						}
						});
				Assert.assertNull("result[0]", result[0]);
				Assert.assertSame("failure == exceptions[0]", failure, exceptions[0]);
				
			}
		});
	}

}
