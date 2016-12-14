package ru.sbt.exchange.client;


import ru.sbt.exchange.domain.*;
import ru.sbt.exchange.domain.instrument.*;

/**
 * Created by Maxim on 11/29/2016.
 */
public class MyAlgoStrategy implements AlgoStrategy {

    private Portfolio myPortfolio;

    @Override
    public void onEvent(ExchangeEvent exchangeEvent, Broker broker) {
        if (exchangeEvent.getExchangeEventType() == ExchangeEventType.ORDER_NEW) {
            onOrderNewEvent(exchangeEvent, broker);
        }
        if (exchangeEvent.getExchangeEventType() == ExchangeEventType.NEW_PERIOD_START) {
            onNewPeriodStart(broker);
        }
    }

    private void onNewPeriodStart(Broker broker) {
        myPortfolio = broker.getMyPortfolio();
        int count = myPortfolio.getCountByInstrument().get(Instruments.zeroCouponBond());
        if (count > 0) {
            TopOrders topOrders = broker.getTopOrders(Instruments.zeroCouponBond());
            double price = topOrders.getSellOrders().isEmpty() ? 50 : topOrders.getSellOrders().get(0).getPrice() - 0.1;
            broker.addOrder(Order.sell(Instruments.zeroCouponBond()).withPrice(price).withQuantity(count).order());
        }
    }

    private void onOrderNewEvent(ExchangeEvent exchangeEvent, Broker broker) {
        Order order = exchangeEvent.getOrder();
        if (order.getInstrument().equals(Instruments.fixedCouponBond())
                && order.getDirection() == Direction.SELL
                && order.getPrice() < 50) {
            Order buyOrder = order.opposite().withQuantity(order.getQuantity() / 2);
            broker.addOrder(buyOrder);
        }
    }

    private void antiCheat(Broker broker){

    }
}
