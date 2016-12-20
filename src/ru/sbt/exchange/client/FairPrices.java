package ru.sbt.exchange.client;

import ru.sbt.exchange.domain.PeriodInfo;
import ru.sbt.exchange.domain.Portfolio;
import ru.sbt.exchange.domain.instrument.Bond;
import ru.sbt.exchange.domain.instrument.Instrument;
import ru.sbt.exchange.domain.instrument.Instruments;

import java.util.concurrent.BrokenBarrierException;

/**
 * Created by Vladimir on 20.12.2016.
 */
public class FairPrices {
    private Broker broker;
    private double minimalPercent;
    private double zeroCouponPrice;
    private double fixedCouponPrice;

    private double floatCouponPriceSell;
    private double floatCouponPriceBuy;
    private Portfolio myPortfolio;

    public double getZeroCouponPrice() {
        return zeroCouponPrice;
    }

    public double getFixedCouponPrice() {
        return fixedCouponPrice;
    }

    public double getFloatCouponPriceSell() {
        return floatCouponPriceSell;
    }

    public double getFloatCouponPriceBuy() {
        return floatCouponPriceBuy;
    }

    public FairPrices(Broker broker, double minimalPercent) {
        this.broker = broker;
        this.minimalPercent = minimalPercent;
        calculate();
    }

    public void calculate() {
        PeriodInfo info = broker.getPeriodInfo();
        myPortfolio = broker.getMyPortfolio();
        Double r = myPortfolio.getPeriodInterestRate();

        zeroCouponPrice = 100.0/ Math.pow((1.0 + r/100.0),
                 info.getEndPeriodNumber() - info.getCurrentPeriodNumber() + 1);
        fixedCouponPrice = zeroCouponPrice;
        for(int i = info.getCurrentPeriodNumber(); i < info.getEndPeriodNumber(); i++)
        fixedCouponPrice += 10.0 / Math.pow((1.0 + r/100.0),
                info.getEndPeriodNumber() - i + 1);
        floatCouponPriceBuy = zeroCouponPrice;
        for(int i = info.getCurrentPeriodNumber(); i < info.getEndPeriodNumber(); i++)
            floatCouponPriceBuy += minimalPercent / Math.pow((1.0 + r/100.0),
                    info.getEndPeriodNumber() - i + 1);
        Bond.Coupon coupon = Instruments.floatingCouponBond().getCouponInPercents();
        floatCouponPriceSell = zeroCouponPrice;
        for(int i = info.getCurrentPeriodNumber(); i < info.getEndPeriodNumber(); i++)
            floatCouponPriceSell += ( coupon.getMax() -minimalPercent  + coupon.getMin()) / Math.pow((1.0 + r/100.0),
                    info.getEndPeriodNumber() - i + 1);
        discountPrices();
    }


    public void discountPrices(){
        Double fee = myPortfolio.getPeriodInterestRate();
        zeroCouponPrice = zeroCouponPrice*(1.0 + fee/100.0);
        fixedCouponPrice = fixedCouponPrice*(1.0 + fee/100.0);
        floatCouponPriceSell = floatCouponPriceSell*(1.0 + fee/100.0);
        floatCouponPriceBuy = floatCouponPriceBuy *(1.0 + fee/100.0);
    }

    public double getPriceByInstrument(Instrument instrument, boolean isSell) {
        if(instrument.equals(Instruments.zeroCouponBond()))
            return zeroCouponPrice;
        if(instrument.equals(Instruments.fixedCouponBond()))
            return fixedCouponPrice;
        if(instrument.equals(Instruments.floatingCouponBond()) && isSell)
            return floatCouponPriceSell;
        if(instrument.equals(Instruments.floatingCouponBond()) && !isSell)
            return floatCouponPriceBuy;
        return 100;
    }
}
