package transactionmonad;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.function.Supplier;

public class TestUtil {

    public static <T> Throwable toThrown(Supplier<T> s) {
        try {
            s.get();
        } catch (Throwable t) {
            return t;
        }

        throw new RuntimeException("Expected a throw");
    }


    public static Integer rowCount(ResultSet r) throws SQLException {
        r.last();
        return r.getRow();
    }

}
