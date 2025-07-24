package com.example.appJava;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.net.InetAddress;
import java.net.UnknownHostException;

@RestController
public class HostnameController {

    @GetMapping("/")
    public String getHostname() {
        try {
            return "Hostname: " + InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            return "Error retrieving hostname: " + e.getMessage();
        }
    }
}
