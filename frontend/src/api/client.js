const API_BASE = import.meta.env.VITE_API_BASE_URL ?? ''

async function request(path) {
  const response = await fetch(`${API_BASE}${path}`)
  if (!response.ok) throw new Error(`API error ${response.status}: ${path}`)
  return response.json()
}

export const fetchGalleries = () => request('/api/galleries')
export const fetchGallery   = (id) => request(`/api/galleries/${encodeURIComponent(id)}`)
