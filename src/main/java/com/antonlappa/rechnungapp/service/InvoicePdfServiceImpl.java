package com.antonlappa.rechnungapp.service;

import com.antonlappa.rechnungapp.exception.PdfGenerationException;
import com.antonlappa.rechnungapp.exception.ResourceNotFoundException;
import com.antonlappa.rechnungapp.exception.BusinessRuleException;
import com.antonlappa.rechnungapp.repository.CompanyProfileRepository;
import com.antonlappa.rechnungapp.repository.InvoiceRepository;
import com.antonlappa.rechnungapp.repository.entity.CompanyProfileEntity;
import com.antonlappa.rechnungapp.repository.entity.InvoiceEntity;
import com.antonlappa.rechnungapp.repository.entity.InvoiceItemEntity;
import com.antonlappa.rechnungapp.repository.entity.InvoiceStatus;
import com.antonlappa.rechnungapp.repository.entity.VatMode;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.util.Base64;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Service responsible for generating invoice PDFs.
 * <p>
 * Uses Thymeleaf to render an HTML template and OpenHTMLtoPDF
 * to convert the rendered HTML into a PDF document.
 * <p>
 * This service is completely separated from {@link InvoiceService}
 * and does not modify any invoice data.
 */
@Service
@RequiredArgsConstructor
public class InvoicePdfServiceImpl implements InvoicePdfService {

