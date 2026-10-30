// Abstract Factory: one factory creates a FAMILY of products that must match each other.
interface StorageFactory {
    BlobStore blobStore();
    Queue queue();
    SecretStore secrets();
}

final class AwsFactory implements StorageFactory {
    public BlobStore blobStore()   { return new S3BlobStore(); }
    public Queue queue()           { return new SqsQueue(); }
    public SecretStore secrets()   { return new AwsSecretsManager(); }
}

final class LocalFactory implements StorageFactory {       // for development and tests
    public BlobStore blobStore()   { return new FileBlobStore(Path.of("/tmp/blobs")); }
    public Queue queue()           { return new InMemoryQueue(); }
    public SecretStore secrets()   { return new EnvSecretStore(); }
}

// The app picks the family once at startup; nothing else knows which cloud it runs on.
StorageFactory infra = "local".equals(env) ? new LocalFactory() : new AwsFactory();
var uploader = new Uploader(infra.blobStore(), infra.queue());
