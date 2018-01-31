package org.hobbit.encryption;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

/**
 * This class is a wrapper for Java AES encryption.
 *
 * @author Ivan Ermilov (iermilov@informatik.uni-leipzig.de)
 *
 */

public class AES {
    private static final Logger LOGGER = LoggerFactory.getLogger(AES.class);

    private Cipher cipher = null;
    private SecretKey secretKey = null;

    public AES(String password, String salt) {
        this.secretKey = AESKeyGenerator.generate(password, salt);

        try {
            this.cipher = Cipher.getInstance("AES/ECB/PKCS5PADDING");
        } catch (NoSuchAlgorithmException e) {
            LOGGER.error("Error initializing AES encryption. AES algorithm not found.\n" + e.getStackTrace());
        } catch (NoSuchPaddingException e) {
            LOGGER.error("Error initializing AES encryption. PKCS5PADDING not found.\n" + e.getStackTrace());
        }
    }

    public byte[] encrypt(String input) {
        byte[] toEncrypt = input.getBytes();
        byte[] encrypted = null;
        try {
            cipher.init(Cipher.ENCRYPT_MODE, this.secretKey);
        } catch (InvalidKeyException e) {
            LOGGER.error("Could not init ENCRYPT_MODE. Invalid key.\n" + e.getStackTrace());
        }

        try {
            encrypted = cipher.doFinal(toEncrypt);
        } catch (IllegalBlockSizeException e) {
            LOGGER.error("Could not encrypt message. Illegal block size.\n" + e.getStackTrace());
        } catch (BadPaddingException e) {
            LOGGER.error("Could not encrypt message. Bad padding.\n" + e.getStackTrace());
        }

        return encrypted;
    }

    public String decrypt(byte[] input) {
        SecretKeySpec spec = new SecretKeySpec(secretKey.getEncoded(), "AES");

        try {
            cipher.init(Cipher.DECRYPT_MODE, spec);
        } catch (InvalidKeyException e) {
            LOGGER.error("Could not init DECRYPT_MODE. Invalid key.\n" + e.getStackTrace());
        }

        byte[] decrypted = new byte[0];
        try {
            decrypted = cipher.doFinal(input);
        } catch (IllegalBlockSizeException e) {
            LOGGER.error("Could not init DECRYPT_MODE. Illegal block size.\n" + e.getStackTrace());
        } catch (BadPaddingException e) {
            LOGGER.error("Could not init DECRYPT_MODE. Bad padding.\n" + e.getStackTrace());
        }
        return new String(decrypted);
    }
}
