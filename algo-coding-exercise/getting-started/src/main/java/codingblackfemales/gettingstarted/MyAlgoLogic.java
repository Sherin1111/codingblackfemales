package codingblackfemales.gettingstarted;

import codingblackfemales.action.Action;
import codingblackfemales.action.CancelChildOrder;
import codingblackfemales.action.CreateChildOrder;
import codingblackfemales.action.NoAction;
import codingblackfemales.algo.AlgoLogic;
import codingblackfemales.sotw.SimpleAlgoState;
import codingblackfemales.sotw.marketdata.BidLevel;
import codingblackfemales.util.Util;
import messages.order.Side;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MyAlgoLogic implements AlgoLogic {

    private static final Logger logger = LoggerFactory.getLogger(MyAlgoLogic.class);

    private static final long MAX_ORDER_QUANTITY = 100;

    @Override
    public Action evaluate(SimpleAlgoState state) {


        logger.info("[MYALGO] The state of the order book is:\n {}", Util.orderBookToString(state));

        var totalOrders = state.getChildOrders().size();

        // Safety exit condition to prevent an accidental order creation loop
        if (totalOrders >= 10) {
            return NoAction.NoAction;
        }

        // This checks if there are any active orders
        final var activeOrders = state.getActiveChildOrders();

        //If there is an active order, check whether the best bid has changed
        if (!activeOrders.isEmpty()) {

            final var option = activeOrders.stream().findFirst();

            // In a real trading system, order creation and cancellation may not happen immediately.
            // The market can move while an order is still pending, which could result in multiple
            // outstanding orders if the algo reacts to every market-data update.
            if (option.isPresent()) {
                var childOrder = option.get();
               
                BidLevel level = state.getBidAt(0);
                final long price = level.price;

                // If the best bid hasn't changed, keep the existing order rather than cancelling and recreating it
                if (childOrder.getPrice() == price) {
                    logger.info("No action required: active order already at " + price);
                    return NoAction.NoAction;
                }

                // If the best bid has changed, cancel the existing order. The algo will create a new order at the new price
                logger.info("Cancelling order:" + childOrder);
                return new CancelChildOrder(childOrder);
            }
            else {
                return NoAction.NoAction;
            }
         } else {
            //This gets the current best bid to use for the new order
            BidLevel level = state.getBidAt(0);
            logger.info("Best bid price:" + level.price);
            logger.info("Best bid quantity:" + level.quantity);
            final long price = level.price;
            final long quantity = Math.min(level.quantity, MAX_ORDER_QUANTITY);
            logger.info("Adding order for" + quantity + "@" + price);
            // If there is no active order, create a BUY at the current best bid,
            // limited to the maximum order quantity.
            return new CreateChildOrder(Side.BUY, quantity, price);

         }

    
    }
}
