package ru.sbt.exchange.client;
import ru.sbt.exchange.domain.ExchangeEvent;

public interface AlgoStrategy {
    void onEvent(ExchangeEvent exchangeEvent, Broker broker);
}
