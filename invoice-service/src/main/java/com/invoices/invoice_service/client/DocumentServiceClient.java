package com.invoices.invoice_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

@FeignClient(name = "document-service")
public interface DocumentServiceClient {

    /**
     * Sube un archivo PDF al document-service
     * @param file Archivo PDF a subir
     * @param invoiceNumber Número de factura asociado
     * @return DocumentResponse con la información del documento guardado
     */
    @PostMapping(value = "/api/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    DocumentResponse uploadPdf(@RequestPart("file") MultipartFile file,
                               @RequestParam("invoiceNumber") String invoiceNumber);
}
