# Login with uniform timing and messages, rate limits, MFA; password resets with
# single-use hashed tokens. hash_password / verify_password from Authentication and
# authorization (scrypt) — argon2 works the same way.
import hashlib
import secrets

# a real hash: verifying costs the same as a real account
DUMMY_HASH = hash_password(secrets.token_hex(16))

class TooManyAttempts(Exception): pass
class BadCredentials(Exception): pass
class InvalidResetToken(Exception): pass

def sha256(s: str) -> str:
    return hashlib.sha256(s.encode()).hexdigest()

def login(email: str, password: str, ip: str):
    normalized = email.strip().lower()
    if not limits.try_ip(ip) or not limits.try_account(normalized):
        raise TooManyAttempts()
    user = users.find_by_email(normalized)
    # always run the slow hash
    ok = verify_password(password, user.password_hash if user else DUMMY_HASH)
    if user is None or not ok:
        limits.record_failure(normalized, ip)
        # one message for both cases
        raise BadCredentials("Invalid email or password")
    limits.reset(normalized)
    return {"mfa_required": sessions.pending_mfa(user)} if user.mfa_enabled else {"session": sessions.create(user, ip)}

# always answers "if it exists, we sent a link"
def request_reset(email: str) -> None:
    user = users.find_by_email(email.strip().lower())
    if user is None:
        return
    token = secrets.token_urlsafe(32)
    with pool.connection() as conn:
        conn.execute("INSERT INTO password_resets (token_hash, user_id, expires_at) "
                     # only the hash
                     "VALUES (%s, %s, now() + interval '15 minutes')", (sha256(token), user.id))
    mailer.send_reset(user.email, token)

def complete_reset(token: str, new_password: str) -> None:
    with pool.connection() as conn, conn.transaction():
        row = conn.execute("""UPDATE password_resets SET used_at = now()
                              WHERE token_hash = %s AND used_at IS NULL AND expires_at > now()
                              RETURNING user_id""", (sha256(token),)).fetchone()          # atomic single-use claim
        if row is None:
            raise InvalidResetToken()
        users.update_password_hash(conn, row[0], hash_password(new_password))
    # log out everywhere
    sessions.revoke_all(row[0])
