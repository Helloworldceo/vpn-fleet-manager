package com.vpnfleet.integration;

import com.vpnfleet.dto.AuthResponse;
import com.vpnfleet.dto.ClientResponse;
import com.vpnfleet.dto.IssueClientRequest;
import com.vpnfleet.dto.LoginRequest;
import com.vpnfleet.dto.RegisterRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proves the Redisson distributed lock in VpnClientService actually does its
 * job: fires N concurrent issuance requests at a node with capacity 1 and
 * asserts exactly one succeeds. Without the lock, a straightforward
 * "read count, check capacity, write" would let multiple requests interleave
 * between the read and the write and over-allocate the node.
 */
class VpnClientConcurrencyTest extends AbstractIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static final int CONCURRENT_REQUESTS = 10;

    @Test
    void onlyOneOfManyConcurrentRequestsSucceedsAgainstACapacityOneNode() throws InterruptedException {
        long nodeId = jdbcTemplate.queryForObject(
                "INSERT INTO vpn_nodes (name, host, region, protocol, capacity, active_client_count, status) " +
                        "VALUES ('concurrency-test-node', 'test.invalid', 'test-region', 'VLESS_REALITY', 1, 0, 'ACTIVE') " +
                        "RETURNING id",
                Long.class
        );

        List<String> tokens = IntStream.range(0, CONCURRENT_REQUESTS)
                .mapToObj(i -> registerAndLogin("concurrency-" + System.nanoTime() + "-" + i + "@example.com"))
                .collect(Collectors.toList());

        ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_REQUESTS);
        CountDownLatch startLatch = new CountDownLatch(1);
        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger conflictCount = new AtomicInteger();

        List<Future<Void>> futures = tokens.stream().map(token -> executor.submit((Callable<Void>) () -> {
            startLatch.await();
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(token);
            ResponseEntity<ClientResponse> response = rest.exchange(
                    baseUrl() + "/api/clients",
                    HttpMethod.POST,
                    new HttpEntity<>(new IssueClientRequest(nodeId), headers),
                    ClientResponse.class
            );
            if (response.getStatusCode() == HttpStatus.CREATED) {
                successCount.incrementAndGet();
            } else if (response.getStatusCode() == HttpStatus.CONFLICT) {
                conflictCount.incrementAndGet();
            }
            return null;
        })).collect(Collectors.toList());

        startLatch.countDown();
        for (Future<Void> f : futures) {
            try {
                f.get(30, TimeUnit.SECONDS);
            } catch (ExecutionException | TimeoutException e) {
                throw new RuntimeException(e);
            }
        }
        executor.shutdown();

        assertThat(successCount.get()).isEqualTo(1);
        assertThat(conflictCount.get()).isEqualTo(CONCURRENT_REQUESTS - 1);

        Integer finalActiveCount = jdbcTemplate.queryForObject(
                "SELECT active_client_count FROM vpn_nodes WHERE id = ?", Integer.class, nodeId
        );
        assertThat(finalActiveCount).isEqualTo(1);
    }

    private String registerAndLogin(String email) {
        rest.postForEntity(baseUrl() + "/api/auth/register", new RegisterRequest(email, "password123"), AuthResponse.class);
        ResponseEntity<AuthResponse> login = rest.postForEntity(
                baseUrl() + "/api/auth/login", new LoginRequest(email, "password123"), AuthResponse.class
        );
        return login.getBody().token();
    }
}
