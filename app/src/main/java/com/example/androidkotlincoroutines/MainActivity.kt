package com.example.androidkotlincoroutines

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.example.androidkotlincoroutines.model.User
import kotlinx.coroutines.*
import kotlin.Exception
import kotlin.RuntimeException
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.system.measureTimeMillis

@InternalCoroutinesApi
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        /**
         * References :
         * - https://kotlinlang.org/docs
         * - https://github.com/Kotlin/kotlinx.coroutines/blob/master/ui/coroutines-guide-ui.md
         *
         * */

        /* basic */
        //launch10000Coroutines()
        //usingDelay()
        //joinFunction()
        //dependentJobs()
        //managingJobsHierarchy()
        //childCancellation()
        //cooperativeCancellation()
        //coroutineCancellationCanNotInterruptThread()
        //coroutineCancellationUseDelaySuspendFun()
        //usingReplay()
        //postingToTheUIThread()
        //timeout()

        /* suspend */
        //composingSuspendingFunctions()
        //asyncStyleFunction()
        //structuredConcurrencyWithAsync()
        //suspendWithCancellable()
        //trySuspendCancellableCoroutine()
        //suspendWithCancellableAndSuspendCancellation()

        /* coroutine context and dispatchers */
        //manipulateContext()
        //dispatcherExamples()

        /* flows */
