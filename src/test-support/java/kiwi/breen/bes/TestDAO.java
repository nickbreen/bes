package kiwi.breen.bes;

import javax.sql.DataSource;
import java.io.BufferedReader;
import java.io.IOError;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class TestDAO
{
    public record Event(String buildId, int component, String invocationId, long sequence, String bazelEventStartedUuid)
    {

    }
    private final DataSource ds;

    public TestDAO(final DataSource ds)
    {
        this.ds = ds;
    }

    public void init(final String name)
    {
        try (final InputStream is = Objects.requireNonNull(TestDAO.class.getResourceAsStream(name)))
        {
            try (final Reader reader = new InputStreamReader(is); final BufferedReader bufferedReader = new BufferedReader(reader))
            {
                // new String(is.readAllBytes())?
                final String sql = bufferedReader.lines().collect(Collectors.joining(System.lineSeparator()));
                try (final Connection connection = ds.getConnection())
                {
                    try (final Statement statement = connection.createStatement())
                    {
                        statement.execute(sql);
                    }
                }
                catch (SQLException e)
                {
                    throw new Error(e);
                }
            }
        }
        catch (IOException e)
        {
            throw new IOError(e);
        }
    }

    public void clear()
    {
        try (final Connection connection = ds.getConnection())
        {
            try (final Statement statement = connection.createStatement())
            {
                statement.execute("DELETE FROM event");
            }
        }
        catch (SQLException e)
        {
            throw new Error(e);
        }
    }

    public void allEvents(final Consumer<Event> consumer)
    {
        try (final Connection connection = ds.getConnection())
        {
            final String sql = switch (connection.getMetaData().getDatabaseProductName().toLowerCase())
            {
                case "mariadb" -> "SELECT *, JSON_VALUE(event, '$.bazelEvent.started.uuid') AS started_uuid FROM event";
                case "postgresql" -> "SELECT *, event #>> '{bazelEvent,started,uuid}' AS started_uuid FROM event";
                default -> "SELECT *, event ->> '$.bazelEvent.started.uuid' AS started_uuid FROM event";
            };
            try (final PreparedStatement select = connection.prepareStatement(sql))
            {
                try (final ResultSet resultSet = select.executeQuery())
                {
                    while (resultSet.next())
                    {
                        consumer.accept(new Event(
                                resultSet.getString("build_id"),
                                resultSet.getInt("component"),
                                resultSet.getString("invocation_id"),
                                resultSet.getLong("sequence"),
                                resultSet.getString("started_uuid")));
                    }
                }
            }
        }
        catch (SQLException e)
        {
            throw new Error(e);
        }
    }

    public void allEventsByBuild(final Consumer<Event> consumer, final String buildId)
    {
        try (final Connection connection = ds.getConnection())
        {
            final String sql = switch (connection.getMetaData().getDatabaseProductName().toLowerCase())
            {
                case "mariadb" -> "SELECT *, JSON_VALUE(event, '$.bazelEvent.started.uuid') AS started_uuid FROM event WHERE build_id = ?";
                case "postgresql" -> "SELECT *, event #>> '{bazelEvent,started,uuid}' AS started_uuid FROM event WHERE build_id = ?";
                default -> "SELECT *, event ->> '$.bazelEvent.started.uuid' AS started_uuid FROM event WHERE build_id = ?";
            };
            try (final PreparedStatement select = connection.prepareStatement(sql))
            {
                select.setString(1, buildId);

                try (final ResultSet resultSet = select.executeQuery())
                {
                    while (resultSet.next())
                    {
                        consumer.accept(new Event(
                                resultSet.getString("build_id"),
                                resultSet.getInt("component"),
                                resultSet.getString("invocation_id"),
                                resultSet.getLong("sequence"),
                                resultSet.getString("started_uuid")));
                    }
                }
            }
        }
        catch (SQLException e)
        {
            throw new Error(e);
        }
    }
}
