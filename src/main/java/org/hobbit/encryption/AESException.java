package org.hobbit.encryption;

public class AESException extends Exception {
    private static String defaultMessage = "Error initializing encryption algorithm AES/ECB/PKCS5PADDING. " +
        "Please check your Java distribution for the availability of this type of encryption. " +
        "To fall back to unencrypted communication between storage-service and other services " +
        "unset AES_PASSWORD and AES_SALT environmental variables.";

    public AESException() {
        super(defaultMessage);
    }
}
