// Levels, a dotted-name logger hierarchy with inherited levels, pluggable appenders and
// formatters.
import { AsyncLocalStorage } from 'node:async_hooks';

export const Level = { DEBUG: 10, INFO: 20, WARN: 30, ERROR: 40 };
const levelName = (n) => Object.keys(Level).find((k) => Level[k] === n);
export const context = new AsyncLocalStorage(); // per-request context, like an MDC

export const patternFormatter = (e) =>
  `${e.time.toISOString()} ${levelName(e.level).padEnd(5)} ${e.logger} - ${e.message}` +
  (Object.keys(e.context).length ? ` ${JSON.stringify(e.context)}` : '');

export const jsonFormatter = (e) => JSON.stringify({ ...e, level: levelName(e.level) });

export class ConsoleAppender {
  constructor(formatter = patternFormatter) {
    this.formatter = formatter;
  }
  append(e) {
    (e.level >= Level.WARN ? console.error : console.log)(this.formatter(e));
  }
}

export class Logger {
  level = null; // null = inherit
  appenders = [];
  additive = true;

  constructor(name, parent) {
    this.name = name;
    this.parent = parent;
  }

  effectiveLevel() {
    for (let l = this; l; l = l.parent) if (l.level !== null) return l.level;
    return Level.INFO;
  }

  log(level, message) {
    if (level < this.effectiveLevel()) return;
    const e = {
      time: new Date(),
      level,
      logger: this.name,
      message: typeof message === 'function' ? message() : message, // lazy messages
      context: { ...(context.getStore() ?? {}) },
    };
    for (let l = this; l; l = l.parent) {
      for (const a of l.appenders) a.append(e);
      if (!l.additive) break;
    }
  }

  debug(m) { this.log(Level.DEBUG, m); }
  info(m) { this.log(Level.INFO, m); }
  warn(m) { this.log(Level.WARN, m); }
  error(m) { this.log(Level.ERROR, m); }
}

const root = new Logger('root', null);
const loggers = new Map();

export function getLogger(name) {
  if (!name) return root;
  if (!loggers.has(name)) {
    const dot = name.lastIndexOf('.');
    loggers.set(name, new Logger(name, dot < 0 ? root : getLogger(name.slice(0, dot))));
  }
  return loggers.get(name);
}
