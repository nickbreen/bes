package nickbreen.bes;

import org.h2.jdbcx.JdbcConnectionPool;
import org.sqlite.javax.SQLiteConnectionPoolDataSource;

import javax.sql.DataSource;
import java.net.URI;

public interface DataSourceFactory
{
    static DataSource buildDataSource(final URI uri)
    {
        // TODO use DBCP or HikariCP
        assert "jdbc".equals(uri.getScheme());
        return switch (URI.create(uri.getSchemeSpecificPart()).getScheme())
        {
            case "sqlite" -> {
                final SQLiteConnectionPoolDataSource ds = new SQLiteConnectionPoolDataSource();
                ds.setUrl(uri.toString());
                yield ds;
            }
            case "h2" -> JdbcConnectionPool.create(uri.toString(), "", "");
            default -> throw new Error("Unsupported JDBC driver");
        };
    }
}
