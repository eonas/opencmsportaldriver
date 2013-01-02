package de.eonas.opencms.util;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jetbrains.annotations.Nullable;

import javax.crypto.*;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.Security;

public class Encryption {

    private static final Log LOG = LogFactory.getLog(Encryption.class);

    // This should be already configured in
    // JAVA_HOME/jre/lib/security/java.security
    private static final String[] SECURITY_PROVIDER = new String[]{"sun.security.provider.Sun", // sun
            "com.sun.rsajca.Provider", // ?
            "com.ibm.crypto.provider.IBMJCE", // ibm
            "com.ibm.jsse.JSSEProvider", // ibm
            "com.ibm.crypto.provider.IBMJCA", // ibm
            "com.ncipher.provider.km.mCipherKM", // bea
    };

    // JCE supported standard symmetric encryption methods
    public static final String METHOD_DES = "DES";
    public static final String METHOD_3DES = "DESede";
    public static final String METHOD_AES = "AES";
    public static final String METHOD_BF = "Blowfish";

    private static final String DEFAULT_METHOD = METHOD_AES;
    private static final boolean NO_LINE_SEP = false;

    private String method = DEFAULT_METHOD;
    @Nullable
    private SecretKey key = null;

    static {
        try {
            LOG.info("Initializing JCE...");
            // Test if JCE is already registered
            Cipher.getInstance(DEFAULT_METHOD);
            LOG.info("Successfully initalized JCE.");

        } catch (Throwable e) {

            // Try installing JCE
            if (!registerJCE()) {
                LOG.error("Failure initializing JCE: Encryption not possible.");
            } else {
                LOG.info("Successfully initalized JCE.");
            }
        }
    }

    private static boolean registerJCE() {
        for (int i = 0; i < SECURITY_PROVIDER.length; i++) {
            try {
                Class<?> clazz = Class.forName(SECURITY_PROVIDER[i]);
                Security.addProvider((Provider) clazz.newInstance());
                Cipher.getInstance(DEFAULT_METHOD);
                return true;
            } catch (Throwable t) {
                LOG.debug("Could not initialize " + SECURITY_PROVIDER[i], t);
            }
        }
        LOG.warn("Could not initialize any security provider.");
        return false;
    }

    public Encryption() throws EncryptionException {
        this(DEFAULT_METHOD);
        LOG.info("Encryption using method '" + method + "'");
    }

    public Encryption(String method) throws EncryptionException {
        try {
            setAlgorithm(method);
        } catch (NoSuchAlgorithmException e) {
            throw new EncryptionException(e);
        }
    }

    private void setAlgorithm(String method) throws NoSuchAlgorithmException {
        this.method = method;
        generateKey();
    }

    private void generateKey() throws NoSuchAlgorithmException {
        KeyGenerator keygen = KeyGenerator.getInstance(method);
        key = keygen.generateKey();
    }

    public String encrypt(@Nullable byte[] value) throws IllegalBlockSizeException, InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, BadPaddingException {
        if (value == null) return "";
        byte[] a = crypt(Cipher.ENCRYPT_MODE, value);
        return Base64.encodeToString(a, NO_LINE_SEP);
    }

    @Nullable
    public byte[] decrypt(@Nullable String value) throws EncryptionException, IllegalBlockSizeException, InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, BadPaddingException {
        if (value == null)
            return null;
        
        LOG.debug("Base64 decode " + value);
        byte[] a = Base64.decode(value.getBytes());

        if ( a != null ) {
            LOG.debug("Base64 decoded " + a.length);
        } else {
            LOG.debug("Base64 decoded to null");
        }
        a = crypt(Cipher.DECRYPT_MODE, a);
        if (a == null || a.length == 0)
            return null;
        return a;
    }

    @Nullable
    private byte[] crypt(int mode, @Nullable byte[] value) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException,
            BadPaddingException {
        if (value == null || value.length == 0)
            return null;
        Cipher cipher = Cipher.getInstance(method);
        cipher.init(mode, key);
        return cipher.doFinal(value);
    }
}
