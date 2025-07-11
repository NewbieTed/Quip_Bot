package com.quip.backend.server.service;


import com.quip.backend.server.mapper.database.ServerMapper;
import com.quip.backend.server.model.Server;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ServerService {
    private final ServerMapper serverMapper;

    public boolean isServerExists(Long id) {
        Server server = serverMapper.findById(id);
        return server != null;
    }
}