#NAME
promise - a JQuery-like promises API for Java

#DESCRIPTION
This library provides an implementation of JQuery-like promises API which is similar to [JDeferred](https://github.com/jdeferred/jdeferred).

Unlike JDeferred, promises for work completed by asynchronous threads are delivered on the caller's synchronous thread rather than on some other asynchronous thread. This behaviour helps reduce the need for application code to  explicitly perform locking in order to safely initiate work on, and receive results from, asycnhronous threads.

For more information about the rationale behind the design of this API and the underlying tasklets API, refer to this presentation ["Tasklets, Promises and Their Application To A Java Raft Consensus Algorithm Implementation
"](https://docs.google.com/presentation/d/1zIRf-X9PdezrJUjGxncfsMto5je9r43PAEkZa7Hs_5I/edit#slide=id.p), the Javadoc or the test cases.


