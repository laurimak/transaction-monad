package transactionmonad;

import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.util.function.Supplier;

public class SpringThreadBoundTransaction implements TransactionWrapper {

    private final TransactionTemplate transactionTemplate;

    public SpringThreadBoundTransaction(DataSource dataSource, TransactionDefinition transactionDefinition) {
        transactionTemplate = new TransactionTemplate(new DataSourceTransactionManager(dataSource), transactionDefinition);
    }

    public SpringThreadBoundTransaction(DataSource dataSource) {
        transactionTemplate = new TransactionTemplate(new DataSourceTransactionManager(dataSource));
    }


    @Override
    public <A> A runTransaction(Supplier<A> supplier) {
        return transactionTemplate.execute(status -> supplier.get());
    }
}
