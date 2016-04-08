package transactionmonad;

import java.util.function.Function;
import java.util.function.Supplier;

public final class Transaction<A> {
    private final Supplier<A> supplier;
    private final TransactionWrapper transactionWrapper;

    Transaction(Supplier<A> supplier, TransactionWrapper transactor) {
        this.supplier = supplier;
        this.transactionWrapper = transactor;
    }

    public <B> Transaction<B> map(Function<? super A, B> f) {
        return new Transaction(() -> f.apply(supplier.get()), transactionWrapper);
    }

    public <B> Transaction<B> map(Supplier<B> f) {
        return map(o -> f.get());
    }

    public <B> Transaction<B> flatMap(Function<? super A, Transaction<B>> f) {
        return map(a -> f.apply(a).commit());
    }

    public A commit() {
        return transactionWrapper.runTransaction(supplier);
    }
}
