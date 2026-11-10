import { createCipheriv, createDecipheriv, randomBytes } from 'node:crypto';

/**
 * Envelope encryption: each record gets a fresh data key, and only the data key is encrypted
 * with the master key. In production the master key stays inside a KMS and this wrapping is a
 * KMS call (GenerateDataKey or Encrypt); here it is a local AES key so the example runs.
 */
function seal(plaintext, masterKey) {
  const dataKey = randomBytes(32);
  const nonce = randomBytes(12); // must never repeat for the same key
  const ciphertext = aesGcmEncrypt(dataKey, nonce, plaintext);
  const keyNonce = randomBytes(12);
  const wrappedKey = Buffer.concat([keyNonce, aesGcmEncrypt(masterKey, keyNonce, dataKey)]);
  return { wrappedKey, nonce, ciphertext }; // stored with the data
}

function open({ wrappedKey, nonce, ciphertext }, masterKey) {
  const dataKey = aesGcmDecrypt(masterKey, wrappedKey.subarray(0, 12), wrappedKey.subarray(12));
  return aesGcmDecrypt(dataKey, nonce, ciphertext); // throws if a single bit was changed
}

/** Returns ciphertext followed by the 16-byte tag, the same layout Java and Python use. */
function aesGcmEncrypt(key, nonce, plaintext) {
  const cipher = createCipheriv('aes-256-gcm', key, nonce);
  return Buffer.concat([cipher.update(plaintext), cipher.final(), cipher.getAuthTag()]);
}

function aesGcmDecrypt(key, nonce, sealed) {
  const decipher = createDecipheriv('aes-256-gcm', key, nonce);
  decipher.setAuthTag(sealed.subarray(sealed.length - 16)); // the tag detects tampering
  return Buffer.concat([decipher.update(sealed.subarray(0, sealed.length - 16)), decipher.final()]);
}
