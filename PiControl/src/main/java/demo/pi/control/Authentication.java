package demo.pi.control;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;

import com.amazonaws.services.iot.client.AWSIotMqttClient;

public class Authentication {

	public static AWSIotMqttClient getAuthenticatedClient(String endpoint, String id) throws Exception {
		
		AWSIotMqttClient client = null;
		
		String certificateFile = System.getProperty("certificateFile", null);
		String privateKeyFile = System.getProperty("privateKeyFile", null);
		if ((certificateFile != null) && (privateKeyFile != null)) {
			Certificate certificate = loadCertificate(certificateFile);
			PrivateKey privateKey = loadPrivateKey(privateKeyFile, null /* default algorithm */);
	        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
	        keyStore.load(null);
	        keyStore.setCertificateEntry("alias", certificate);

	        // randomly generated key password for the key in the KeyStore
	        String keyPassword = new BigInteger(128, new SecureRandom()).toString(32);
	        keyStore.setKeyEntry("alias", privateKey, keyPassword.toCharArray(), new Certificate[] { certificate });
			client = new AWSIotMqttClient(endpoint, id, keyStore, keyPassword);
		}

		if (client == null) {
			String awsAccessKeyId = System.getProperty("awsAccessKeyId", null);
			String awsSecretAccessKey = System.getProperty("awsSecretAccessKey", null);

			if ((awsAccessKeyId != null) && (awsSecretAccessKey != null)) {
				client = new AWSIotMqttClient(endpoint, id, awsAccessKeyId, awsSecretAccessKey);
			}
        }
			
		return client;
    }

    private static Certificate loadCertificate(String filename) {
        Certificate certificate = null;

        File file = new File(filename);
        if (!file.exists()) {
            System.out.println("Certificate file not found: " + filename);
            return null;
        }
        try (BufferedInputStream stream = new BufferedInputStream(new FileInputStream(file))) {
            CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
            certificate = certFactory.generateCertificate(stream);
        } catch (IOException | CertificateException e) {
            System.out.println("Failed to load certificate file " + filename);
        }

        return certificate;
    }

    private static PrivateKey loadPrivateKey(String filename, String algorithm) {
        PrivateKey privateKey = null;

        File file = new File(filename);
        if (!file.exists()) {
            System.out.println("Private key file not found: " + filename);
            return null;
        }
        try (DataInputStream stream = new DataInputStream(new FileInputStream(file))) {
            privateKey = PrivateKeyReader.getPrivateKey(stream, algorithm);
        } catch (IOException | GeneralSecurityException e) {
            System.out.println("Failed to load private key from file " + filename);
        }

        return privateKey;
    }

}
