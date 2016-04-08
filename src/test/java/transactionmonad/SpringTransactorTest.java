package transactionmonad;

import org.h2.jdbcx.JdbcConnectionPool;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.interceptor.DefaultTransactionAttribute;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static transactionmonad.TestUtil.rowCount;
import static transactionmonad.TestUtil.toThrown;

public class SpringTransactorTest {

    private final JdbcConnectionPool jdbcConnectionPool = JdbcConnectionPool.create("jdbc:h2:mem:test", "sa", "sa");

    @Before
    public void before() {
        createTestTable();
    }

    @After
    public void after() {
        jdbcConnectionPool.dispose();

    }

    @Test
    public void insert_is_performed_in_transaction() throws ClassNotFoundException, SQLException {
        monad()
                .map(() -> statementResult(s -> s.execute("insert into TEST values (1)")))
                .commit();

        assertThat(statementResult(s -> rowCount(s.executeQuery("select * from TEST"))), is(1));
    }

    @Test
    public void two_inserts_are_mapped_together() throws ClassNotFoundException, SQLException {
        monad()
                .map(() -> statementResult(s -> s.execute("insert into TEST values (1)")))
                .map(() -> statementResult(s -> s.execute("insert into TEST values (1)")))
                .commit();

        assertThat(statementResult(s -> rowCount(s.executeQuery("select * from TEST"))), is(2));
    }

    @Test
    public void two_inserts_are_flatmapped_together() throws ClassNotFoundException, SQLException {
        monad()
                .map(() -> statementResult(s1 -> s1.execute("insert into TEST values (1)")))
                .flatMap(b -> monad()
                        .map(() -> statementResult(s2 -> s2.execute("insert into TEST values (1)"))))
                .commit();

        assertThat(statementResult(s -> rowCount(s.executeQuery("select * from TEST"))), is(2));
    }

    @Test
    public void exception_rolls_back_transaction() throws ClassNotFoundException, SQLException {
        toThrown(() -> monad()
                .map(() -> statementResult(s -> s.execute("insert into TEST values (1)")))
                .map(() -> { throw new RuntimeException(); })
                .commit());

        assertThat(statementResult(s -> rowCount(s.executeQuery("select * from TEST"))), is(0));
    }

    @Test
    public void flatmapping_treats_transactions_as_independent() {
        Transaction<Boolean> transactionThatGetsCommitted = monad(new DefaultTransactionAttribute(TransactionDefinition.PROPAGATION_REQUIRES_NEW))
                .map(() -> statementResult(s -> s.execute("insert into TEST values (1)")));

        Transaction<Boolean> transactionThatIsRolledback = monad()
                .map(() -> statementResult(s -> s.execute("insert into TEST values (1)")));

        toThrown(() ->
                transactionThatIsRolledback.flatMap(b -> transactionThatGetsCommitted)
                        .map(() -> { throw new RuntimeException(); })
                        .commit());

        assertThat(statementResult(s -> rowCount(s.executeQuery("select * from TEST"))), is(1));
    }

    private Transaction<Void> monad() {
        DefaultTransactionAttribute transactionDefinition = new DefaultTransactionAttribute();
        return monad(transactionDefinition);
    }

    private Transaction<Void> monad(TransactionDefinition transactionDefinition) {
        SpringThreadBoundTransaction springTransactor = new SpringThreadBoundTransaction(jdbcConnectionPool, transactionDefinition);
        Transaction<Void> monad = new Transaction<>(() -> (Void) null, springTransactor);
        return monad;
    }

    private <R> R statementResult(ThrowingFunction<Statement, R> f) {
        Connection connection = DataSourceUtils.getConnection(jdbcConnectionPool);
        try (Statement statement = connection.createStatement()) {
            return f.apply(statement);
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            DataSourceUtils.releaseConnection(connection, jdbcConnectionPool);
        }
    }

    private void sql(String sql) {
        statementResult(s -> s.execute(sql));
    }

    private void createTestTable() {
        sql("CREATE TABLE TEST (id INTEGER)");
    }

}