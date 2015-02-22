package transactionmonad;

import java.util.function.Supplier;

interface Transactor {
    <A> A execute(Supplier<A> supplier);
}
