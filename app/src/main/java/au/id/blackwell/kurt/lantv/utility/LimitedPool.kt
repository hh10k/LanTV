package au.id.blackwell.kurt.lantv.utility

import io.reactivex.Observable
import io.reactivex.subjects.SingleSubject
import java.util.ArrayDeque
import java.util.ArrayList

class LimitedPool<T> : Pool<T> {
    private val mItems = ArrayList<T>()
    private val mQueue = ArrayDeque<SingleSubject<T>>()

    override fun addItem(item: T) {
        mItems.add(item)
    }

    override fun removeItem(item: T) {
        mItems.remove(item)
    }

    override fun request(): Observable<T> {
        val subject = SingleSubject.create<T>()
        mQueue.add(subject)
        update()

        return subject.toObservable()
            .doOnDispose({ mQueue.remove(subject) })
    }

    private fun update() {
        if (!mItems.isEmpty() && !mQueue.isEmpty()) {
            val subject = mQueue.pop()
            val item = mItems.removeAt(mItems.size - 1)

            subject.onSuccess(item)
        }
    }
}
