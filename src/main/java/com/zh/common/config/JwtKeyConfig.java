package com.zh.common.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import java.io.InputStream;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;

@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class JwtKeyConfig {

    @Bean
    public KeyPair keyPair(JwtProperties properties) throws Exception {
        Resource location = properties.getLocation();
        char[] password = properties.getPassword().toCharArray();
        // 1.加载jks密钥库
        KeyStore keyStore = KeyStore.getInstance("JKS");
        try (InputStream in = location.getInputStream()) {
            keyStore.load(in, password);
        }
        // 2.取出私钥条目
        KeyStore.PrivateKeyEntry entry = (KeyStore.PrivateKeyEntry) keyStore.getEntry(
                properties.getAlias(), new KeyStore.PasswordProtection(password));
        PrivateKey privateKey = entry.getPrivateKey();
        // 3.从证书中获取公钥
        Certificate cert = entry.getCertificate();
        PublicKey publicKey = cert.getPublicKey();
        // 4.组装KeyPair
        return new KeyPair(publicKey, privateKey);
    }
}
