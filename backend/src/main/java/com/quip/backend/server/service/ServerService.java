package com.quip.backend.server.service;


import com.quip.backend.common.exception.ValidationException;
import com.quip.backend.server.mapper.database.ServerMapper;
import com.quip.backend.server.model.Server;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ServerService {
    private final ServerMapper serverMapper;

    public boolean isServerExists(Long id) {
        Server server = serverMapper.selectById(id);
        return server != null;
    }


    public void validateServer(Long serverId, String operation) {
        if (serverId == null) {
            throw new ValidationException(operation, "serverId", "must not be null");
        }
        if (!isServerExists(serverId)) {
            throw new ValidationException(operation, "serverId", "must refer to an existing server");
        }
        log.info("Validated serverId: {}", serverId);
    }

}