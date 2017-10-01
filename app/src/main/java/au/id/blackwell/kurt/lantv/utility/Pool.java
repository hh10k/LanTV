package au.id.blackwell.kurt.lantv.utility;

public interface Pool<T> {
    interface Item<T> {
        T get();
        void release();
    }

    interface Callback {
        void run(Item item);
    };

    void request(Callback callback);

    void cancel(Callback callback);
}
