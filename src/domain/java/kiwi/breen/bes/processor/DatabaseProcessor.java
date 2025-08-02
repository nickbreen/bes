package kiwi.breen.bes.processor;

import com.google.devtools.build.v1.OrderedBuildEvent;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;

import javax.sql.DataSource;
import java.io.IOError;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class DatabaseProcessor extends BuildEventProcessor
{
    private final JsonFormat.Printer printer;
    private final DataSource ds;
    private final String sql;

    public DatabaseProcessor(
            final JsonFormat.Printer printer,
            final DataSource ds,
            final String sql)
    {
        this.printer = printer;
        this.ds = ds;
        this.sql = sql;
    }

    @Override
    protected void accept(final OrderedBuildEvent orderedBuildEvent)
    {
        try (final Connection connection = ds.getConnection())
        {
            try (final PreparedStatement insert = connection.prepareStatement(sql))
            {
                insert.setString(1, orderedBuildEvent.getStreamId().getBuildId());
                insert.setInt(2, orderedBuildEvent.getStreamId().getComponentValue()); // Enum! Store the name?
                insert.setString(3, orderedBuildEvent.getStreamId().getInvocationId());
                insert.setLong(4, orderedBuildEvent.getSequenceNumber());
                insert.setString(5, printer.print(orderedBuildEvent.getEvent()));
                insert.execute();
            }
        }
        catch (final SQLException e)
        {
            throw new Error(e);
        }
        catch (final InvalidProtocolBufferException e)
        {
            throw new IOError(e);
        }
    }
}
