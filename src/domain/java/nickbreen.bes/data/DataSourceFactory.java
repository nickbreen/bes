package nickbreen.bes.data;

import com.mchange.v2.c3p0.ComboPooledDataSource;

import javax.sql.DataSource;
import java.net.URI;

public interface DataSourceFactory
{
    static DataSource buildDataSource(final URI uri)
    {
        assert "jdbc".equals(uri.getScheme());
        final ComboPooledDataSource ds = new ComboPooledDataSource();
        ds.setJdbcUrl(uri.toString());
        return ds;
    }

}
