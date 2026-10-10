package cn.keking.config;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;

@AutoConfigureTestRestTemplate
@SpringBootTest(classes = ActuatorExposureSecurityTests.ManagementApp.class,
        properties = "spring.config.name=test",
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ActuatorOverrideExposureSecurityTests extends ActuatorExposureSecurityTests {
}
