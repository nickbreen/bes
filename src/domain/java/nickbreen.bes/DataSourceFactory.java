package nickbreen.bes;

import com.mchange.v2.c3p0.DataSources;
import com.mchange.v2.c3p0.PoolBackedDataSource;
import nickbreen.bes.processor.DatabaseEventProcessor;
import org.mariadb.jdbc.MariaDbPoolDataSource;
import org.postgresql.ds.PGConnectionPoolDataSource;
import org.sqlite.javax.SQLiteConnectionPoolDataSource;

import javax.sql.DataSource;
import java.beans.PropertyVetoException;
import java.io.IOError;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.sql.SQLException;
import java.util.Properties;

public interface DataSourceFactory
{
    static DataSource buildDataSource(final URI uri)
    {
        assert "jdbc".equals(uri.getScheme());
        return switch (URI.create(uri.getSchemeSpecificPart()).getScheme())
        {
            case "sqlite" -> {
                final SQLiteConnectionPoolDataSource ds = new SQLiteConnectionPoolDataSource();
                ds.setUrl(uri.toString());
                yield ds;
            }
            case "pgsql" -> {
                final PGConnectionPoolDataSource pds = new PGConnectionPoolDataSource();
                pds.setUrl(uri.toString());
                final PoolBackedDataSource ds = new PoolBackedDataSource();
                try
                {
                    ds.setConnectionPoolDataSource(pds);
                }
                catch (PropertyVetoException e)
                {
                    throw new Error(e);
                }
                yield ds;
            }
            case "mysql" -> {
                try
                {
                    yield DataSources.pooledDataSource(DataSources.unpooledDataSource(uri.toString(), "root", ""));
                }
                catch (SQLException e)
                {
                    throw new Error(e);
                }
            }
            case "mariadb" -> {
                try
                {
                    yield  new MariaDbPoolDataSource(uri.toString());
                }
                catch (SQLException e)
                {
                    throw new Error(e);
                }
            }
            // case "h2" -> JdbcConnectionPool.create(uri.toString(), "", "");
            default -> throw new Error("Unsupported JDBC URI " + uri);
        };
    }

    static Properties loadDbProperties(final URI uri)
    {
        final Properties props = new Properties();
        final String db = URI.create(uri.getSchemeSpecificPart()).getScheme();
        try (final InputStream is = DatabaseEventProcessor.class.getResourceAsStream(String.format("/%s.properties", db)))
        {
            props.load(is);
        }
        catch (IOException e)
        {
            throw new IOError(e);
        }
        return props;
    }
}
