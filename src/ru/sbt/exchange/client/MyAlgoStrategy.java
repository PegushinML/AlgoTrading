package ru.sbt.exchange.client;


import ru.sbt.exchange.domain.*;
import ru.sbt.exchange.domain.instrument.*;

/**
 * Created by Maxim on 11/29/2016.
 */
public class MyAlgoStrategy implements AlgoStrategy {

    private Portfolio myPortfolio;
    private FairPrices fairPrices;
    private Order LastZeroOrder;
    private Order LastFixedOrder;
    private Order LastFloatOrder;


    @Override
    public void onEvent(ExchangeEvent exchangeEvent, Broker broker) {
        if(exchangeEvent.getExchangeEventType() == ExchangeEventType.STRATEGY_START){
            //TODO add smth
        }
        if (exchangeEvent.getExchangeEventType() == ExchangeEventType.ORDER_NEW) {
            onOrderNewEvent(exchangeEvent, broker);
        }
        if (exchangeEvent.getExchangeEventType() == ExchangeEventType.NEW_PERIOD_START) {
            onNewPeriodStart(broker);
        }
        if(exchangeEvent.getExchangeEventType() == ExchangeEventType.ORDER_EVICT) {
            onOrderEviction(exchangeEvent, broker);
        }
    }

    private void onOrderEviction(ExchangeEvent exchangeEvent, Broker broker) {
        //do nothing...
    }

    private void onNewPeriodStart(Broker broker) {
        //just calculate fair prices and make some offers
        fairPrices = new FairPrices(broker, 7.5);
        myPortfolio = broker.getMyPortfolio();
        LastZeroOrder = Order.sell(Instruments.zeroCouponBond())
                .withPrice(fairPrices.getZeroCouponPrice() + 10)
                .withQuantity(myPortfolio.getCountByInstrument()
                        .get(Instruments.zeroCouponBond()) / 2)
                .order();
        LastFloatOrder =  Order.sell(Instruments.floatingCouponBond())
                .withPrice(fairPrices.getFloatCouponPrice() + 10)
                .withQuantity(myPortfolio.getCountByInstrument()
                        .get(Instruments.floatingCouponBond()) / 2)
                .order();
        LastFixedOrder =  Order.sell(Instruments.fixedCouponBond())
                .withPrice(fairPrices.getFixedCouponPrice() + 10)
                .withQuantity(myPortfolio.getCountByInstrument()
                        .get(Instruments.fixedCouponBond()) / 2)
                .order();
    }

    private void onOrderNewEvent(ExchangeEvent exchangeEvent, Broker broker) {
        Order order = exchangeEvent.getOrder();

        antiCheatOffer(exchangeEvent, broker);

        if (order.getDirection() == Direction.SELL)
            if (order.getPrice() < fairPrices.getPriceByInstrument(order.getInstrument())) {
                Order buyOrder = order.opposite().withQuantity(order.getQuantity());
                broker.addOrder(buyOrder);
                //TODO work on case, where opponent has lower price
        }


        if(order.getDirection() == Direction.BUY) {
            if (order.getPrice() > fairPrices.getPriceByInstrument(order.getInstrument()) &&
                    myPortfolio.getCountByInstrument().get(order.getInstrument()).intValue() > order.getQuantity()) {
                Order sellOrder = order.opposite().withQuantity(order.getQuantity());
                broker.addOrder(sellOrder);
            } else if (order.getPrice() > getLastOrderPriceByInstrument(order.getInstrument()) + 0.1) {

                Order newOrder = order.withPrice(order.getPrice() + 0.1);
                broker.cancelOrdersByInstrument(newOrder.getInstrument());
                broker.addOrder(newOrder);
                setLastOrder(newOrder);
            }
        }
    }

    private void setLastOrder(Order newOrder) {
        if(newOrder.getInstrument().equals(Instruments.zeroCouponBond()))
            LastZeroOrder = newOrder;
        if(newOrder.getInstrument().equals(Instruments.fixedCouponBond()))
            LastFixedOrder = newOrder;
        if(newOrder.getInstrument().equals(Instruments.floatingCouponBond()))
            LastFloatOrder = newOrder;

    }



    private boolean checkIfNoShort(Order order) {
        return true;
    }

    private double getLastOrderPriceByInstrument(Instrument instrument) {

        if(instrument.equals(Instruments.zeroCouponBond()))
            return LastZeroOrder.getPrice();
        if(instrument.equals(Instruments.fixedCouponBond()))
            return LastFixedOrder.getPrice();
        if(instrument.equals(Instruments.floatingCouponBond()))
            return LastFloatOrder.getPrice();
        return 100;

    }

    private void antiCheatOffer(ExchangeEvent exchangeEvent, Broker broker) {
        Order order = exchangeEvent.getOrder();
        Order middleOrder = broker.getTopOrders(order.getInstrument()).getSellOrders().get(4);
        if(order.getDirection() == Direction.SELL)
        if(order.getPrice() > middleOrder.getPrice() + 20.0)
            broker.addOrder(order.withPrice(order.getPrice() - 5.0));
    }

}
