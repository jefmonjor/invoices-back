package com.invoices.invoice.infrastructure.verifactu;

import com.invoices.invoice.application.services.InvoiceCanonicalService;
import com.invoices.invoice.domain.entities.Client;
import com.invoices.invoice.domain.entities.Company;
import com.invoices.invoice.domain.entities.Invoice;
import com.invoices.invoice.domain.ports.ClientRepository;
import com.invoices.invoice.domain.ports.CompanyRepository;
import com.invoices.invoice.domain.ports.InvoiceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.w3c.dom.Document;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VerifactuRealServiceTest {

        @Mock
        private VerifactuXmlBuilder xmlBuilder;
        @Mock
        private XadesSigner signer;
        @Mock
        private VerifactuSoapClient soapClient;
        @Mock
        private InvoiceRepository invoiceRepository;
        @Mock
        private CompanyRepository companyRepository;
        @Mock
        private ClientRepository clientRepository;
        @Mock
        private InvoiceCanonicalService canonicalService;

        @InjectMocks
        private VerifactuRealService verifactuService;

        private Invoice invoice;
        private Company company;
        private Client client;

        @BeforeEach
        void setUp() {
                invoice = mock(Invoice.class);
                company = mock(Company.class);
                client = mock(Client.class);

                when(invoice.getId()).thenReturn(1L);
                when(invoice.getCompanyId()).thenReturn(100L);
                when(invoice.getClientId()).thenReturn(200L);
        }

        @Test
        void shouldChainWithPreviousInvoiceHash() throws Exception {
                // Given
                String previousHash = "PREVIOUS_HASH_123";
                String currentHash = "CURRENT_HASH_456";
                Invoice previousInvoice = mock(Invoice.class);

                when(invoiceRepository.findById(1L)).thenReturn(Optional.of(invoice));
                when(companyRepository.findById(100L)).thenReturn(Optional.of(company));
                when(clientRepository.findById(200L)).thenReturn(Optional.of(client));

                // Mock finding previous invoice
                when(invoiceRepository.findLastInvoiceByCompanyIdAndIdNot(100L, 1L))
                                .thenReturn(Optional.of(previousInvoice));
                when(previousInvoice.getDocumentHash()).thenReturn(previousHash);

                // Mock canonical service
                when(canonicalService.calculateInvoiceHash(invoice, company, client, previousHash))
                                .thenReturn(currentHash);

                // Mock XML building and signing
                when(xmlBuilder.buildAltaFacturaXml(any(), any(), any(), eq(currentHash), eq(previousHash)))
                                .thenReturn(mock(Document.class));
                when(signer.signDocument(any())).thenReturn(mock(Document.class));
                when(soapClient.sendInvoice(any())).thenReturn(mock(Document.class));

                // When
                verifactuService.processInvoice(1L);

                // Then
                verify(invoice).setPreviousDocumentHash(previousHash);
                verify(invoice).setDocumentHash(currentHash);
                verify(invoiceRepository, times(1)).save(invoice);
        }

        @Test
        void shouldHandleGenesisInvoiceWithEmptyPreviousHash() throws Exception {
                // Given
                String currentHash = "GENESIS_HASH_789";

                when(invoiceRepository.findById(1L)).thenReturn(Optional.of(invoice));
                when(companyRepository.findById(100L)).thenReturn(Optional.of(company));
                when(clientRepository.findById(200L)).thenReturn(Optional.of(client));

                // Mock NO previous invoice (Genesis)
                when(invoiceRepository.findLastInvoiceByCompanyIdAndIdNot(100L, 1L))
                                .thenReturn(Optional.empty());

                // Mock canonical service with empty previous hash
                when(canonicalService.calculateInvoiceHash(invoice, company, client, ""))
                                .thenReturn(currentHash);

                // Mock XML building
                when(xmlBuilder.buildAltaFacturaXml(any(), any(), any(), eq(currentHash), eq("")))
                                .thenReturn(mock(Document.class));
                when(signer.signDocument(any())).thenReturn(mock(Document.class));
                when(soapClient.sendInvoice(any())).thenReturn(mock(Document.class));

                // When
                verifactuService.processInvoice(1L);

                // Then
                verify(invoice).setPreviousDocumentHash("");
                verify(invoice).setDocumentHash(currentHash);
                verify(invoiceRepository, times(1)).save(invoice);
        }
}
