class EmailNotifier { send(to, msg) { /* SMTP */ } }
class SmsNotifier { send(to, msg) { /* SMS API */ } }

const creators = {
  email: () => new EmailNotifier(),
  sms: () => new SmsNotifier(),
};

export function createNotifier(channel) {
  const create = creators[channel];
  if (!create) throw new Error(`Unknown channel: ${channel}`);
  return create();
}
