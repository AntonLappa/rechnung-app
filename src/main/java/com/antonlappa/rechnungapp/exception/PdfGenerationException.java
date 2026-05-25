package com.antonlappa.rechnungapp.exception;

/**
 * Thrown when PDF generation fails due to rendering or I/O errors.
 * <p>
 * Handled by {@link com.antonlappa.rechnungapp.exception.exceptionhandler.GlobalExceptionHandler}
 * which logs the root cause server-side and returns a stable 500 response.
 */
public class PdfGenerationException extends RuntimeException {
    public PdfGenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}
