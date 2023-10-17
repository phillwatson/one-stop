package com.hillayes.auth.crypto;

import com.hillayes.auth.errors.EncryptionConfigException;
import lombok.extern.slf4j.Slf4j;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import jakarta.enterprise.context.ApplicationScoped;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Random;

/**
 * A class for generating an encrypted hash value of a given password. This hash
 * can be safely persisted without revealing the password itself. Later the same
 * password can be verified by repeating the same hash generation and comparing
 * the result with the original, persisted hash.
 */
@ApplicationScoped
@Slf4j
public class PasswordCrypto {
    /**
     * The number of iterations that the encryption algorithm will use.
     */
    private static final int ITERATIONS = 1000;

    /**
     * The character used to delineate the elements (iterations, hash and salt) of the generated
     * password hash.
     */
    private static final char DELIMITER = ':';

    // an assortment of characters to select from when generating a random password
    private static final char[][] SELECTIONS = {
        "abcdefghijklmnopqrstuvwxyz".toCharArray(),
        "ABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray(),
        "0123456789".toCharArray(),
        "!£$%^&*()-=+@;:<>,./?".toCharArray()
    };

    // a randomizer with which to select characters from the above collections
    private static Random random;

    /**
     * The random number generator used to generate salt values for the password encryption.
     */
    private final SecureRandom saltGenerator;

    /**
     * The encrypted secret key factory; used to apply the encryption algorithm to the secret
     * phrase.
     */
    private final SecretKeyFactory keyFactory;

    public PasswordCrypto() throws NoSuchAlgorithmException {
        saltGenerator = SecureRandom.getInstance("SHA1PRNG");
        keyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
    }

    /**
     * Generate a hash of the given password. Subsequent, the value given can be passed, with the
     * return value, into the {@link #verify(char[], String)} method in order to verify password is
     * a match with the original.
     *
     * @param aPassword the password to be encrypted.
     * @return the encrypted form of the given password.
     * @throws EncryptionConfigException if the encryption API is not configured correctly.
     */
    public String getHash(char[] aPassword) throws EncryptionConfigException {
        try {
            byte[] salt = getSalt();
            byte[] hash = getPBKDF2(aPassword, salt, PasswordCrypto.ITERATIONS, 64 * 8);

            return String.valueOf(PasswordCrypto.ITERATIONS) + PasswordCrypto.DELIMITER + toHex(salt)
                + PasswordCrypto.DELIMITER + toHex(hash);
        } catch (InvalidKeySpecException e) {
            throw new EncryptionConfigException(e);
        }
    }

    /**
     * Verify that the given (unencrypted) password matches the original encrypted value. The given
     * secret is the value originally generated from the {@link #getHash(char[])} method, for the
     * same password.
     *
     * @param aPassword the password (unencrypted) to be verified.
     * @param aSecret   the encrypted form of the original password to which this given password is to
     *                  be verified.
     * @return <code>true</code> if the given password is a match for the given encrypted form.
     */
    public boolean verify(char[] aPassword, String aSecret) {
        String[] parts = aSecret.split(String.valueOf(PasswordCrypto.DELIMITER));
        int iterations = Integer.parseInt(parts[0]);
        byte[] salt = fromHex(parts[1]);
        byte[] hash = fromHex(parts[2]);

        try {
            byte[] newHash = getPBKDF2(aPassword, salt, iterations, hash.length * 8);

            if (hash.length == newHash.length) {
                // compare the hash values byte-for-byte
                for (int i = 0; i < hash.length; i++) {
                    if (hash[i] != newHash[i])
                        return false;
                }

                return true;
            }
        } catch (GeneralSecurityException e) {
            log.warn("Unable to verify password", e);
        }

        return false;
    }

    /**
     * Generates a hash value for the given password and salt, running the hash algorithm the given
     * number of iterations.
     *
     * @param aPassword   the password to be hashed.
     * @param aSalt       the salt to include in the password.
     * @param aIterations the number of iterations to perform the hashing algorithm.
     * @param aLength     the length of the generated key
     * @return the generated hash value.
     * @throws InvalidKeySpecException if the given parameters ar inappropriate for the algorithm to
     *                                 produce a hash.
     */
    private byte[] getPBKDF2(char[] aPassword, byte[] aSalt, int aIterations, int aLength)
        throws InvalidKeySpecException {
        PBEKeySpec spec = new PBEKeySpec(aPassword, aSalt, aIterations, aLength);
        return keyFactory.generateSecret(spec).getEncoded();
    }

    /**
     * Generate a new, random 16-byte salt value. This will be applied to the hashing algorithm to
     * prevent the generated hash value being guessed using a rainbow table (a table of well-known
     * hash values and their corresponding original password values).
     *
     * @return the array of random values.
     */
    private byte[] getSalt() {
        // Create array for salt - 16 byte salt
        byte[] salt = new byte[16];

        //Get a random salt
        saltGenerator.nextBytes(salt);

        return salt;
    }

    private byte[] fromHex(String aHex) {
        byte[] result = new byte[aHex.length() / 2];
        for (int i = 0; i < result.length; i++) {
            result[i] = (byte) Integer.parseInt(aHex.substring(2 * i, 2 * i + 2), 16);
        }
        return result;
    }

    private String toHex(byte[] aBytes) {
        BigInteger bi = new BigInteger(1, aBytes);
        String result = bi.toString(16);

        int paddingLength = (aBytes.length * 2) - result.length();
        if (paddingLength > 0) {
            result = String.format("%0" + paddingLength + "d", 0) + result;
        }

        return result;
    }

    /**
     * Generates a random password of the given number characters.
     *
     * @param aLength the required length of the password.
     * @return the random password of the given length.
     */
    public char[] randomPassword(int aLength) {
        char[] result = new char[aLength];

        if (random == null) {
            // late-initialisation to support native build
            random = new Random(System.currentTimeMillis());
        }

        for (int i = 0; i < aLength; i++) {
            // select a group of characters
            int selection = random.nextInt(SELECTIONS.length);

            // select a character from the group
            int pos = random.nextInt(SELECTIONS[selection].length);

            // add it to the result
            result[i] = SELECTIONS[selection][pos];
        }

        return result;
    }
}
