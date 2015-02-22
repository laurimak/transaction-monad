package transactionmonad;

import java.util.function.Function;
import java.util.function.Supplier;

public final class TransactionMonad<A> {
    private final Supplier<A> supplier;
    private final Transactor transactor;

    TransactionMonad(Supplier<A> supplier, Transactor transactor) {
        this.supplier = supplier;
        this.transactor = transactor;
    }

    public <B> TransactionMonad<B> map(Function<A, B> f) {
        return new TransactionMonad(() -> f.apply(supplier.get()), transactor);
    }

    public <B> TransactionMonad<B> flatMap(Function<A, TransactionMonad<B>> f) {
        return map(a -> f.apply(a).supplier.get());
    }

    public A commit() {
        return transactor.execute(supplier);
    }
}
