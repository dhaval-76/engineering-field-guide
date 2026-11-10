import os

# pip install cryptography: Python's standard library has no AES.
from cryptography.hazmat.primitives.ciphers.aead import AESGCM


def seal(plaintext: bytes, master_key: bytes) -> dict:
    """Envelope encryption: each record gets a fresh data key, and only the data key is
    encrypted with the master key. In production the master key stays inside a KMS and this
    wrapping is a KMS call (GenerateDataKey or Encrypt); here it is a local AES key."""
    data_key = AESGCM.generate_key(bit_length=256)
    nonce = os.urandom(12)  # must never repeat for the same key
    ciphertext = AESGCM(data_key).encrypt(nonce, plaintext, None)  # ciphertext + 16-byte tag
    key_nonce = os.urandom(12)
    wrapped_key = key_nonce + AESGCM(master_key).encrypt(key_nonce, data_key, None)
    return {"wrapped_key": wrapped_key, "nonce": nonce, "ciphertext": ciphertext}


def open_sealed(sealed: dict, master_key: bytes) -> bytes:
    wrapped = sealed["wrapped_key"]
    data_key = AESGCM(master_key).decrypt(wrapped[:12], wrapped[12:], None)
    # raises InvalidTag if a single bit of the record was changed
    return AESGCM(data_key).decrypt(sealed["nonce"], sealed["ciphertext"], None)
