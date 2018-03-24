package au.id.blackwell.kurt.lantv.utility

import io.reactivex.Observable

interface Pool<T> {
    fun request(): Observable<T>

    fun addItem(item: T)

    fun removeItem(item: T)
}
