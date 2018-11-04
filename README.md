# n26 - Backend Senior Engineer Test

## Notes and remarks

The requirement is that `POST /transactions` and `GET /statistics` endpoints MUST execute in constant time and memory ie `O(1)`. 

Constant time requirement basically crosses out every solution that depends on keeping a structured list of transactions.

Constant memory requirement suggests that in order to keep statistics, we should not be relying on stored transactions but keep a representation of statistics updated with each incoming transaction.

Therefore in this solution I have used a fixed size circular array to keep record of transactions happening in `n` number of time frames called `'Pocket'`s. Each pocket represents a digest of transactions that took place in a certain interval of time. An array of pockets can represent a longer duration of time.

More officially these two parameters are represented as such:
* `Pocket.SPAN_MS`: interval in milliseconds that a single pocket represents in time.
* `Pocket.COUNT`: number of Pockets in the circular array.
    
For example in order to track last `60` seconds we can use a span_ms of `1000ms` and count of `60`. Or `100ms` and count of `600`. In first example, statistics between last 59 ~ 60 seconds will be returned based on the time of invocation (how full the current pocket threads are writing in is). In the second example, with a tighter `SPAN_MS` the results will be shown somewhere between 59.9 ~ 60 seconds. 

Also with a tighter span there are less threads racing for same Pockets but more on that below under concurrency. But basically these parameters can be tweaked based on the requirements, server-load etc.

When a transaction occurs, it is mapped to an index between 0 and `Pocket.COUNT` based on its timestamp. Pocket in this index of array is then updated with the transaction's payload. It is worth mentioning each pocket keeps an up to date representation of statistics such as sum, min, max, count and whenever updated with a new transaction computes the latest values. Therefore satisfying `O(1)` time and memory complexity for adding transactions. 

Later when the controller is asking for the overall statistics, the array is traversed and pockets representing a certain time interval are reduced to a single pocket representing the whole duration which is an operation of `O(array.length)` therefore alsol `O(1)` in terms of memory and time.

## Concurrency

Since this is a web environment and requests will be racing in order to update the same Pocket most of the time, we need a concurrency check in place. Therefore an `AtomicReferenceArray<Pocket>` is used as an array. What this achieves is atomically swapping a reference by using the `getAndUpdate(index, (T) -> T)` method. Atomic swapping of references may fail when many threads are racing to update the same reference but eventually all updates will be performed after retries.

Another important thing to consider is `Pocket` must be immutable in order for this schema to work for reasons such as: 
* Each change to a pocket must change its reference since AtomicReferences operate on `==` instead of `equals()` method.
* Pocket itself is composed of non-atomic members such as count,sum etc, therefore not thread-safe

## Separation of concerns

Last but not least, keeping statistics and storing transactions are separate concerns and therefore separated by an aspect oriented approach in the solution. Any error with keeping statistics should not prevent normal execution path of a `TransactionService`.

 Keeping statistics is enabled by, marking a `TransactionService` implementation with the class level `@KeepStatistics` annotation. When this annotation is in place, `addTransaction(...)` and `deleteTransactions()` methods are intercepted and appropriate `StatisticsService` methods are invoked right after `TransactionService` invocation completes without an exception.