// A module-level singleton that tests can still replace.
import { readFileSync } from 'node:fs';

let instance = null; // loaded lazily, once
let override = null; // test seam

export function featureFlags() {
  if (override) return override;
  instance ??= makeFlags(JSON.parse(readFileSync('flags.json', 'utf8')));
  return instance;
}

const makeFlags = (flags) => Object.freeze({ isOn: (name) => flags[name] === true });

export const overrideForTests = (flags) => { override = makeFlags(flags); };
export const resetForTests = () => { override = null; };
