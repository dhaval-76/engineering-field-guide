record Sealed(byte[] wrappedKey, byte[] nonce, byte[] ciphertext) {}

/**
 * Envelope encryption: each record gets a fresh data key, and only the data key is encrypted
 * with the master key. In production the master key stays inside a KMS and this wrapping is a
 * KMS call (GenerateDataKey or Encrypt); here it is a local AES key so the example runs.
 */
Sealed seal(byte[] plaintext, SecretKey masterKey) throws GeneralSecurityException {
    KeyGenerator generator = KeyGenerator.getInstance("AES");
    generator.init(256);
    SecretKey dataKey = generator.generateKey();
    byte[] nonce = random(12); // must never repeat for the same key
    byte[] ciphertext = aesGcm(Cipher.ENCRYPT_MODE, dataKey, nonce, plaintext);
    byte[] keyNonce = random(12);
    byte[] wrapped = aesGcm(Cipher.ENCRYPT_MODE, masterKey, keyNonce, dataKey.getEncoded());
    return new Sealed(concat(keyNonce, wrapped), nonce, ciphertext); // stored with the data
}

byte[] open(Sealed sealed, SecretKey masterKey) throws GeneralSecurityException {
    byte[] keyNonce = Arrays.copyOfRange(sealed.wrappedKey(), 0, 12);
    byte[] wrapped = Arrays.copyOfRange(sealed.wrappedKey(), 12, sealed.wrappedKey().length);
    SecretKey dataKey = new SecretKeySpec(
            aesGcm(Cipher.DECRYPT_MODE, masterKey, keyNonce, wrapped), "AES");
    // throws AEADBadTagException if a single bit of the record was changed
    return aesGcm(Cipher.DECRYPT_MODE, dataKey, sealed.nonce(), sealed.ciphertext());
}

byte[] aesGcm(int mode, Key key, byte[] nonce, byte[] input) throws GeneralSecurityException {
    Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
    cipher.init(mode, key, new GCMParameterSpec(128, nonce)); // 128-bit tag detects tampering
    return cipher.doFinal(input); // ciphertext followed by the tag
}

byte[] random(int n) {
    byte[] b = new byte[n];
    new SecureRandom().nextBytes(b);
    return b;
}

byte[] concat(byte[] a, byte[] b) {
    byte[] out = Arrays.copyOf(a, a.length + b.length);
    System.arraycopy(b, 0, out, a.length, b.length);
    return out;
}
