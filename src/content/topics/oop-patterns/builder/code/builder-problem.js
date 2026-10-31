// Positional arguments: which one is the phone number? What do null and true mean?
new User('Asha', null, null, 'asha@example.com', 28, true, null, 'ADMIN');

// JavaScript's usual fix is an options object — named, order-free, optional fields default:
new User({ name: 'Asha', email: 'asha@example.com', age: 28, verified: true, role: 'ADMIN' });
// A builder still helps when construction has steps, validation across fields, or must stay immutable.
