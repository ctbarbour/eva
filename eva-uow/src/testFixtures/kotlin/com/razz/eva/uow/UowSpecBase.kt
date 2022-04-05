package com.razz.eva.uow

import com.razz.eva.domain.Model
import com.razz.eva.domain.ModelEvent
import com.razz.eva.domain.ModelId
import java.util.*

open class UowSpecBase<R> private constructor(
    private val result: R,
    private val executionHistory: Deque<Change>,
    private val publishedEvents: Deque<ModelEvent<out ModelId<out Comparable<*>>>>,
    private val peekingPersisting: PeekingPersisting = PeekingPersisting()
) {

    constructor(
        changes: ChangesWithResult<R>
    ) : this(
        result = changes.result,
        executionHistory = ArrayDeque(changes.toPersist.filter { it != Noop }),
        publishedEvents = ArrayDeque(changes.toPersist.flatMap { it.modelEvents })
    )

    fun verifyEnd() {
        check(executionHistory.isEmpty()) { "No more changes expected" }
        check(publishedEvents.isEmpty()) { "No more events expected" }
    }

    protected fun verifyResult(verification: (R) -> Unit) {
        verification(result)
    }

    protected fun <M : Model<*, *>> verifyAdded(verify: (M) -> Unit): M {
        val model = when (val next = checkNotNull(executionHistory.pollFirst()) { "Expecting [Add] got nothing" }) {
            is Add<*, *, *> -> {
                next.persist(peekingPersisting)
                peekingPersisting.peek()
            }
            else -> throw IllegalStateException("Expecting [Add] was [$next]")
        }
        @Suppress("UNCHECKED_CAST")
        verify(model as M)
        return model
    }

    protected fun <M : Model<*, *>> verifyUpdated(verify: (M) -> Unit) {
        val model = when (val next = checkNotNull(executionHistory.pollFirst()) { "Expecting [Update] got nothing" }) {
            is Update<*, *, *> -> {
                next.persist(peekingPersisting)
                peekingPersisting.peek()
            }
            else -> throw IllegalStateException("Expecting [Update] was [$next]")
        }
        @Suppress("UNCHECKED_CAST")
        verify(model as M)
    }

    protected fun <E : ModelEvent<out ModelId<out Comparable<*>>>> verifyEmitted(verify: (E) -> Unit) {
        @Suppress("UNCHECKED_CAST")
        val next = checkNotNull(publishedEvents.pollFirst()) { "Expecting [ModelEvent] got nothing" } as E
        verify(next)
    }
}
