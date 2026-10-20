package com.m4r10kyou.shortlinkQr.web;

import com.m4r10kyou.shortlinkQr.service.AdminTokenVerifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.health.actuate.endpoint.HealthEndpoint;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;


@RestController
@RequestMapping(value = "/api/health", produces = {MediaType.APPLICATION_JSON_VALUE})
public class HealthController {

    private final String name;
    private final String description;
    private final String version;
    private final HealthEndpoint healthEndpoint;
    private final AdminTokenVerifier adminTokenVerifier;

    public HealthController(@Value("${spring.application.name}")String name,
                            @Value("${app.description}")String description,
                            @Value("${app.version}")String version,
                            HealthEndpoint healthEndpoint, AdminTokenVerifier adminTokenVerifier) {

        this.name = name;
        this.description = description;
        this.version = version;

        this.healthEndpoint = healthEndpoint;
        this.adminTokenVerifier = adminTokenVerifier;
    }


    @GetMapping("/ping")
    public ResponseEntity<Map<String, String>> ping(){

        return ResponseEntity.ok(Map.of("status","OK") );
    }

    @GetMapping({"", "/"})
    public ResponseEntity<Map<String, String>> check(  @RequestHeader(required = false , name ="X-Admin-Token")String rawToken){


        Map<String,String> map = new HashMap<>();

        map.put("project",name);
        map.put("description",description);
        map.put("version",version);

        if(adminTokenVerifier.verify(rawToken)) {
            String health = healthEndpoint.health().getStatus().getCode();
            map.put("status", health);
        }

        return ResponseEntity.ok(map);
    }
}
