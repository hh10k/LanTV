package au.id.blackwell.kurt.lantv.utility

import java.util.ArrayDeque
import java.util.ArrayList

class LimitedPool<T> : Pool<T> {
    private val mItems = ArrayList<T>()
    private val mQueue = ArrayDeque<PoolCallback<T>>()

    fun addItem(item: T) {
        mItems.add(item)
    }

    fun removeItem(item: T) {
        mItems.remove(item)
    }

    override fun request(callback: PoolCallback<T>) {
        mQueue.add(callback)
        update()
    }

    override fun cancel(callback: PoolCallback<T>) {
        mQueue.remove(callback)
    }

    private fun update() {
        if (!mItems.isEmpty() && !mQueue.isEmpty()) {
            val requester = mQueue.pop()
            val item = mItems.removeAt(mItems.size - 1)

            requester(object : Pool.Item<T> {
                override fun get(): T {
                    return item
                }

                override fun release() {
                    addItem(item)
                }
            })
        }
    }
}
