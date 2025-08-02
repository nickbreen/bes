package kiwi.breen.bes;

import com.mchange.v2.c3p0.ComboPooledDataSource;

import javax.sql.DataSource;
import java.net.URI;
import java.util.Optional;

public interface DataSourceFactory
{
    static DataSource buildDataSource(final URI uri)
    {
        return buildDataSource(uri, Optional.empty(), Optional.empty());
    }

    static DataSource buildDataSource(
            final URI uri,
            final String username,
            final String password)
    {
        return buildDataSource(uri, Optional.of(username), Optional.of(password));
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private static DataSource buildDataSource(
            final URI uri,
            final Optional<String> username,
            final Optional<String> password)
    {
        final ComboPooledDataSource ds = new ComboPooledDataSource();
        ds.setJdbcUrl(uri.toString());
        username.ifPresent(ds::setUser);
        password.ifPresent(ds::setPassword);
        return ds;
    }
}
