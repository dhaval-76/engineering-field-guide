// JS has try/catch/finally; there are no checked exceptions.
async function readConfig(path) {
  const file = await fs.open(path);           // node:fs/promises
  try {
    return JSON.parse(await file.readFile('utf8'));
  } catch (err) {
    throw new Error(`invalid config ${path}`, { cause: err });   // keep the cause
  } finally {
    await file.close();
  }
}
// Newer runtimes support `await using file = await fs.open(path)` (explicit resource management),
// the equivalent of try-with-resources.
