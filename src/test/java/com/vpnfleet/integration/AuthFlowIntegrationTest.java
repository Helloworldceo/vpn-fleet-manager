package com.vpnfleet.integration;

import com.vpnfleet.dto.AuthResponse;
import com.vpnfleet.dto.ClientResponse;
import com.vpnfleet.dto.LoginRequest;
import com.vpnfleet.dto.NodeResponse;
import com.vpnfleet.dto.RegisterRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

class AuthFlowIntegrationTest extends AbstractIntegrationTest {

    @Test
    void registerLoginIssueListAndRevoke_endToEnd() {
        String email = "flow-" + System.nanoTime() + "@example.com";

        ResponseEntity<AuthResponse> registerResponse = rest.postForEntity(
                baseUrl() + "/api/auth/register",
                new RegisterRequest(email, "password123"),
                AuthResponse.class
        );
        assertThat(registerResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(registerResponse.getBody()).isNotNull();

        ResponseEntity<AuthResponse> loginResponse = rest.postForEntity(
                baseUrl() + "/api/auth/login",
                new LoginRequest(email, "password123"),
                AuthResponse.class
        );
        assertThat(loginResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        String token = loginResponse.getBody().token();
        assertThat(token).isNotBlank();

        HttpHeaders authHeaders = new HttpHeaders();
        authHeaders.setBearerAuth(token);

        ResponseEntity<NodeResponse[]> nodesResponse = rest.exchange(
                baseUrl() + "/api/nodes", HttpMethod.GET, new HttpEntity<>(authHeaders), NodeResponse[].class
        );
        assertThat(nodesResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(nodesResponse.getBody()).isNotEmpty();

        ResponseEntity<ClientResponse> issueResponse = rest.exchange(
                baseUrl() + "/api/clients", HttpMethod.POST, new HttpEntity<>(null, authHeaders), ClientResponse.class
        );
        assertThat(issueResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        ClientResponse issued = issueResponse.getBody();
        assertThat(issued).isNotNull();
        assertThat(issued.config()).startsWith("vless://");
        assertThat(issued.status()).isEqualTo("ACTIVE");

        ResponseEntity<ClientResponse[]> myClientsResponse = rest.exchange(
                baseUrl() + "/api/clients/me", HttpMethod.GET, new HttpEntity<>(authHeaders), ClientResponse[].class
        );
        assertThat(myClientsResponse.getBody()).extracting(ClientResponse::id).contains(issued.id());

        ResponseEntity<ClientResponse> revokeResponse = rest.exchange(
                baseUrl() + "/api/clients/" + issued.id(), HttpMethod.DELETE, new HttpEntity<>(authHeaders), ClientResponse.class
        );
        assertThat(revokeResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(revokeResponse.getBody().status()).isEqualTo("REVOKED");
    }
}
