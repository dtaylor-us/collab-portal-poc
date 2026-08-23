package com.miso.planning.notification;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "camunda.client.enabled=false")
class PlanningNotificationWorkerApplicationTest {

  @Test
  void contextLoadsWithoutCamunda() {}
}
