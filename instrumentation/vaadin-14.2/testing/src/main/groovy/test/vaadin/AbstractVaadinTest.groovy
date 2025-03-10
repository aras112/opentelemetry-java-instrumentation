/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package test.vaadin

import com.vaadin.flow.server.Version
import com.vaadin.flow.spring.annotation.EnableVaadin
import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.instrumentation.test.AgentInstrumentationSpecification
import io.opentelemetry.instrumentation.test.asserts.TraceAssert
import io.opentelemetry.instrumentation.test.base.HttpServerTestTrait
import org.openqa.selenium.firefox.FirefoxOptions
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.ConfigurableApplicationContext
import org.testcontainers.Testcontainers
import org.testcontainers.containers.BrowserWebDriverContainer
import org.testcontainers.containers.output.Slf4jLogConsumer
import spock.lang.Shared

import java.util.concurrent.TimeUnit

abstract class AbstractVaadinTest extends AgentInstrumentationSpecification implements HttpServerTestTrait<ConfigurableApplicationContext> {
  private static final Logger logger = LoggerFactory.getLogger(AbstractVaadinTest)

  @Shared
  BrowserWebDriverContainer<?> browser

  @SpringBootApplication
  @EnableVaadin("test.vaadin")
  static class TestApplication {
    static ConfigurableApplicationContext start(int port, String contextPath) {
      def app = new SpringApplication(TestApplication)
      app.setDefaultProperties([
        "server.port"                 : port,
        "server.servlet.contextPath"  : contextPath,
        "server.error.include-message": "always"])
      def context = app.run()
      return context
    }
  }

  def setupSpec() {
    Testcontainers.exposeHostPorts(port)

    browser = new BrowserWebDriverContainer<>()
      .withCapabilities(new FirefoxOptions())
      .withLogConsumer(new Slf4jLogConsumer(logger))
    browser.start()

    address = new URI("http://host.testcontainers.internal:$port" + getContextPath() + "/")
  }

  def cleanupSpec() {
    browser?.stop()
  }

  @Override
  ConfigurableApplicationContext startServer(int port) {
    // set directory for files generated by vaadin development mode
    // by default these go to project root
    System.setProperty("vaadin.project.basedir", new File("build/vaadin-" + Version.getFullVersion()).getAbsolutePath())
    return TestApplication.start(port, getContextPath())
  }

  @Override
  void stopServer(ConfigurableApplicationContext ctx) {
    ctx.close()
  }

  @Override
  String getContextPath() {
    return "/xyz"
  }

  def waitForStart(driver) {
    // In development mode ui javascript is compiled when application starts
    // this involves downloading and installing npm and a bunch of packages
    // and running webpack. Wait until all of this is done before starting test.
    driver.manage().timeouts().implicitlyWait(3, TimeUnit.MINUTES)
    driver.get(address.resolve("main").toString())
    // wait for page to load
    driver.findElementById("main.label")
    // clear traces so test would start from clean state
    clearExportedData()

    driver.manage().timeouts().implicitlyWait(30, TimeUnit.SECONDS)
  }

  def getWebDriver() {
    return browser.getWebDriver()
  }

  abstract List<String> getRequestHandlers()

  def getRequestHandlers(String lastHandler) {
    def handlers = getRequestHandlers()
    int index = handlers.indexOf(lastHandler)
    if (index == -1) {
      throw new IllegalStateException("unexpected handler " + lastHandler)
    }
    return handlers.subList(0, index + 1)
  }

  abstract void assertFirstRequest()

  abstract void assertButtonClick()

  static serverSpan(TraceAssert trace, int index, String spanName) {
    trace.span(index) {
      hasNoParent()

      name spanName
      kind SpanKind.SERVER
    }
  }

  def "test vaadin"() {
    setup:
    def driver = getWebDriver()
    waitForStart(driver)

    // fetch the test page
    driver.get(address.resolve("main").toString())

    expect:
    // wait for page to load
    "Main view" == driver.findElementById("main.label").getText()
    assertFirstRequest()

    clearExportedData()

    when:
    // click a button to trigger calling java code in MainView
    driver.findElementById("main.button").click()

    then:
    // wait for page to load
    "Other view" == driver.findElementById("other.label").getText()
    assertButtonClick()

    cleanup:
    driver.close()
  }
}
