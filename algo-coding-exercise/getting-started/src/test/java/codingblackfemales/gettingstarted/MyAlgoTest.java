package codingblackfemales.gettingstarted;

import codingblackfemales.algo.AlgoLogic;
import messages.marketdata.BookUpdateEncoder;
import messages.marketdata.InstrumentStatus;
import messages.marketdata.MessageHeaderEncoder;
import messages.marketdata.Source;
import messages.marketdata.Venue;
import messages.order.Side;

import static org.junit.Assert.assertEquals;

import java.nio.ByteBuffer;

import org.agrona.concurrent.UnsafeBuffer;
import org.junit.Test;


/**
 * This test is designed to check your algo behavior in isolation of the order book.
 *
 * You can tick in market data messages by creating new versions of createTick() (ex. createTick2, createTickMore etc..)
 *
 * You should then add behaviour to your algo to respond to that market data by creating or cancelling child orders.
 *
 * When you are comfortable you algo does what you expect, then you can move on to creating the MyAlgoBackTest.
 *
 */
public class MyAlgoTest extends AbstractAlgoTest {

    @Override
    public AlgoLogic createAlgoLogic() {
        //this adds your algo logic to the container classes
        return new MyAlgoLogic();
    }

    // I added a second market-data scenario to test how my algo responds when the best bid changes from 98 to 95
    protected UnsafeBuffer createTick2() {
        final MessageHeaderEncoder headerEncoder = new MessageHeaderEncoder();
        final BookUpdateEncoder encoder = new BookUpdateEncoder();

        final ByteBuffer byteBuffer = ByteBuffer.allocateDirect(1024);
        final UnsafeBuffer directBuffer = new UnsafeBuffer(byteBuffer);

        
        encoder.wrapAndApplyHeader(directBuffer, 0, headerEncoder);

        
        encoder.venue(Venue.LME);
        encoder.instrumentId(123L);

        encoder.bidBookCount(3)
                .next().price(95L).size(100L)
                .next().price(93L).size(200L)
                .next().price(91L).size(300L);

        encoder.askBookCount(4)
                .next().price(98L).size(501L)
                .next().price(101L).size(200L)
                .next().price(110L).size(5000L)
                .next().price(119L).size(5600L);

        encoder.instrumentStatus(InstrumentStatus.CONTINUOUS);
        encoder.source(Source.STREAM);

        return directBuffer;
    }

    protected UnsafeBuffer createLargeQuantityTick() {
        
        final MessageHeaderEncoder headerEncoder = new MessageHeaderEncoder();
        final BookUpdateEncoder encoder = new BookUpdateEncoder();

        final ByteBuffer byteBuffer = ByteBuffer.allocateDirect(1024);
        final UnsafeBuffer directBuffer = new UnsafeBuffer(byteBuffer);

        encoder.wrapAndApplyHeader(directBuffer, 0, headerEncoder);

        encoder.venue(Venue.LME);
        encoder.instrumentId(123L);

        encoder.bidBookCount(3)
            .next().price(98L).size(500L)
            .next().price(95L).size(200L)
            .next().price(91L).size(300L);

        encoder.askBookCount(4)
            .next().price(100L).size(101L)
            .next().price(110L).size(200L)
            .next().price(115L).size(5000L)
            .next().price(119L).size(5600L);

        encoder.instrumentStatus(InstrumentStatus.CONTINUOUS);
        encoder.source(Source.STREAM);

        return directBuffer;
    }

    protected UnsafeBuffer createNoBidTick() {
        final MessageHeaderEncoder headerEncoder = new MessageHeaderEncoder();
        final BookUpdateEncoder encoder = new BookUpdateEncoder();

        final ByteBuffer byteBuffer = ByteBuffer.allocateDirect(1024);
        final UnsafeBuffer directBuffer = new UnsafeBuffer(byteBuffer);

        
        encoder.wrapAndApplyHeader(directBuffer, 0, headerEncoder);

        
        encoder.venue(Venue.LME);
        encoder.instrumentId(123L);

        encoder.bidBookCount(0);
                
        encoder.askBookCount(4)
                .next().price(98L).size(501L)
                .next().price(101L).size(200L)
                .next().price(110L).size(5000L)
                .next().price(119L).size(5600L);

        encoder.instrumentStatus(InstrumentStatus.CONTINUOUS);
        encoder.source(Source.STREAM);

        return directBuffer;
    }

    @Test
    public void testDispatchThroughSequencer() throws Exception {

        //create a sample market data tick....
        send(createTick());

         // I added these assertions to check that the algo creates a BUY order at the best bid price and quantity
        assertEquals(1, container.getState().getChildOrders().size());

       
        var childOrder = container.getState().getActiveChildOrders().get(0);

        assertEquals(Side.BUY, childOrder.getSide());
        assertEquals(98, childOrder.getPrice());
        assertEquals(100, childOrder.getQuantity());

        // This second market-data scenario changes the best bid from 98 to 95
        send(createTick2());
        
        // I added a second market-data scenario to check that the algo cancels the old order and creates a new one when the best bid changes
        assertEquals(1, container.getState().getActiveChildOrders().size()); 

        var newChildOrder = container.getState().getActiveChildOrders().get(0);

        assertEquals(Side.BUY, newChildOrder.getSide());
        assertEquals(95, newChildOrder.getPrice());
        assertEquals(100, newChildOrder.getQuantity());

    }

    @Test
    public void testMaximumOrderQuantity() throws Exception {

        send(createLargeQuantityTick());

        // I added this assertion to check that theb algo limits the order quantity
        // when the best bid has more quantity available than the maximum order size
        var childOrder = container.getState().getActiveChildOrders().get(0);

        assertEquals(98, childOrder.getPrice());
        assertEquals(100, childOrder.getQuantity());
    }


    @Test
    public void testNoBidDoesNotCreateOrder() throws Exception {

        send(createNoBidTick());

        assertEquals(0, container.getState().getChildOrders().size());
       
    }
}
