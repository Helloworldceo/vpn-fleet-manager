package com.vpnfleet.controller;

import com.vpnfleet.dto.ClientResponse;
import com.vpnfleet.dto.IssueClientRequest;
import com.vpnfleet.security.CurrentUser;
import com.vpnfleet.service.VpnClientService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/clients")
@RequiredArgsConstructor
public class ClientController {

    private final VpnClientService vpnClientService;

    @PostMapping
    public ResponseEntity<ClientResponse> issueClient(@RequestBody(required = false) IssueClientRequest request) {
        Long nodeId = request == null ? null : request.nodeId();
        var response = vpnClientService.issueClient(CurrentUser.get().id(), nodeId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/me")
    public List<ClientResponse> myClients() {
        return vpnClientService.listForUser(CurrentUser.get().id());
    }

    @DeleteMapping("/{id}")
    public ClientResponse revoke(@PathVariable Long id) {
        var user = CurrentUser.get();
        boolean isAdmin = user.role().name().equals("ADMIN");
        return vpnClientService.revokeClient(user.id(), isAdmin, id);
    }
}
