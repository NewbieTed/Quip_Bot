package com.quip.backend.server.service;


import com.quip.backend.common.exception.ValidationException;
import com.quip.backend.server.mapper.database.ServerMapper;
import com.quip.backend.server.model.Server;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ServerService {
    private final ServerMapper serverMapper;


    public Server validateServer(Long serverId, String operation) {
        if (serverId == null) {
            throw new ValidationException(operation, "serverId", "must not be null");
        }
        if (operation == null) {
            throw new IllegalArgumentException("Parameter 'operation' must not be null.");
        }
        Server server = serverMapper.selectById(serverId);
        if (server == null) {
            throw new ValidationException(operation, "serverId", "must refer to an existing server");
        }
        log.info("Validated serverId: {}", serverId);
        return server;
    }

    public Server assertServerExist(Long serverId) {
        if (serverId == null) {
            throw new IllegalArgumentException("Parameter 'serverId' must not be null");
        }
        Server server = serverMapper.selectById(serverId);
        if (server == null) {
            throw new EntityNotFoundException("Server not found for serverId: " + serverId);
        }
        log.info("ServerId existence validated: {}", serverId);
        return server;
    }

}