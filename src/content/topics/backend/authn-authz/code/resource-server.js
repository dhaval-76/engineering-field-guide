// Express + jose: verify against the issuer's JWKS (keys are cached and rotated
// automatically)
import { createRemoteJWKSet, jwtVerify } from 'jose';

const JWKS = createRemoteJWKSet(new URL('https://auth.example.com/.well-known/jwks.json'));

export async function requireAuth(req, res, next) {
  const token = req.get('authorization')?.replace(/^Bearer /, '');
  if (!token) return res.status(401).set('WWW-Authenticate', 'Bearer').end();
  try {
    const { payload } = await jwtVerify(token, JWKS, {
      issuer: 'https://auth.example.com',
      audience: 'orders-api',
      // never accept "none" or an algorithm you didn't expect
      algorithms: ['RS256'],
      clockTolerance: 30,
    });
    req.user = { id: payload.sub, scopes: String(payload.scope ?? '').split(' ') };
    next();
  } catch {
    res.status(401).set('WWW-Authenticate', 'Bearer error="invalid_token"').end();
  }
}

export const requireScope = (scope) => (req, res, next) =>
  req.user.scopes.includes(scope) ? next() : res.status(403).end();
