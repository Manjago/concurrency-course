package course.concurrency.m2_async.cf.min_price;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

public class PriceAggregator {

    private PriceRetriever priceRetriever = new PriceRetriever();
    private Collection<Long> shopIds = Set.of(10L, 45L, 66L, 345L, 234L, 333L, 67L, 123L, 768L);

    public void setPriceRetriever(PriceRetriever priceRetriever) {
        this.priceRetriever = priceRetriever;
    }

    public void setShops(Collection<Long> shopIds) {
        this.shopIds = shopIds;
    }

    public double getMinPrice(long itemId) {

        final int SLA = 3000;
        final int LAME_EMPIRICAL_CORRECTION = 20;

        final List<CompletableFuture<Double>> completableFutures =
                shopIds.stream().map(shopId -> getPriceAsync(itemId, shopId)).collect(Collectors.toList());

        final CompletableFuture<Void> allCompletableFuturesResult = CompletableFuture
                .allOf(completableFutures.toArray(new CompletableFuture[0]));

        try {
            /*
            Вот здесь какое-то сырое место, за которое мне стыдно.
            "Метод getPrice должен выполняться не более трёх секунд" -
            и вот я влепил какую-то эмпирическую константу, потому что есть
            же ще еще накладные расходы чисто на исполнение кода, помимо таймаута к CompletableFuture

            Ну ладно, на моем ноутбуке этот прокатывает, а на других?
            Не люблю я завязку на таймауты - зависит от техники.

            Будет считать, что это типа выкатили на прод, магическую константу засунули
            в настройки и задокументировали. Группа эксплуатации предупреждена,
            что константу надо подкручивать, и на всё это навесили мониторинг и
            алерты с рекомендациями о том, что ВОЗМОЖНО придется это коррекцию подкрутить
             */
            allCompletableFuturesResult.get(SLA - LAME_EMPIRICAL_CORRECTION, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            //  Таймаут случился
        } catch (Exception e) {
            // Магазин ниасилил и сам сломался - можно было бы эти 2 catch схлопнуть в один, но я их все-таки разведу
        }

        final List<Double> resultList = completableFutures
                .stream()
                .filter(future -> future.isDone() && !future.isCompletedExceptionally())
                .map(CompletableFuture::join)
                .collect(Collectors.<Double>toList());

        if (!resultList.isEmpty()) {
            return Collections.min(resultList);
        } else {
            return Double.NaN;
        }
    }


    private CompletableFuture<Double> getPriceAsync(long itemId, long shopId) {
        return CompletableFuture.supplyAsync(() -> priceRetriever.getPrice(itemId, shopId));
    }

}
