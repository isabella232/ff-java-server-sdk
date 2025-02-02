package io.harness.cf.client.api;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.google.common.collect.Maps;
import io.harness.cf.client.connector.Connector;
import io.harness.cf.client.dto.Target;
import io.harness.cf.model.FeatureConfig;
import io.harness.cf.model.Metrics;
import io.harness.cf.model.Variation;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

@Slf4j
public class MetricsProcessorTest implements MetricsCallback {
  final int BUFFER_SIZE = 10;
  @Mock private Connector connector;

  private MetricsProcessor metricsProcessor;

  @BeforeMethod
  public void setup() {
    MockitoAnnotations.openMocks(this);
    metricsProcessor =
        Mockito.spy(
            new MetricsProcessor(
                connector, BaseConfig.builder().bufferSize(BUFFER_SIZE).build(), this));
  }

  @Test
  public void testPushToQueue() throws InterruptedException {
    ExecutorService WORKER_THREAD_POOL = Executors.newFixedThreadPool(BUFFER_SIZE);
    CountDownLatch latch = new CountDownLatch(BUFFER_SIZE);
    Target target = Target.builder().identifier("harness").build();
    FeatureConfig feature = FeatureConfig.builder().feature("bool-flag").build();
    Variation variation = Variation.builder().identifier("true").value("true").build();

    for (int i = 1; i <= BUFFER_SIZE; i++) {
      WORKER_THREAD_POOL.submit(
          () -> {
            for (int j = 1; j <= BUFFER_SIZE; j++) {
              metricsProcessor.pushToQueue(target, feature, variation);
            }
            latch.countDown();
          });
    }
    latch.await();

    verify(metricsProcessor, times(BUFFER_SIZE - 1)).runOneIteration();
  }

  @Override
  public void onMetricsReady() {
    // not used
  }

  @Override
  public void onMetricsError(@NonNull String error) {
    // not used
  }

  @Override
  public void onMetricsFailure() {
    // not used
  }

  @Test
  public void testPrepareSummaryMetricsBody() {
    Target target = Target.builder().identifier("harness").build();
    FeatureConfig feature = FeatureConfig.builder().feature("bool-flag").build();
    Variation variation = Variation.builder().identifier("true").value("true").build();
    MetricEvent event = new MetricEvent(feature, target, variation);

    Map<MetricEvent, Integer> map = Maps.newHashMap();
    map.put(event, 6);

    Metrics metrics = metricsProcessor.prepareSummaryMetricsBody(map);

    assert metrics.getTargetData() != null;
    assert metrics.getTargetData().get(1).getIdentifier().equals(target.getIdentifier());
  }
}
