package com.vpnfleet.controller;

import com.vpnfleet.dto.NodeResponse;
import com.vpnfleet.service.NodeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/nodes")
@RequiredArgsConstructor
public class NodeController {

    private final NodeService nodeService;

    @GetMapping
    public List<NodeResponse> listNodes() {
        return nodeService.listAll();
    }
}
