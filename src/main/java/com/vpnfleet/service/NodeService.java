package com.vpnfleet.service;

import com.vpnfleet.dto.CreateNodeRequest;
import com.vpnfleet.dto.NodeResponse;
import com.vpnfleet.entity.NodeStatus;
import com.vpnfleet.entity.VpnNode;
import com.vpnfleet.exception.ApiException;
import com.vpnfleet.mapper.VpnNodeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NodeService {

    private final VpnNodeMapper vpnNodeMapper;

    @Cacheable(cacheNames = "nodes", key = "'all'")
    public List<NodeResponse> listAll() {
        return vpnNodeMapper.findAll().stream().map(NodeResponse::from).toList();
    }

    public VpnNode getByIdOrThrow(Long id) {
        return vpnNodeMapper.findById(id)
                .orElseThrow(() -> ApiException.notFound("No node with id " + id));
    }

    @CacheEvict(cacheNames = "nodes", key = "'all'")
    public NodeResponse createNode(CreateNodeRequest request) {
        VpnNode node = VpnNode.builder()
                .name(request.name())
                .host(request.host())
                .region(request.region())
                .protocol(request.protocol() == null || request.protocol().isBlank() ? "VLESS_REALITY" : request.protocol())
                .capacity(request.capacity())
                .activeClientCount(0)
                .status(NodeStatus.ACTIVE)
                .build();
        vpnNodeMapper.insert(node);
        return NodeResponse.from(node);
    }

    @CacheEvict(cacheNames = "nodes", key = "'all'")
    public NodeResponse updateStatus(Long id, NodeStatus status) {
        VpnNode node = getByIdOrThrow(id);
        vpnNodeMapper.updateStatus(id, status.name());
        node.setStatus(status);
        return NodeResponse.from(node);
    }
}
