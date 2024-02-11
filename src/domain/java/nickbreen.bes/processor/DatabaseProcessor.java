package nickbreen.bes.processor;

import com.google.devtools.build.v1.OrderedBuildEvent;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import nickbreen.bes.data.EventDAO;

import java.io.IOError;

public class DatabaseProcessor extends BuildEventProcessor
{
    private final EventDAO dao;
    private final JsonFormat.Printer printer;

    public DatabaseProcessor(final EventDAO eventDao, final JsonFormat.Printer printer)
    {
        this.dao = eventDao;
        this.printer = printer;
    }

    @Override
    protected void accept(final OrderedBuildEvent orderedBuildEvent)
    {
        try
        {
            dao.insert(new EventDAO.Event(
                    orderedBuildEvent.getStreamId().getBuildId(),
                    orderedBuildEvent.getStreamId().getComponentValue(),
                    orderedBuildEvent.getStreamId().getInvocationId(),
                    orderedBuildEvent.getSequenceNumber(),
                    printer.print(orderedBuildEvent.getEvent())));
        }
        catch (InvalidProtocolBufferException e)
        {
            throw new IOError(e);
        }
    }
}
