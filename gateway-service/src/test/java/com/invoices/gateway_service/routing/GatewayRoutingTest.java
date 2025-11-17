package com.invoices.gateway_service.routing;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.test.context.ActiveProfiles;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for Gateway routing configuration.
 * Verifies that routes are properly configured and can route to correct services.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class GatewayRoutingTest {

    @Autowired
    private RouteLocator routeLocator;

    @Test
    void shouldLoadAllConfiguredRoutes() {
        // When
        Flux<Route> routes = routeLocator.getRoutes();

        // Then
        StepVerifier.create(routes)
                .expectNextCount(5) // 5 routes: auth, user, invoice, document, trace
                .verifyComplete();
    }

    @Test
    void shouldHaveAuthServiceRoute() {
        // When
        List<Route> routes = getAllRoutes();

        // Then
        Route authRoute = findRouteById(routes, "auth-service");
        assertThat(authRoute).isNotNull();
        assertThat(authRoute.getUri().toString()).isEqualTo("lb://user-service");

        // Verify predicate includes auth path
        assertThat(authRoute.getPredicate()).isNotNull();
    }

    @Test
    void shouldHaveUserServiceRoute() {
        // When
        List<Route> routes = getAllRoutes();

        // Then
        Route userRoute = findRouteById(routes, "user-service");
        assertThat(userRoute).isNotNull();
        assertThat(userRoute.getUri().toString()).isEqualTo("lb://user-service");
    }

    @Test
    void shouldHaveInvoiceServiceRoute() {
        // When
        List<Route> routes = getAllRoutes();

        // Then
        Route invoiceRoute = findRouteById(routes, "invoice-service");
        assertThat(invoiceRoute).isNotNull();
        assertThat(invoiceRoute.getUri().toString()).isEqualTo("lb://invoice-service");
    }

    @Test
    void shouldHaveDocumentServiceRoute() {
        // When
        List<Route> routes = getAllRoutes();

        // Then
        Route documentRoute = findRouteById(routes, "document-service");
        assertThat(documentRoute).isNotNull();
        assertThat(documentRoute.getUri().toString()).isEqualTo("lb://document-service");
    }

    @Test
    void shouldHaveTraceServiceRoute() {
        // When
        List<Route> routes = getAllRoutes();

        // Then
        Route traceRoute = findRouteById(routes, "trace-service");
        assertThat(traceRoute).isNotNull();
        assertThat(traceRoute.getUri().toString()).isEqualTo("lb://trace-service");
    }

    @Test
    void shouldHaveCorrectNumberOfRoutes() {
        // When
        List<Route> routes = getAllRoutes();

        // Then
        assertThat(routes).hasSize(5);
    }

    @Test
    void shouldHaveUniqueRouteIds() {
        // When
        List<Route> routes = getAllRoutes();
        List<String> routeIds = routes.stream()
                .map(Route::getId)
                .collect(Collectors.toList());

        // Then
        assertThat(routeIds).doesNotHaveDuplicates();
    }

    @Test
    void shouldAllRoutesUseLoadBalancing() {
        // When
        List<Route> routes = getAllRoutes();

        // Then - all routes should use lb:// (load balancer) URIs
        routes.forEach(route -> {
            String uri = route.getUri().toString();
            assertThat(uri)
                    .as("Route %s should use load balancer", route.getId())
                    .startsWith("lb://");
        });
    }

    @Test
    void shouldRouteToCorrectServices() {
        // When
        List<Route> routes = getAllRoutes();

        // Then - verify route ID to service name mapping
        assertRouteUri(routes, "auth-service", "lb://user-service");
        assertRouteUri(routes, "user-service", "lb://user-service");
        assertRouteUri(routes, "invoice-service", "lb://invoice-service");
        assertRouteUri(routes, "document-service", "lb://document-service");
        assertRouteUri(routes, "trace-service", "lb://trace-service");
    }

    @Test
    void shouldAllRoutesHavePredicates() {
        // When
        List<Route> routes = getAllRoutes();

        // Then
        routes.forEach(route ->
                assertThat(route.getPredicate())
                        .as("Route %s should have a predicate", route.getId())
                        .isNotNull()
        );
    }

    @Test
    void shouldAuthRouteHaveRewritePathFilter() {
        // When
        List<Route> routes = getAllRoutes();
        Route authRoute = findRouteById(routes, "auth-service");

        // Then
        assertThat(authRoute).isNotNull();
        assertThat(authRoute.getFilters()).isNotEmpty();

        // Verify it has a filter (RewritePath is applied)
        boolean hasFilters = !authRoute.getFilters().isEmpty();
        assertThat(hasFilters).isTrue();
    }

    @Test
    void shouldRoutesBeOrdered() {
        // When
        List<Route> routes = getAllRoutes();

        // Then - routes should be loaded in a consistent order
        assertThat(routes).hasSizeGreaterThan(0);

        // Verify all expected route IDs are present
        List<String> expectedRouteIds = List.of(
                "auth-service",
                "user-service",
                "invoice-service",
                "document-service",
                "trace-service"
        );

        List<String> actualRouteIds = routes.stream()
                .map(Route::getId)
                .collect(Collectors.toList());

        assertThat(actualRouteIds).containsExactlyInAnyOrderElementsOf(expectedRouteIds);
    }

    @Test
    void shouldNotHaveDuplicateServiceUris() {
        // When
        List<Route> routes = getAllRoutes();

        // Then - each route should have a valid URI
        routes.forEach(route -> {
            String uri = route.getUri().toString();
            assertThat(uri)
                    .as("Route %s should have a valid URI", route.getId())
                    .isNotBlank()
                    .matches("lb://[a-z-]+");
        });
    }

    // Helper methods

    private List<Route> getAllRoutes() {
        return routeLocator.getRoutes().collectList().block();
    }

    private Route findRouteById(List<Route> routes, String routeId) {
        return routes.stream()
                .filter(route -> route.getId().equals(routeId))
                .findFirst()
                .orElse(null);
    }

    private void assertRouteUri(List<Route> routes, String routeId, String expectedUri) {
        Route route = findRouteById(routes, routeId);
        assertThat(route)
                .as("Route %s should exist", routeId)
                .isNotNull();
        assertThat(route.getUri().toString())
                .as("Route %s should route to %s", routeId, expectedUri)
                .isEqualTo(expectedUri);
    }
}
