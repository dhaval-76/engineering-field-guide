import { createHash } from "node:crypto";

const firstUserText = (e) => e.turns.find((t) => t.role === "user")?.content ?? "";
const normalize = (text) => text.trim().toLowerCase().replace(/\s+/g, " ");

/** Problems that would waste a training run. An empty list means the example is usable. */
export function validate(e) {
  if (e.turns.length === 0) return ["no turns"];
  const problems = [];
  if (!firstUserText(e).trim()) problems.push("no user message");
  const last = e.turns.at(-1);
  if (last.role !== "assistant" || !last.content.trim()) {
    problems.push("must end with a non-empty assistant message");
  }
  return problems;
}

/**
 * Splits by a hash of the question, so the same example always lands on the same side. A random
 * split lets near-identical questions leak into validation and inflate the score.
 */
export function split(examples, validationPercent) {
  const unique = new Map();
  for (const e of examples) {
    const key = normalize(firstUserText(e));
    if (!unique.has(key)) unique.set(key, e);
  }
  const train = [];
  const validation = [];
  for (const [key, e] of unique) {
    const bucket = createHash("sha256").update(key).digest()[0] % 100;
    (bucket < validationPercent ? validation : train).push(e);
  }
  return { train, validation };
}
