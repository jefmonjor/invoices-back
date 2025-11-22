package com.invoices.invoice.infrastructure.verifactu;

import lombok.extern.slf4j.Slf4j;
import org.apache.wss4j.common.crypto.Crypto;
import org.apache.wss4j.common.crypto.CryptoFactory;
import org.apache.wss4j.dom.WSConstants;
import org.apache.wss4j.dom.message.WSSecHeader;
import org.apache.wss4j.dom.message.WSSecSignature;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;

import java.util.Properties;

@Component
@Slf4j
public class XadesSigner {

    @Value("${verifactu.keystore.path:keystore.jks}")
    private String keystorePath;

    @Value("${verifactu.keystore.password:password}")
    private String keystorePassword;

    @Value("${verifactu.keystore.alias:alias}")
    private String keystoreAlias;

    public Document signDocument(Document doc) {
        try {
            WSSecHeader secHeader = new WSSecHeader(doc);
            secHeader.insertSecurityHeader();

            Properties properties = new Properties();
            properties.put("org.apache.wss4j.crypto.provider", "org.apache.wss4j.common.crypto.Merlin");
            properties.put("org.apache.wss4j.crypto.merlin.keystore.type", "jks");
            properties.put("org.apache.wss4j.crypto.merlin.keystore.password", keystorePassword);
            properties.put("org.apache.wss4j.crypto.merlin.keystore.alias", keystoreAlias);
            properties.put("org.apache.wss4j.crypto.merlin.keystore.file", keystorePath);

            Crypto crypto = CryptoFactory.getInstance(properties);

            WSSecSignature builder = new WSSecSignature(secHeader);
            builder.setUserInfo(keystoreAlias, keystorePassword);
            builder.setKeyIdentifierType(WSConstants.BST_DIRECT_REFERENCE);
            builder.setSignatureAlgorithm(WSConstants.RSA_SHA256);

            // XAdES specific configuration would go here (simplified for now)
            // Real implementation needs to add SignedProperties with Policy Identifier

            return builder.build(crypto);
        } catch (Exception e) {
            log.error("Error signing document", e);
            throw new RuntimeException("Failed to sign VeriFactu XML", e);
        }
    }
}
