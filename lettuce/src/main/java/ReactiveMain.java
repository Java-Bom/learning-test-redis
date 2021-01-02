import org.reactivestreams.Publisher;

public class ReactiveMain {

    public static void main(String[] args) {
        Publisher<TempInfo> publisher = sub -> sub.onSubscribe(new TempSubscription(sub, "seoul"));
        publisher.subscribe(new TempSubscriber());
    }

}
