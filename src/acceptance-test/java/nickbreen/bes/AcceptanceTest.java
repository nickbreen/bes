package nickbreen.bes;

import com.google.devtools.build.v1.OrderedBuildEvent;
import com.google.devtools.build.v1.PublishBuildEventGrpc;
import com.google.protobuf.Message;
import io.grpc.Grpc;
import io.grpc.InsecureChannelCredentials;
import io.grpc.ManagedChannel;
import nickbreen.bes.processor.JournalProcessor;
import org.junit.Test;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static nickbreen.bes.TestUtil.loadBinary;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

public class AcceptanceTest
{
    @Test
    public void shouldWriteJournalEquivalentToSource() throws IOException
    {
        final List<Message> sink = new ArrayList<>();
        final PublishBuildEventProcessor service = new PublishBuildEventProcessor(Optional.empty(), List.of(new JournalProcessor(sink::add)));
        final Thread serverThread = new Thread(new BesServer(8888, service));
        serverThread.start();

        final List<OrderedBuildEvent> events = loadBinary(OrderedBuildEvent::parseDelimitedFrom, AcceptanceTest.class::getResourceAsStream, "/jnl.bin");
        BesClient.create(URI.create("grpc://localhost:8888")).accept(events.stream());

        assertThat(sink, hasSize(events.size()));
        assertThat(sink.stream().map(OrderedBuildEvent.class::cast).toList(), equalTo(events));

        serverThread.interrupt();
    }

    @Test
    public void shouldProxyAndWriteEquivalentJournal() throws IOException
    {
        final List<Message> sink = new ArrayList<>();
        final PublishBuildEventProcessor service = new PublishBuildEventProcessor(Optional.empty(), List.of(new JournalProcessor(sink::add)));
        final Thread serverThread = new Thread(new BesServer(28888, service));
        serverThread.start();

        final Thread proxyThread = new Thread(new BesProxy(18888, URI.create("grpc://localhost:28888")));
        proxyThread.start();

        final List<OrderedBuildEvent> events = loadBinary(OrderedBuildEvent::parseDelimitedFrom, AcceptanceTest.class::getResourceAsStream, "/jnl.bin");
        BesClient.create(URI.create("grpc://localhost:28888")).accept(events.stream());

        assertThat(sink, hasSize(events.size()));
        assertThat(sink.stream().map(OrderedBuildEvent.class::cast).toList(), equalTo(events));

        proxyThread.interrupt();
        serverThread.interrupt();
    }

    @Test
    public void shouldWriteEquivalentJournalAndProxyAndWriteEquivalentJournal() throws IOException
    {
        final List<Message> sink2 = new ArrayList<>();
        final PublishBuildEventProcessor service2 = new PublishBuildEventProcessor(Optional.empty(), List.of(new JournalProcessor(sink2::add)));
        final Thread serverThread2 = new Thread(new BesServer(28788, service2));
        serverThread2.start();

        final List<Message> sink1 = new ArrayList<>();
        final ManagedChannel channel = Grpc.newChannelBuilderForAddress("localhost", 28788, InsecureChannelCredentials.create()).build();
        final PublishBuildEventGrpc.PublishBuildEventStub stub = PublishBuildEventGrpc.newStub(channel);
        final PublishBuildEventProcessor service1 = new PublishBuildEventProcessor(Optional.of(stub), List.of(new JournalProcessor(sink1::add)));
        final Thread serverThread1 = new Thread(new BesServer(18788, service1));
        serverThread1.start();

        final List<OrderedBuildEvent> events = loadBinary(OrderedBuildEvent::parseDelimitedFrom, AcceptanceTest.class::getResourceAsStream, "/jnl.bin");
        BesClient.create(URI.create("grpc://localhost:18788")).accept(events.stream());

        serverThread1.interrupt();
        serverThread2.interrupt();

        assertThat(sink1, hasSize(events.size()));
        assertThat(sink1.stream().map(OrderedBuildEvent.class::cast).toList(), equalTo(events));
        assertThat(sink2, hasSize(events.size()));
        assertThat(sink2.stream().map(OrderedBuildEvent.class::cast).toList(), equalTo(events));
    }
}