    private final InvoiceRepository invoiceRepository;
    private final CompanyProfileRepository companyProfileRepository;
    private final TemplateEngine templateEngine;
    private final StorageService storageService;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale.GERMAN);

    private static final DecimalFormat AMOUNT_FORMAT;
    private static final DecimalFormat QUANTITY_FORMAT;
    private static final DecimalFormat VAT_RATE_FORMAT;

    static {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.GERMAN);
        symbols.setDecimalSeparator(',');
        symbols.setGroupingSeparator('.');

        AMOUNT_FORMAT = new DecimalFormat("#,##0.00", symbols);
        QUANTITY_FORMAT = new DecimalFormat("#,##0.####", symbols);
        VAT_RATE_FORMAT = new DecimalFormat("0", symbols);
    }

    /**
     * Generates a PDF for the given invoice.
     *
     * @param userId    the authenticated user's UUID (for access control)
     * @param invoiceId the invoice to render
     * @return the PDF as a byte array
     * @throws ResourceNotFoundException if the invoice does not exist or does not
     *                                   belong to the user
     * @throws IllegalArgumentException  if the invoice is not in FINAL status
     * @throws IllegalArgumentException  if the user has no company profile
     */
    @Override
    @Transactional(readOnly = true)
    public PdfDocument generatePdf(UUID userId, UUID invoiceId) {
        // 1. Load and validate the invoice
        InvoiceEntity invoice = invoiceRepository.findByIdAndUserId(invoiceId, userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Invoice not found with id: " + invoiceId));

        if (invoice.getStatus() != InvoiceStatus.FINAL) {
            throw new BusinessRuleException(
                    "PDF can only be generated for FINAL invoices. Current status: " + invoice.getStatus());
        }

        // 2. Load company profile (required for seller info on the invoice)
        CompanyProfileEntity profile = companyProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Company profile is required to generate invoices. Please create one first."));

        // 3. Build Thymeleaf context
        Context context = buildThymeleafContext(invoice, profile);

        // 4. Render HTML
        String html = templateEngine.process("invoice", context);

        // 5. Convert HTML → PDF
        byte[] pdfBytes = renderPdf(html);

        String filename = invoice.getInvoiceNumber() != null
                ? "invoice-" + invoice.getInvoiceNumber() + ".pdf"
                : "invoice-" + invoiceId + ".pdf";

        return new PdfDocument(pdfBytes, filename);
    }

    // ── Private helpers ──────────────────────────────────────────────

    /**
     * Builds the Thymeleaf context with all variables needed by the invoice
     * template.
     */
    private Context buildThymeleafContext(InvoiceEntity invoice, CompanyProfileEntity profile) {
        Context context = new Context(Locale.GERMAN);

        // CSS path — resolved from classpath for OpenHTMLtoPDF
        String cssPath = resolveClasspathUri("templates/invoice.css");
        context.setVariable("cssPath", cssPath);

        // ── Company (seller) info ────────────────────────────────────
        context.setVariable("companyName", profile.getCompanyName());
        context.setVariable("ownerName", profile.getOwnerName());
        context.setVariable("companyStreet", blankToNull(profile.getStreet()));
        context.setVariable("companyPostalCode", blankToNull(profile.getPostalCode()));
        context.setVariable("companyCity", blankToNull(profile.getCity()));
        context.setVariable("companyAddressOneLine", buildAddressOneLine(profile));
        context.setVariable("companyEmail", profile.getEmail());
        context.setVariable("companyPhone", profile.getPhone());
        context.setVariable("taxNumber", blankToNull(profile.getTaxNumber()));
        context.setVariable("vatId", blankToNull(profile.getVatId()));
        context.setVariable("registrationNumber", blankToNull(profile.getRegistrationNumber()));
        context.setVariable("registrationCourt", blankToNull(profile.getRegistrationCourt()));
        context.setVariable("registrationCountry", blankToNull(profile.getRegistrationCountry()));
        context.setVariable("bankName", blankToNull(profile.getBankName()));
        context.setVariable("iban", blankToNull(profile.getIban()));
        context.setVariable("bic", blankToNull(profile.getBic()));
        context.setVariable("logoDataUri", buildLogoDataUri(profile.getLogoPath()));

        // ── Customer (recipient) info ────────────────────────────────
        context.setVariable("customerName", invoice.getCustomer().getName());
        context.setVariable("customerAddressHtml",
                xmlEscapeText(invoice.getCustomer().getAddress()).replace("\n", "<br/>"));
        context.setVariable("customerVatId", invoice.getCustomer().getVatId());

        // ── Invoice metadata ─────────────────────────────────────────
        context.setVariable("invoiceNumber", invoice.getInvoiceNumber());
        context.setVariable("invoiceDateFormatted", formatDate(invoice.getInvoiceDate()));
        context.setVariable("serviceDateFormatted",
                invoice.getServiceDate() != null ? formatDate(invoice.getServiceDate()) : null);
        context.setVariable("currency", invoice.getCurrency());

        // Currency symbol for display
        String currencySymbol = "EUR".equals(invoice.getCurrency()) ? "€" : invoice.getCurrency();
        context.setVariable("currencySymbol", currencySymbol);

        // Payment due date (30 days from invoice date)
        LocalDate dueDate = invoice.getInvoiceDate().plusDays(14);
        context.setVariable("paymentDueDate", formatDate(dueDate));

        // ── VAT mode handling ────────────────────────────────────────
        boolean showVatColumns = invoice.getVatMode() == VatMode.STANDARD;
        context.setVariable("showVatColumns", showVatColumns);

        // Determine the dominant VAT percentage for display in totals
        String vatPercentageDisplay = "";
        if (showVatColumns && !invoice.getItems().isEmpty()) {
            BigDecimal dominantRate = invoice.getItems().stream()
                    .map(InvoiceItemEntity::getVatPercentage)
                    .filter(rate -> rate.compareTo(BigDecimal.ZERO) > 0)
                    .findFirst()
                    .orElse(BigDecimal.ZERO);
            vatPercentageDisplay = VAT_RATE_FORMAT.format(dominantRate) + "%";
        }
        context.setVariable("vatPercentageDisplay", vatPercentageDisplay);

        String vatModeNote = switch (invoice.getVatMode()) {
            case KLEINUNTERNEHMER -> "Gemäß § 19 UStG wird keine Umsatzsteuer berechnet.";
            case VAT_FREE -> "Steuerfreie Leistung gemäß UStG.";
            case STANDARD -> null;
        };
        context.setVariable("vatModeNote", vatModeNote);

        // ── Items ────────────────────────────────────────────────────
        List<InvoiceItemEntity> itemEntities = invoice.getItems();
        List<Map<String, Object>> formattedItems = new ArrayList<>(itemEntities.size());
        for (int i = 0; i < itemEntities.size(); i++) {
            Map<String, Object> itemMap = formatItem(itemEntities.get(i));
            itemMap.put("position", i + 1);
            formattedItems.add(itemMap);
        }
        context.setVariable("items", formattedItems);

        // ── Totals ───────────────────────────────────────────────────
        context.setVariable("totalNetFormatted", formatAmount(invoice.getTotalNet()));
        context.setVariable("totalVatFormatted", formatAmount(invoice.getTotalVat()));
        context.setVariable("totalGrossFormatted", formatAmount(invoice.getTotalGross()));

        return context;
    }

    /**
     * Formats a single invoice item into a map of display-ready values.
     */
    private Map<String, Object> formatItem(InvoiceItemEntity item) {
        Map<String, Object> map = new HashMap<>();
        map.put("position", item.getPosition());
        map.put("name", item.getName());
        map.put("description", item.getDescription());
        map.put("unit", item.getUnit());
        map.put("quantityFormatted", formatQuantity(item.getQuantity()));
        map.put("unitPriceFormatted", formatAmount(item.getUnitPrice()));
        map.put("totalNetFormatted", formatAmount(item.getTotalNet()));
        map.put("vatPercentageFormatted", VAT_RATE_FORMAT.format(item.getVatPercentage()));
        map.put("totalVatFormatted", formatAmount(item.getTotalVat()));
        map.put("totalGrossFormatted", formatAmount(item.getTotalGross()));
        return map;
    }

    /**
     * Renders the given HTML string into a PDF byte array using OpenHTMLtoPDF.
     */
    private byte[] renderPdf(String html) {
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            String baseUri = resolveClasspathUri("templates/");

            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withHtmlContent(html, baseUri);
            builder.toStream(os);
            builder.run();

            return os.toByteArray();
        } catch (Exception e) {
            throw new PdfGenerationException("Failed to generate PDF", e);
        }
    }

    /**
     * Resolves a classpath resource path to a URI string that OpenHTMLtoPDF
     * can use to load CSS and other resources.
     */
    private String resolveClasspathUri(String path) {
        try {
            return new ClassPathResource(path).getURL().toExternalForm();
        } catch (Exception e) {
            throw new PdfGenerationException("Failed to resolve classpath resource: " + path, e);
        }
    }

    private String buildLogoDataUri(String logoKey) {
        if (logoKey == null || logoKey.isBlank()) {
            return null;
        }
        try {
            byte[] bytes = storageService.download(logoKey);
            String ext = logoKey.substring(logoKey.lastIndexOf('.') + 1).toLowerCase();
            String mimeType = ext.equals("jpg") ? "image/jpeg" : "image/" + ext;
            return "data:" + mimeType + ";base64," + Base64.getEncoder().encodeToString(bytes);
        } catch (Exception e) {
            return null;
        }
    }

    private String buildAddressOneLine(com.antonlappa.rechnungapp.repository.entity.CompanyProfileEntity profile) {
        String street = blankToNull(profile.getStreet());
        String postal = blankToNull(profile.getPostalCode());
        String city = blankToNull(profile.getCity());
        String country = blankToNull(profile.getRegistrationCountry());

        StringBuilder sb = new StringBuilder();
        if (street != null) sb.append(street);
        String postalCity = (postal != null && city != null) ? postal + " " + city
                          : (postal != null) ? postal
                          : city;
        if (postalCity != null) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(postalCity);
        }
        if (country != null) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(country);
        }
        return sb.length() > 0 ? sb.toString() : blankToNull(profile.getAddress()) != null
                ? profile.getAddress().replace("\n", ", ") : "";
    }

    /** Trims whitespace; returns {@code null} when absent or blank (hides optional footer rows). */
    private static String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    // Escapes only the XML special characters (&, <, >) so that the result is
    // safe to inject via th:utext. Intentionally does NOT encode non-ASCII chars
    // (ß, ä, ö, ü …) as named HTML entities — those are not valid XML entities
    // and would cause OpenHTMLtoPDF's parser to fail.
    private static String xmlEscapeText(String text) {
        if (text == null)
            return "";
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private String formatDate(LocalDate date) {
        return date.format(DATE_FORMATTER);
    }

    private String formatAmount(BigDecimal amount) {
        return AMOUNT_FORMAT.format(amount);
    }

    private String formatQuantity(BigDecimal quantity) {
        return QUANTITY_FORMAT.format(quantity);
    }
}
