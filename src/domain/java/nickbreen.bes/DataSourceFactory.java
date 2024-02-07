package nickbreen.bes;

import com.mchange.v2.c3p0.ComboPooledDataSource;
import nickbreen.bes.processor.DatabaseEventProcessor;

import javax.sql.DataSource;
import java.io.IOError;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Properties;

public interface DataSourceFactory
{
    static DataSource buildDataSource(final URI uri)
    {
        assert "jdbc".equals(uri.getScheme());
        final ComboPooledDataSource ds = new ComboPooledDataSource();
        ds.setJdbcUrl(uri.toString());
        return ds;
    }

    static Properties loadDbProperties(final URI uri)
    {
        final Properties props = new Properties();
        final String db = URI.create(uri.getSchemeSpecificPart()).getScheme();
        try (final InputStream is = DatabaseEventProcessor.class.getResourceAsStream(String.format("/%s.properties", db)))
        {
            if (null != is)
            {
                props.load(is);
            }
        }
        catch (IOException e)
        {
            throw new IOError(e);
        }
        return props;
    }
}
