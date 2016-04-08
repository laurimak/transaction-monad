package transactionmonad;

import java.util.function.Supplier;

interface TransactionWrapper {
    <A> A runTransaction(Supplier<A> supplier);
}
