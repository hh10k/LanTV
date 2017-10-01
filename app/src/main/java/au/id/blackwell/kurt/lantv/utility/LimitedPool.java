package au.id.blackwell.kurt.lantv.utility;

import java.util.ArrayDeque;
import java.util.ArrayList;

public class LimitedPool<T> implements Pool<T> {
    private final ArrayList<T> mItems = new ArrayList<T>();
    private final ArrayDeque<Callback> mQueue = new ArrayDeque<Callback>();

    public void addItem(T item) {
        mItems.add(item);
    }

    public void removeItem(T item) {
        mItems.remove(item);
    }

    @Override
    public void request(Callback callback) {
        mQueue.add(callback);
        update();
    }

    @Override
    public void cancel(Callback callback) {
        mQueue.remove(callback);
    }

    private void update() {
        if (!mItems.isEmpty() && !mQueue.isEmpty()) {
            final Callback requester = mQueue.pop();
            final T item = mItems.remove(mItems.size() - 1);

            requester.run(new Item() {
                @Override
                public T get() {
                    return item;
                }

                @Override
                public void release() {
                    addItem(item);
                }
            });
        }
    }
}
