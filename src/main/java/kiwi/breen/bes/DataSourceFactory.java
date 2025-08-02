package kiwi.breen.bes;

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

    static DataSource buildDataSource(final URI uri, final String username, final String password)
    {
        assert "jdbc".equals(uri.getScheme());
        assert null != username;
        assert null != password;
        final ComboPooledDataSource ds = new ComboPooledDataSource();
        ds.setJdbcUrl(uri.toString());
        ds.setUser(username);
        ds.setPassword(password);
        return ds;
    }

}
