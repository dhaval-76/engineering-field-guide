// Abstract Factory: one factory creates a FAMILY of products that must match each other.
const awsFactory = {
  blobStore: () => new S3BlobStore(),
  queue: () => new SqsQueue(),
  secrets: () => new AwsSecretsManager(),
};

const localFactory = {                       // for development and tests
  blobStore: () => new FileBlobStore('/tmp/blobs'),
  queue: () => new InMemoryQueue(),
  secrets: () => new EnvSecretStore(),
};

// The app picks the family once at startup; nothing else knows which cloud it runs on.
const infra = process.env.APP_ENV === 'local' ? localFactory : awsFactory;
const uploader = new Uploader(infra.blobStore(), infra.queue());
