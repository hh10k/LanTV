package au.id.blackwell.kurt.lantv.utility

typealias PoolCallback<T> = (Pool.Item<T>) -> Unit

interface Pool<T> {
    interface Item<T> {
        fun get(): T
        fun release()
    }

    fun request(callback: PoolCallback<T>)

    fun cancel(callback: PoolCallback<T>)
}
