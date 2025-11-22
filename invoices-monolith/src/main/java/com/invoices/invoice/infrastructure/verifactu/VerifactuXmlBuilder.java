package com.invoices.invoice.infrastructure.verifactu;

import com.invoices.invoice.domain.entities.Invoice;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.time.format.DateTimeFormatter;

@Service
@Slf4j
public class VerifactuXmlBuilder {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    public Document buildAltaFacturaXml(Invoice invoice) throws ParserConfigurationException {
        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

        // Root element: SuministroLRFacturasEmitidas
        Document doc = docBuilder.newDocument();
        Element rootElement = doc.createElementNS(
                "https://www2.agenciatributaria.gob.es/static_files/common/internet/dep/aplicaciones/es/aeat/tike/cont/ws/SuministroLR.xsd",
                "SuministroLRFacturasEmitidas");
        doc.appendChild(rootElement);

        // Cabecera
        Element cabecera = doc.createElement("Cabecera");
        rootElement.appendChild(cabecera);

        Element idVersion = doc.createElement("IDVersionSii");
        idVersion.appendChild(doc.createTextNode("1.0")); // Example version
        cabecera.appendChild(idVersion);

        // Titular
        Element titular = doc.createElement("Titular");
        cabecera.appendChild(titular);

        Element nombreRazon = doc.createElement("NombreRazon");
        nombreRazon.appendChild(doc.createTextNode(invoice.getCompany().getBusinessName()));
        titular.appendChild(nombreRazon);

        Element nif = doc.createElement("NIF");
        nif.appendChild(doc.createTextNode(invoice.getCompany().getTaxId()));
        titular.appendChild(nif);

        // RegistroLRFacturasEmitidas
        Element registro = doc.createElement("RegistroLRFacturasEmitidas");
        rootElement.appendChild(registro);

        // PeriodoImpositivo
        Element periodo = doc.createElement("PeriodoImpositivo");
        registro.appendChild(periodo);

        Element ejercicio = doc.createElement("Ejercicio");
        ejercicio.appendChild(doc.createTextNode(String.valueOf(invoice.getIssueDate().getYear())));
        periodo.appendChild(ejercicio);

        Element periodoDetalle = doc.createElement("Periodo");
        periodoDetalle.appendChild(doc.createTextNode("0A")); // 0A = Annual/Not specified
        periodo.appendChild(periodoDetalle);

        // IDFactura
        Element idFactura = doc.createElement("IDFactura");
        registro.appendChild(idFactura);

        Element idEmisorFactura = doc.createElement("IDEmisorFactura");
        idFactura.appendChild(idEmisorFactura);

        Element nifEmisor = doc.createElement("NIF");
        nifEmisor.appendChild(doc.createTextNode(invoice.getCompany().getTaxId()));
        idEmisorFactura.appendChild(nifEmisor);

        Element numSerieFactura = doc.createElement("NumSerieFacturaEmisor");
        numSerieFactura.appendChild(doc.createTextNode(invoice.getInvoiceNumber()));
        idFactura.appendChild(numSerieFactura);

        Element fechaExpedicion = doc.createElement("FechaExpedicionFacturaEmisor");
        fechaExpedicion.appendChild(doc.createTextNode(invoice.getIssueDate().format(DATE_FORMATTER)));
        idFactura.appendChild(fechaExpedicion);

        // FacturaExpedida
        Element facturaExpedida = doc.createElement("FacturaExpedida");
        registro.appendChild(facturaExpedida);

        Element tipoFactura = doc.createElement("TipoFactura");
        tipoFactura.appendChild(doc.createTextNode("F1")); // F1 = Factura ordinaria
        facturaExpedida.appendChild(tipoFactura);

        Element descripcionOperacion = doc.createElement("DescripcionOperacion");
        descripcionOperacion.appendChild(doc.createTextNode("Prestación de servicios"));
        facturaExpedida.appendChild(descripcionOperacion);

        // Desglose (Importes)
        Element desglose = doc.createElement("Desglose");
        facturaExpedida.appendChild(desglose);

        Element sujeta = doc.createElement("Sujeta");
        desglose.appendChild(sujeta);

        Element noExenta = doc.createElement("NoExenta");
        sujeta.appendChild(noExenta);

        Element desgloseIVA = doc.createElement("DesgloseIVA");
        noExenta.appendChild(desgloseIVA);

        // Calculate VAT breakdown
        java.util.Map<java.math.BigDecimal, java.math.BigDecimal> vatBreakdown = new java.util.HashMap<>();

        for (com.invoices.invoice.domain.entities.InvoiceItem item : invoice.getItems()) {
            java.math.BigDecimal vatRate = item.getVatPercentage();
            java.math.BigDecimal base = item.calculateSubtotal(); // Subtotal includes discounts

            vatBreakdown.merge(vatRate, base, java.math.BigDecimal::add);
        }

        for (java.util.Map.Entry<java.math.BigDecimal, java.math.BigDecimal> entry : vatBreakdown.entrySet()) {
            java.math.BigDecimal rate = entry.getKey();
            java.math.BigDecimal base = entry.getValue();
            java.math.BigDecimal quota = base.multiply(rate).divide(new java.math.BigDecimal("100"), 2,
                    java.math.RoundingMode.HALF_UP);

            Element detalleIVA = doc.createElement("DetalleIVA");
            desgloseIVA.appendChild(detalleIVA);

            Element tipoImpositivo = doc.createElement("TipoImpositivo");
            tipoImpositivo.appendChild(doc.createTextNode(rate.toString()));
            detalleIVA.appendChild(tipoImpositivo);

            Element baseImponible = doc.createElement("BaseImponible");
            baseImponible.appendChild(doc.createTextNode(base.toString()));
            detalleIVA.appendChild(baseImponible);

            Element cuotaRepercutida = doc.createElement("CuotaRepercutida");
            cuotaRepercutida.appendChild(doc.createTextNode(quota.toString()));
            detalleIVA.appendChild(cuotaRepercutida);
        }

        return doc;
    }
}
