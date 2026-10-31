import os
from typing import Protocol

class StorageFactory(Protocol):
    """Abstract Factory: one factory creates a FAMILY of products that must match each other."""
    def blob_store(self): ...
    def queue(self): ...
    def secrets(self): ...

class AwsFactory:
    def blob_store(self): return S3BlobStore()
    def queue(self): return SqsQueue()
    def secrets(self): return AwsSecretsManager()

class LocalFactory:                          # for development and tests
    def blob_store(self): return FileBlobStore("/tmp/blobs")
    def queue(self): return InMemoryQueue()
    def secrets(self): return EnvSecretStore()

# The app picks the family once at startup; nothing else knows which cloud it runs on.
infra: StorageFactory = LocalFactory() if os.environ.get("APP_ENV") == "local" else AwsFactory()
uploader = Uploader(infra.blob_store(), infra.queue())
