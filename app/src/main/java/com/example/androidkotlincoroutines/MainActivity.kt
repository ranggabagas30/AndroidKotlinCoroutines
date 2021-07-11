package com.example.androidkotlincoroutines

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.example.androidkotlincoroutines.model.User
import kotlinx.coroutines.*
import java.lang.RuntimeException
import kotlin.Exception
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@InternalCoroutinesApi
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //launch10000Coroutines()
        //usingDelay()
        //dependentJobs()
        //managingJobsHierarchy()
        //childCancellation()
        //cooperativeCancellation()
        //usingReplay()
        //postingToTheUIThread()
        timeout()

        /* suspend */
        //suspendWithCancellable()
        //trySuspendCancellableCoroutine()
        //trySuspendCancellableCoroutineWithCancellation()
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
        Thread.sleep(1000)
    }

    fun managingJobsHierarchy() {
        with(GlobalScope) {
            val parentJob = launch {
                delay(300)
                println("I'm the parent")
                delay(300)
            }
            val job1 = launch(context = parentJob) {
                delay(200)
                println("I'm the first child")
                delay(200)
            }
            val job2 = launch(context = parentJob) {
                delay(200)
                println("I'm the second child")
                delay(200)
            }
            val job3 = launch(context = parentJob) {
                delay(200)
                println("I'm the third child")
                delay(200)
            }
            //parentJob.cancel(null) // will cancel parent and all children jobs
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
            var r = Resource()
            repeat(100_000) {
                launch {
                    val resource = withTimeout(60) { // delay 60 ms
                        delay(50) // delay 50 ms
                        r.allocate()
                        r
                    }
                    resource.close()
                }
            }
            println("final acquired: ${r.acquired}")
        }
    }

    /**
     * suspend with cancellable
     * */
    private suspend fun work(){
        val startTime = System.currentTimeMillis()
        var nextPrintTime = startTime
        var i = 0
        while (i < 5) {
            yield()
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

    private suspend fun fetchUserWithCancellation(): User = suspendCancellableCoroutine { cancellableContinuation ->
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
        Toast.makeText(this, "User name: ${user.name} and address: ${user.address}", Toast.LENGTH_SHORT).show()
    }
}