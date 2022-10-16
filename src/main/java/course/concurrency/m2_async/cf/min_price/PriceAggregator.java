package course.concurrency.m2_async.cf.min_price;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
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

        final int CORRECTED_SLA = 3000 - 20;

        final var completableFutures =
                shopIds.stream().map(shopId -> CompletableFuture.supplyAsync(() -> priceRetriever.getPrice(itemId, shopId))).collect(Collectors.toList());

        try {
            CompletableFuture
                    .allOf(completableFutures.toArray(new CompletableFuture[0])).get(CORRECTED_SLA, TimeUnit.MILLISECONDS);
        } catch (Exception ignored) {
            // do nothing
        }

        return completableFutures.stream()
                .filter(future -> future.isDone() && !future.isCompletedExceptionally())
                .mapToDouble(CompletableFuture::join)
                .min().orElse(Double.NaN);
    }


}
