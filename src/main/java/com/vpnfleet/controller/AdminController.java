package com.vpnfleet.controller;

import com.vpnfleet.dto.ClientResponse;
import com.vpnfleet.dto.CreateNodeRequest;
import com.vpnfleet.dto.NodeResponse;
import com.vpnfleet.dto.UpdateNodeStatusRequest;
import com.vpnfleet.entity.NodeStatus;
import com.vpnfleet.service.NodeService;
import com.vpnfleet.service.VpnClientService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Everything under here requires ROLE_ADMIN (enforced in SecurityConfig). */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final NodeService nodeService;
    private final VpnClientService vpnClientService;

    @GetMapping("/nodes")
    public List<NodeResponse> listNodes() {
        return nodeService.listAll();
    }

    @PostMapping("/nodes")
    public ResponseEntity<NodeResponse> createNode(@Valid @RequestBody CreateNodeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(nodeService.createNode(request));
    }

    @PatchMapping("/nodes/{id}")
    public NodeResponse updateNodeStatus(@PathVariable Long id, @Valid @RequestBody UpdateNodeStatusRequest request) {
        return nodeService.updateStatus(id, NodeStatus.valueOf(request.status()));
    }

    @GetMapping("/clients")
    public List<ClientResponse> listAllClients() {
        return vpnClientService.listAll();
    }
}
