const DEFAULT_ENV: ViteEnv = {
  VITE_API_BASE_URL: '/api',
  VITE_API_TARGET: 'http://127.0.0.1:8080',
  VITE_API_PROXY_REWRITE: false,
  VITE_REQUEST_ID_ENABLED: true,
  VITE_PORT: 5175,
  VITE_USE_COMPRESS: false,
};

const booleanKeys = new Set<keyof ViteEnv>([
  'VITE_API_PROXY_REWRITE',
  'VITE_REQUEST_ID_ENABLED',
  'VITE_USE_COMPRESS',
]);

function parseEnvValue(
  envName: string,
  value: string,
): string | boolean | number {
  const normalized = value.replace(/\\n/g, '\n');
  if (booleanKeys.has(envName as keyof ViteEnv)) return normalized === 'true';
  if (envName === 'VITE_PORT') return Number(normalized);
  return normalized;
}

export function wrapperEnv(envConf: Recordable<string>): ViteEnv {
  const result: ViteEnv = { ...DEFAULT_ENV };

  for (const envName of Object.keys(envConf)) {
    const value = parseEnvValue(envName, envConf[envName] ?? '');
    result[envName as keyof ViteEnv] = value as never;
  }

  return result;
}