//        with(KotlinFlow) {
//            tryFlow()
//        }
    }

    /**
    public fun CoroutineScope.launch(
    context: CoroutineContext = EmptyCoroutineContext,
    start: CoroutineStart = CoroutineStart.DEFAULT,
    block: suspend CoroutineScope.() -> Unit
    ): Job

    -   A CoroutineContext is a persistent dataset of contextual information about
    the current coroutine. This can contain objects like the Job and Dispatcher
    of the coroutine, both of which you will touch on later. Since you haven’t
    specified anything in the snippet above, it will use the EmptyCoroutineContext,
    which points to whatever context the specified CoroutineScope uses

    -   The CoroutineStart is the mode in which you can start a coroutine. Options are:
    • DEFAULT: Immediately schedules a coroutine for execution according to its context.
    • LAZY: Starts coroutine lazily.
    • ATOMIC: Same as DEFAULT but cannot be cancelled before it starts.
    • UNDISPATCHED: Runs the coroutine until its first suspension point.

     **/
    fun launch10000Coroutines() {
        (1..10000).forEach {
            GlobalScope.launch {
                // This body is coroutine builder
                // GlobalScope means that the coroutine lifecycle is bound to the lifecycle
                // of the application. It means that if application is stopped, so do the coroutine
                val threadName = Thread.currentThread().name
                println("$it printed on thread $threadName")
            }
        }
        // using thread sleep to wait for the coroutine to finish the work
        Thread.sleep(1000)
    }

    fun usingDelay() {
        GlobalScope.launch {
            println("Hello coroutine")
            delay(500)
            println("Right back at ya!")
        }
        Thread.sleep(1000)
    }

    private fun joinFunction() {
        GlobalScope.launch {
            println("parentJob1 Coroutine start")
            val parentJob1 = launch {
                launch {
                    for (i in 0..5) {
                        println("parentJob1, child work $i")
                    }
                }
            }
            parentJob1.join()
            println("parentJob1 Coroutine end")

            println("parentJob2 Coroutine start")
            val parentJob2 = launch {
                launch {
                    for (i in 0..5) {
                        println("parentJob2, child work $i")
                    }
                }
            }
            println("parentJob2 Coroutine end")
        }

        // without using join()
        GlobalScope.launch {
            println("parentJob1 Coroutine start")
             val parentJob1 = launch {
                launch {
                    for (i in 0..5) {
                        println("parentJob1, child work $i")
                    }
                }
            }
            println("parentJob1 Coroutine end")

            println("parentJob2 Coroutine start")
            val parentJob2 = launch {
                launch {
                    for (i in 0..5) {
                        println("parentJob2, child work $i")
                    }
                }
            }
            println("parentJob2 Coroutine end")
        }
    }

    // Job.join suspends a coroutine until the work is completed
    fun dependentJobs() {
        val job1 = GlobalScope.launch(start = CoroutineStart.LAZY) {
            delay(400)
            println("Pong")
            delay(400)
        }
        Log.d(this::dependentJobs.name, "job1 is active? ${job1.isActive}")
        GlobalScope.launch {
            delay(200)
            println("Ping")
            job1.join()
            Log.d("launch", "job1 is completed? ${job1.isCompleted}")
            println("Ping")
            delay(200)
        }
        Log.d(this::dependentJobs.name, "job1 is completed? ${job1.isCompleted}")
        //Thread.sleep(1000)
    }

    fun managingJobsHierarchy() {
        with(MainScope()) {
            val parentJob = launch {
                try {
                    println("I'm the parent")
                    delay(350)
                    this.cancel() // will cancel parent and all children jobs
                } catch (e: CancellationException) {
                    println("parent cancellation: ${e.message}")
                }
            }
            val job1 = launch(context = parentJob) {
                delay(300)
                println("I'm the first child")
            }
            val job2 = launch(context = parentJob) {
                delay(300)
                println("I'm the second child")
            }
            val job3 = launch(context = parentJob) {
                delay(300)
                println("I'm the third child")
            }
            // job1, job2, and job3 are siblings to each other
            // job1.cancel() will not make other sibling jobs cancelled
            parentJob.children.forEach {
                println("== Child's job status ==")
                println("is active?: ${it.isActive}")
                println("is cancelled?: ${it.isCancelled}")
                println("is completed?: ${it.isCompleted}")
            }
            if (parentJob.children.iterator().hasNext()) {
                println("The job has children ${parentJob.children}")
            } else {
                println("The job has no children")
            }
            Thread.sleep(1000)
        }

        // second example
        // parent cancellation, all of its children are getting cancelled
        GlobalScope.launch {
            val parentJob = launch {
                println("job1...")
                val job1 = launch {
                    for (i in 0..5) {
                        delay(100)
                        println("job1, work $i")
                    }
                }

                println("job2...")
                val job2 = launch {
                    for (i in 0..5) {
                        delay(100)
                        println("job2, work $i")
                    }
                }
                delay(1000)
                println("Job2 is cancelled?: ${job2.isCancelled}")
            }
            delay(300)
            parentJob.cancel()
            delay(500)
            println("Job1 is cancelled?: ${parentJob.isCancelled}")
        }
    }

    @InternalCoroutinesApi
    fun childCancellation() {

        // simple example
        runBlocking {
            val job = GlobalScope.launch {
                repeat(1000) { i ->
                    println("job: I'am sleeping $i..")
                    delay(500L)
                }
            }
            delay(1300L)
            println("main: I'm tired of waiting!")
            job.cancel()
            job.join()
            println("main: Now I can quit")
        }

        val parentScope = GlobalScope.launch {
            val job1 = launch {
                println("first child job work 1")
                val job2 = launch {
                    for (i in 0..5) {
                        delay(100)
                        println("second child job work $i")
                    }
                }
                delay(300)
                println("Job2 is cancelled?: ${job2.isCancelled}")
                job2.cancelAndJoin()
                println("Job2 is cancelled?: ${job2.isCancelled}")
                println("first child job work 2")
                val job3 = launch {
                    for (i in 0..5) {
                        if (isActive && i == 3) {
                            cancel()
                        }
                        delay(100)
                        println("third child job work $i")
                    }
                }
                delay(1000)
                println("Job3 is cancelled?: ${job3.isCancelled}")
            }
            delay(2000)
            println("Job1 is canclled?: ${job1.isCancelled}")
        }
        Thread.sleep(3000)
    }

    fun cooperativeCancellation() {
        runBlocking {

            println("=== Creates a blocking coroutine that executes in current thread (main)")
            println("Main program starts: ${Thread.currentThread().name}")

            val job = launch { // Thread T1: Creates a non-blocking coroutine
                for (i in 0..1000) {
                    print("$i.")
                }
            }
            job.join() // waits for coroutine to finish
            println("\nMain program ends: ${Thread.currentThread().name}") // main thread

            println("\n\n=== uncheck cancellation, thus can not be cancelled")
            val startTime = System.currentTimeMillis()
            val job2 = launch(Dispatchers.Default) {
                var nextPrintTime = startTime
                var i = 0
                while (i < 5) {
                    if (System.currentTimeMillis() >= nextPrintTime) {
                        println("job: I'm sleeping ${i++} .. ")
                        nextPrintTime += 500L
                    }
                }
            }
            delay(1300L)
            println("main: I'm tired of waiting")
            job2.cancelAndJoin()
            println("main: Now I can quit")

            println("\n\n=== Make computation code cancellable")
            val startTime2 = System.currentTimeMillis()
            val job3 = launch(Dispatchers.Default) {
                var nextPrintTime = startTime2
                var i = 0
                while (isActive) {
                    if (System.currentTimeMillis() >= nextPrintTime) {
                        println("job: I'm sleeping ${i++} .. ")
                        nextPrintTime += 500L
                    }
                }
            }
            delay(1300L)
            println("main: I'm tired of waiting")
            job3.cancelAndJoin()
            println("main: Now I can quit")

        }
    }

    fun coroutineCancellationCanNotInterruptThread() {
        runBlocking {
            println("launching")
            try {
                withTimeout(1000) {
                    withContext(Dispatchers.IO) {
                        println("sleeping")
                        // coroutine could not cancel / interrupt the Thread https://discuss.kotlinlang.org/t/calling-blocking-code-in-coroutines/2368/5, so
                        // the coroutine wait for the Thread until finish, then return the result of exception
                        Thread.sleep(2000)
                        println("after sleeping")
                    }
                }
            } catch (e: TimeoutCancellationException) {
                println("Time out: $e")
            }
        }
    }

    fun coroutineCancellationUseDelaySuspendFun() {
        runBlocking {
            println("launching")
            launch {
                try {
                    withTimeout(1000) {
                        withContext(Dispatchers.IO) {
                            println("sleeping")
                            // coroutine could not cancel / interrupt the Thread https://discuss.kotlinlang.org/t/calling-blocking-code-in-coroutines/2368/5, so
                            // the coroutine wait for the Thread until finish, then return the result of exception
                            delay(2000)
                            println("after sleeping") // this code won't ever be executed if the coroutine is cancelled by the timeout
                        }
                    }
                    println("Test after timeout") // this code won't ever be executed if the coroutine is cancelled by the timeout
                } catch (e: TimeoutCancellationException) {
                    println("Time out: $e")
                } finally {
                    work() // still can be executed, since only the withTimeout which is cancelled
                }
            }
        }
    }

    fun usingReplay() {
        var isDoorOpen = false

        println("Unlocking the door. Please wait")
        GlobalScope.launch {
            delay(3000)
            isDoorOpen = true
        }
        GlobalScope.launch {
            repeat(4) {
                println("Trying to open the door...\n")
                delay(800)

                if (isDoorOpen) {
                    println("Opened the door!\n")
                } else {
                    println("The door is still locked\n")
                }
            }
        }
        Thread.sleep(5000)
    }

    fun postingToTheUIThread() {
        GlobalScope.launch {
            val bgThreadName = Thread.currentThread().name
            println("I'm Job 1 in thread $bgThreadName")
            delay(200)
            GlobalScope.launch(Dispatchers.Main) {
                val uiThreadName = Thread.currentThread().name
                println("I'm Job 2 in thread $uiThreadName")
            }
        }
        Thread.sleep(1000)
    }

    internal class Resource {
        var acquired = 0

        fun allocate() {
            acquired++ // Acquire the resource
            println("acquire: $acquired")
        }

        fun close() {
            acquired--
            println("release: $acquired")
        } // Release the resource
    }

    fun timeout() {
        var r: Resource? = null
        runBlocking {
            try {
                println("=== using timeout for cancellation after x seconds")
                withTimeout(1300L) {
                    repeat(1000) { i ->
                        println("I'm sleeping $i...")
                        delay(500L)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            println("\n\n=== using withTimeoutOrNull will return null instead exception if timeout")
            val result = withTimeoutOrNull(1300L) {
                repeat(1000) { i ->
                    println("I'm sleeping $i...")
                    delay(500L)
                }
                "Done"
            }
            println("Result is $result")

            println("\n\n=== timeout is async and resources")
            r = Resource()
            repeat(10_000) {
                println("$it iteration outer block")
                launch {
                    println("$it iteration launch block")
                    val resource = withTimeout(1000) { // delay 1000 ms
                        println("-> before delay")
                        delay(90) // delay 90 ms
                        println("-> after delay 90 ms")
                        r?.allocate()
                        r
                    }
                    resource?.close()
                }
            }
        }
        println("final acquired: ${r?.acquired}")
    }


    /**
     * Composing suspending functions
     * */
    private suspend fun doSomethingOne(): Int {
        delay(1000L) // pretend do something useful
        println("-> success do something one")
        return 13
    }

    private suspend fun doSomethingTwo(): Int {
        delay(500L) // pretend do something useful
        println("-> success do something two")
        return 29
    }

    private fun composingSuspendingFunctions() {
        runBlocking {
            // assumes the doSomethingOne is the dependency to decide whether to run doSomethingTwo or not
            val time = measureTimeMillis {
                val one = doSomethingOne()
                val two = doSomethingTwo()
                println("The answer is ${one + two}")
            }
            println("Completed in $time millis")

            // assumes there are no dependencies between doSomethingOne() and doSomethingTwo() and want to get answer
            // faster by doing concurrently
            val time2 = measureTimeMillis {
                val one = async { doSomethingOne() }
                val two = async { doSomethingTwo() }
                println("The answer is ${one.await() + two.await()}")
            }
            println("Completed in $time2 ms")

            // start async lazy, only. It only starts the coroutine when its result is required by await, or
            // if its Job 's start function is invoked
            val time3 = measureTimeMillis {
                val one = async(start = CoroutineStart.LAZY) { doSomethingOne() }
                val two = async(start = CoroutineStart.LAZY) { doSomethingTwo() }
                // some computation
                one.start()
                two.start()
                println("The answer is ${one.await() + two.await()}")
            }
            println("Completed in $time3 ms")

            // async lazy not calling start() will make both suspend async function sequentially executed which is not the intended behaviour
            val time4 = measureTimeMillis {
                val one = async(start = CoroutineStart.LAZY) { doSomethingOne() }
                val two = async(start = CoroutineStart.LAZY) { doSomethingTwo() }
                // some computation
                //one.start()
                //two.start()
                println("The answer is ${one.await() + two.await()}")
            }
            println("Completed in $time4 ms")
        }
    }

    /**
     * Async-style functions
     * */

    // below are not suspending functions
    private fun somethingUsefulOneAsync() = GlobalScope.async {
        doSomethingOne()
    }

    private fun somethingUsefulTwoAsync() = GlobalScope.async {
        doSomethingTwo()
    }

    private fun asyncStyleFunction() {
        val time = measureTimeMillis {
            // initiate async actions outside of a coroutine
            val one = somethingUsefulOneAsync()
            val two = somethingUsefulTwoAsync()
            // but waiting for a result must involve either suspending or blocking.
            // here we use `runBlocking { ... }` to block the main thread while waiting for the result
            runBlocking {
                println("The answer is ${one.await() + two.await()}")
            }
        }
        println("Completed in $time ms")

        // using run blocking as a root coroutine scope builder
        runBlocking {
            val time = measureTimeMillis {
                // initiate async actions outside of a coroutine
                val one = somethingUsefulOneAsync()
                val two = somethingUsefulTwoAsync()
                println("The answer is ${one.await() + two.await()}")
            }
            println("Completed in $time ms")
        }

        // Simulate error and throws an exception between async function and the await() call.
        runBlocking {
            try {
                val time = measureTimeMillis {
                    // The async function still running in the background, even though the operation that initiated it
                    // was aborted
                    val one = somethingUsefulOneAsync()
                    val two = somethingUsefulTwoAsync()
                    throw RuntimeException("sammple error")
                    println("The answer is ${one.await() + two.await()}")
                }
                println("Completed in $time ms")
            } catch (e: Exception) {
                Log.e(this@MainActivity::class.java.simpleName, "error exception: $e")
            }
        }
    }

    /**
     * Structured concurrency with async
     * */
    private suspend fun concurrentSum(): Int = coroutineScope {
        val one = async<Int> {
            try {
                delay(Long.MAX_VALUE) // Emulates very long computation
                42
            } finally {
                println("First child was cancelled")
            }
        }
        val two = async<Int> {
            println("Second child throws an exception")
            throw ArithmeticException()
        }
        one.await() + two.await()
    }

    private fun structuredConcurrencyWithAsync() {
        // This way, if something goes wrong inside the code of the concurrentSum function,
        // and it throws an exception, all the coroutines that were launched in its scope will
        // be cancelled.
        runBlocking {
            try {
                val time = measureTimeMillis {
                    println("The answer is ${concurrentSum()}")
                }
                println("Completed in $time ms")
            } catch (e: ArithmeticException) {
                println("Computation failed with ArithmeticException")
            }
        }
    }

    /**
     * suspend with cancellable
     * */
    private suspend fun work() {
        val startTime = System.currentTimeMillis()
        var nextPrintTime = startTime
        var i = 0
        while (i < 1000) {
            yield() // to make this suspend function cancellable
            // print a message twice a second
            if (System.currentTimeMillis() >= nextPrintTime) {
                println("Hello ${i++}")
                nextPrintTime += 500L
            }
        }
    }

    private fun suspendWithCancellable() {
        runBlocking {
            val job = launch {
                try {
                    work()
                } catch (e: CancellationException) {
                    Log.e("suspend", e.message, e)
                } finally {
                    println("Clean up!")

                    // below suspend function will never be called, because the job is cancelled
                    // A coroutine in the cancelling state is not able to suspend!
                    work()
                    println("Work completed!")
                }
            }
            delay(1000L)
            println("Cancel!")
            job.cancel()
            println("Done!")
        }
    }

    private fun suspendWithCancellableAndSuspendCancellation() {
        runBlocking {
            println("=== catch cancellation exception ===")
            val job = launch {
                try {
                    work()
                } catch (e: CancellationException) {
                    Log.e("suspend", e.message, e)
                } finally {
                    println("Clean up!")
                    // To be able to call suspend functions when a coroutine is cancelled, we will need to switch the cleanup work we need to do in a
                    // NonCancellable CoroutineContext. This will allow the code to suspend and will keep the coroutine in the Cancelling
                    // state until the work is done.
                    withContext(NonCancellable) {
                        work()
                    }
                    println("Work completed!")
                }
            }
            delay(1000L)
            println("Cancel!")
            job.cancel()
            println("Done!")

            // below snippet will not get CancellationException and will continue the work, since the try catch
            // block is outside of the launch block
            println("=== try catch outside launch ===")
            try {
                val job2 = launch {
                    work()
                }
                delay(1000L)
                println("Batalkan!")
                job2.cancel()
                println("Selesai!")
            } catch (e: CancellationException) {
                Log.e("Coba suspend", e.message, e)
            } finally {
                println("Bersihkan!")
            }
        }
    }

    /**
     * suspendCancellableCoroutine
     * */
    interface Callback {
        fun onSuccess(user: User)
        fun onFailure(exception: Exception)
    }

    private fun trySuspendCancellableCoroutine() {
        MainScope().launch {
            try {
                val user = fetchUser()
                updateUser(user)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            println("this is line after try catch")
        }
    }

    private fun trySuspendCancellableCoroutineWithCancellation() {
        MainScope().launch {
            try {
                val user = fetchUserWithCancellation()
                updateUser(user)
            } catch (e: Exception) {
                e.printStackTrace() // return java.util.concurrent.CancellationException: Continuation CancellableContinuation
            }
            println("this is line after try catch")

            // running suspend function with cancellation without try catch
            val user = fetchUserWithCancellation() // no crash for CancellationException
            updateUser(user)

            // below line will not be executed if the CancellationException is not handled
            // because this job itself is cancelled
            println("this is line after without try catch block")
        }
    }

    private suspend fun fetchUser(): User = suspendCancellableCoroutine { cancellableContinuation ->
        fetchUserFromNetwork(object : Callback {
            override fun onSuccess(user: User) {
                cancellableContinuation.resume(user)
            }

            override fun onFailure(exception: Exception) {
                cancellableContinuation.resumeWithException(exception)
            }
        })
    }

    private suspend fun fetchUserWithCancellation(): User =
        suspendCancellableCoroutine { cancellableContinuation ->
            fetchUserFromNetwork(object : Callback {
                override fun onSuccess(user: User) {
                    cancellableContinuation.resume(user)
                }

                override fun onFailure(exception: Exception) {
                    cancellableContinuation.resumeWithException(exception)
                }
            })

            // We call "continuation.cancel()" to cancel this suspend function
            cancellableContinuation.cancel()

            println("After cancellation inside suspend ")
        }

    private fun fetchUserFromNetwork(callback: Callback) {
        Thread {
            Thread.sleep(3000)

            // Invokes on success with user data
            callback.onSuccess(User("Rangga", "KHI"))
        }.start()
    }

    private fun fetchUserFromNetworkFailure(callback: Callback) {
        Thread {
            Thread.sleep(3000)

            // Invokes on failure
            callback.onFailure(RuntimeException("error get user"))
        }.start()
    }

    private fun updateUser(user: User) {
        Toast.makeText(
            this,
            "User name: ${user.name} and address: ${user.address}",
            Toast.LENGTH_SHORT
        ).show()
    }

    /**
     * Coroutine Context and Dispatchers
     * */
    private fun manipulateContext() {
        /*
        * The notable design here is that an instance of Element is a singleton CoroutineContext by itself.
        * Hence, we can easily create a new context by adding an element to a context
        * */
        val context = EmptyCoroutineContext
        val newContext = context + CoroutineName("rangga")
        println("is newContext != context? ${newContext != context}")
        println("is 'rangga' == newContext name? ${"rangga" == newContext[CoroutineName]!!.name}")

        /*
        * Or we can remove an element from a CoroutineContext by calling CoroutineContext#minusKey
        * */
        val newContext2 = newContext.minusKey(CoroutineName)
        println("is newContext2 != newContext? ${newContext2 != newContext}")
        println("is newContext2 != context? ${newContext2 != context}")
    }

    @ObsoleteCoroutinesApi
    private fun dispatcherExamples() {
//        runBlocking {
//            println("runBlocking working in thread ${Thread.currentThread().name}")
//            launch(Dispatchers.Unconfined) {
//                println("Unconfined : I'm working in thread ${Thread.currentThread().name}")
//                delay(500) // run in thread kotlinx.coroutines.DefaultExecutor
//                println("Unconfined : After delay in thread ${Thread.currentThread().name}") // Unconfined will also run in the thread in which delay is executed
//            }
//            launch {
//                println("main runBlocking : I'm working in thread ${Thread.currentThread().name}")
//                delay(500)
//                println("main runBlocking : After delay in thread ${Thread.currentThread().name}")
//            }
//            launch(Dispatchers.Default) {
//                println("Default : I'm working in thread ${Thread.currentThread().name}")
//            }
//            launch(newSingleThreadContext("MyOwnThread")) {
//                println("newSingleThreadContext: I'm working in thread ${Thread.currentThread().name}")
//            }
//        }

        runBlocking {
            println("runBlocking2 working in thread ${Thread.currentThread().name}")
            // using start = CoroutineStart.UNDISPATCHED and context = Dispatchers.DEFAULT
            launch(start = CoroutineStart.UNDISPATCHED) {
                println("CoroutineStart.UNDISPATCHED : working in thread ${Thread.currentThread().name}")
                delay(500)
                println("CoroutineStart.UNDISPATCHED : after delay in thread ${Thread.currentThread().name}")
            }

            // using start = CoroutineStart.DEFAULT and context = Dispatchers.Unconfined
            launch(Dispatchers.Unconfined) {
                println("Unconfined : I'm working in thread ${Thread.currentThread().name}")
                delay(500) // run in thread kotlinx.coroutines.DefaultExecutor
                println("Unconfined : After delay in thread ${Thread.currentThread().name}") // Unconfined will also run in the thread in which delay is executed
            }
        }
    }
}