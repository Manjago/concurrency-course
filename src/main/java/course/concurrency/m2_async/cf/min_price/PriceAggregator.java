package course.concurrency.m2_async.cf.min_price;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

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

        final int CORRECTED_SLA = 3000 - 20;

        final var completableFutures =
                shopIds.stream().map(shopId -> CompletableFuture.supplyAsync(() -> priceRetriever.getPrice(itemId, shopId))).toArray(CompletableFuture[]::new);

        try {
            CompletableFuture
                    .allOf(completableFutures).get(CORRECTED_SLA, TimeUnit.MILLISECONDS);
        } catch (Exception ignored) {
            // do nothing
        }

        return Stream.of(completableFutures)
                .filter(future -> future.isDone() && !future.isCompletedExceptionally())
                .map(CompletableFuture::join)
                .map(object -> (Double) object)
                .min(Double::compareTo).orElse(Double.NaN);

    }


}
