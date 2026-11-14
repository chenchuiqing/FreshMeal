import { readFile, readdir } from 'node:fs/promises';
import { join, resolve } from 'node:path';
import { describe, expect, it } from 'vitest';

async function collectVueFiles(directory: string): Promise<string[]> {
  const entries = await readdir(directory, { withFileTypes: true });
  const files = await Promise.all(
    entries.map((entry) => {
      const path = join(directory, entry.name);
      return entry.isDirectory() ? collectVueFiles(path) : [path];
    }),
  );
  return files.flat().filter((path) => path.endsWith('.vue'));
}

describe('rider XSS protection', () => {
  it('does not render raw HTML in Vue views', async () => {
    const viewsPath = resolve(process.cwd(), 'src/views');
    const files = await collectVueFiles(viewsPath);

    expect(files.length).toBeGreaterThan(0);
    for (const file of files) {
      const source = await readFile(file, 'utf8');
      expect(source, file).not.toMatch(/\bv-html\b/);
      expect(source, file).not.toMatch(/\binnerHTML\b/);
    }
  });
});
