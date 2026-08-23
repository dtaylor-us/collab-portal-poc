package com.miso.planning.dpp;

import io.camunda.client.annotation.Deployment;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@Deployment(resources = {"classpath:workflows/dpp-result-review.bpmn", "classpath:workflows/forms/*.form"})
public class DppCollaborationApiApplication {

  public static void main(String[] args) {
    SpringApplication.run(DppCollaborationApiApplication.class, args);
  }
}
