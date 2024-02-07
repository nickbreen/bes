package nickbreen.bes.processor;

import com.google.devtools.build.v1.OrderedBuildEvent;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;

import javax.sql.DataSource;
import java.io.BufferedReader;
import java.io.IOError;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Objects;
import java.util.stream.Collectors;

public class DatabaseEventProcessor extends BuildEventProcessor
{
    private final DataSource dataSource;
    private final JsonFormat.Printer printer;

    public DatabaseEventProcessor(final DataSource dataSource, final JsonFormat.Printer printer)
    {
        this.dataSource = dataSource;
        this.printer = printer;
    }

    public static void init(final DataSource dataSource, final String name)
    {
        // TODO FlyWay?
        try (final InputStream is = Objects.requireNonNull(DatabaseEventProcessor.class.getResourceAsStream(name)))
        {
            try (final Reader reader = new InputStreamReader(is); final BufferedReader bufferedReader = new BufferedReader(reader))
            {
                final String sql = bufferedReader.lines().collect(Collectors.joining(System.lineSeparator()));
                try (final Connection connection = dataSource.getConnection())
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

    @Override
    protected void accept(final OrderedBuildEvent orderedBuildEvent)
    {
        try (final Connection connection = dataSource.getConnection())
        {
            try
            {
                final String sql = switch (connection.getMetaData().getDatabaseProductName().toLowerCase())
                {
                    case "h2" -> "INSERT INTO event VALUES (?,?,?,?,? FORMAT JSON)";
                    case "sqlite" -> "INSERT INTO event VALUES (?,?,?,?,JSONB(?)) ON CONFLICT DO NOTHING";
                    case "postgresql" -> "INSERT INTO event VALUES (?,?,?,?,CAST(? AS JSONB)) ON CONFLICT DO NOTHING";
                    default -> "INSERT IGNORE INTO event VALUES (?,?,?,?,?)";
                };
                try (final PreparedStatement insert = connection.prepareStatement(sql))
                {
                    insert.setString(1, orderedBuildEvent.getStreamId().getBuildId());
                    insert.setInt(2, orderedBuildEvent.getStreamId().getComponentValue());
                    insert.setString(3, orderedBuildEvent.getStreamId().getInvocationId());
                    insert.setLong(4, orderedBuildEvent.getSequenceNumber());
                    insert.setString(5, printer.print(orderedBuildEvent.getEvent()));
                    insert.execute();
                }
            }
            catch (InvalidProtocolBufferException e)
            {
                throw new IOError(e);
            }
        }
        catch (SQLException e)
        {
            throw new Error(e);
        }
    }
}
