package com.scott.ezmessaging

import com.scott.ezmessaging.provider.DispatcherProvider
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.extension.AfterEachCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext

/**
 * Sets the main coroutine's dispatcher to the provided [testDispatcher] for unit testing.
 * @property testDispatcher A [TestDispatcher].
 * @property dispatcherProvider An instance of a [DispatcherProvider]. All [CoroutineDispatcher]s provided
 * will be the [testDispatcher].
 *
 * Declare it as an Extension:
 *
 * ```
 * @ExtendWith(MainCoroutineRule::class)
 * class MyTestClass
 * ```
 *
 * Then, use `runTest` to execute your tests.
 */
@ExperimentalCoroutinesApi
abstract class MainCoroutineRule(
    private val testDispatcher: TestDispatcher
) : BeforeEachCallback, AfterEachCallback {

    init {
        dispatcherProvider = object : DispatcherProvider {
            override fun main() = testDispatcher
            override fun default() = testDispatcher
            override fun io() = testDispatcher
            override fun unconfined() = testDispatcher
        }
    }

    override fun beforeEach(p0: ExtensionContext?) {
        Dispatchers.setMain(testDispatcher)
    }

    override fun afterEach(p0: ExtensionContext?) {
        Dispatchers.resetMain()
    }

    companion object {
        lateinit var dispatcherProvider: DispatcherProvider
    }
}

/**
 * A [MainCoroutineRule] that uses the [UnconfinedTestDispatcher]
 * Provides no control over the execution of coroutines.
 * In return for giving up this control, however, you are not required to manually call runCurrent()
 * and advanceUntilIdle() in your tests as coroutines are eagerly launched with this dispatcher.
 * Example:
 * ```
 * println("starting")
 * launch(dispatcherProvider.io()) { println("coroutine 1 executing") } // eagerly started
 * launch(dispatcherProvider.io()) { println("coroutine 2 executing") } // eagerly started
 * println("done")
 *
 * The above would print the following:
 * starting
 * coroutine 1 executing
 * coroutine 2 executing
 * done
 * ```
 */
@OptIn(ExperimentalCoroutinesApi::class)
class UnconfinedCoroutineRule : MainCoroutineRule(UnconfinedTestDispatcher())

/**
 * A [MainCoroutineRule] that uses the [StandardTestDispatcher]
 * Provides control over the execution of coroutines.
 * As new coroutines launch, they're queued up on the scheduler and ran when the thread is free to use.
 * Must manually call runCurrent() or advanceUntilIdle() directly after a coroutine for it to run before code after the coroutine will execute.
 * Example:
 * ```
 * println("starting")
 * launch(dispatcherProvider.io()) { println("coroutine 1 executing") } // added to the queue
 * launch(dispatcherProvider.io()) { println("coroutine 2 executing") } // added to the queue
 * println("done") // once this line is executed, the scheduler starts running the coroutines on the queue
 *
 * The above would print the following:
 * starting
 * done
 * coroutine 1 executing
 * coroutine 2 executing
 * ```
 *
 * Example:
 * ```
 * println("starting")
 * launch(dispatcherProvider.io()) { println("coroutine 1 executing") } // added to the queue
 * launch(dispatcherProvider.io()) { println("coroutine 2 executing") } // added to the queue
 * advanceUntilIdle() // executes any coroutines on the queue
 * println("done")
 *
 * The above would print the following:
 * starting
 * coroutine 1 executing
 * coroutine 2 executing
 * done
 * ```
 */
@OptIn(ExperimentalCoroutinesApi::class)
class StandardCoroutineRule() : MainCoroutineRule(StandardTestDispatcher())