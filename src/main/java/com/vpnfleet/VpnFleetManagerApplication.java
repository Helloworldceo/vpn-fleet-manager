package com.vpnfleet;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.vpnfleet.mapper")
public class VpnFleetManagerApplication {
    public static void main(String[] args) {
        SpringApplication.run(VpnFleetManagerApplication.class, args);
    }
}
