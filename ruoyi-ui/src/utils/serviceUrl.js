const localHosts = ['localhost', '127.0.0.1']

export function resolveServiceUrl(configuredUrl, fallbackUrl) {
  const rawUrl = configuredUrl || fallbackUrl
  if (typeof window === 'undefined') return rawUrl

  const url = new URL(rawUrl, window.location.origin)
  if (localHosts.includes(url.hostname) && localHosts.includes(window.location.hostname)) {
    url.hostname = window.location.hostname
  }
  return url.toString()
}
