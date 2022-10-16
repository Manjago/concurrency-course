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

        final List<CompletableFuture<Double>> completableFutures =
                shopIds.stream().map(shopId -> getPriceAsync(itemId, shopId)).collect(Collectors.toList());

        final CompletableFuture<Void> allFuturesResult = CompletableFuture
                .allOf(completableFutures.toArray(new CompletableFuture[completableFutures.size()]));

        try {
            allFuturesResult.get(3000 - 20, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            System.out.println("Таймаут случился " + e.getMessage());
        } catch (Exception e) {
            System.out.println("Магазин ниасилил " + e.getMessage());
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
