// ✗ Swallowing: the failure disappears and the system carries on in a bad state
try { sendEmail(user); } catch (Exception e) { }

// ✗ Losing the cause: the stack trace that explains the failure is thrown away
catch (IOException e) { throw new RuntimeException("email failed"); }

// ✗ Log-and-rethrow at every layer: the same error appears five times in the logs
catch (IOException e) { log.error("failed", e); throw new EmailException(e); }

// ✓ Translate at a boundary, keep the cause, log once where it is finally handled
catch (IOException e) { throw new EmailDeliveryException("sending welcome email to " + user.id(), e); }

// ✓ Restore the interrupt flag if you catch InterruptedException and can't rethrow it
catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new CancellationException("interrupted"); }
