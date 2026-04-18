const API_BASE_URL = (
  import.meta.env.VITE_GATEWAY_BASE_URL ||
  import.meta.env.VITE_API_BASE_URL ||
  ''
).replace(/\/$/, '')

/**
 * Analyse a symptom description via the AI backend.
 * @param {string} symptoms  - Free-text symptom description
 * @param {string|null} userId - Optional patient identifier (X-User-ID header)
 * @returns {Promise<SymptomResponse>}
 */
export async function analyseSymptoms(symptoms, userId = null) {
  const headers = { 'Content-Type': 'application/json' }
  if (userId) headers['X-User-ID'] = userId

  const response = await fetch(`${API_BASE_URL}/ai/api/v1/symptoms/analyse`, {
    method: 'POST',
    credentials: 'include',
    headers,
    body: JSON.stringify({ symptoms }),
  })

  const contentType = response.headers.get('content-type') || ''
  const payload = contentType.includes('application/json')
    ? await response.json()
    : await response.text()

  if (!response.ok) {
    const message = typeof payload === 'string'
      ? payload
      : payload?.detail || payload?.message || payload?.error || `Request failed (${response.status})`
    throw new Error(message)
  }

  return payload
}

/**
 * @typedef {Object} SymptomResponse
 * @property {string}   request_id
 * @property {string|null} patient_id
 * @property {string}   category
 * @property {number}   confidence_score
 * @property {'high'|'medium'|'low'} confidence_tier
 * @property {string[]} suggestions
 * @property {string}   reasoning
 * @property {boolean}  verification_required
 * @property {boolean}  escalate_to_human
 * @property {object}   workflow_metadata
 */
