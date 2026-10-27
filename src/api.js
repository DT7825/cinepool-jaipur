const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';

export async function publicApiFetch(path, options = {}) {
  const response = await fetch(new URL(path, API_BASE_URL), options);
  if (!response.ok) {
    let message = `API request failed (${response.status}).`;
    try {
      const body = await response.json();
      if (typeof body.message === 'string') message = body.message;
      else if (typeof body.detail === 'string') message = body.detail;
    } catch {
      // Keep the generic response message if the server did not return JSON.
    }
    throw new Error(message);
  }
  return response.status === 204 ? null : response.json();
}

// Pass Clerk's getToken function from useAuth(); the API validates this bearer
// token with Clerk's public JWKS before it accepts any /api/** request.
export async function clerkApiFetch(path, getToken, options = {}) {
  const token = await getToken();
  if (!token) throw new Error('Sign in is required to access this API.');
  const headers = new Headers(options.headers || {});
  headers.set('Authorization', `Bearer ${token}`);
  if (options.body && !headers.has('Content-Type')) headers.set('Content-Type', 'application/json');
  const response = await fetch(new URL(path, API_BASE_URL), { ...options, headers });
  if (!response.ok) {
    let message = `API request failed (${response.status}).`;
    try {
      const body = await response.json();
      if (typeof body.message === 'string') message = body.message;
      else if (typeof body.detail === 'string') message = body.detail;
      else if (typeof body.error === 'string') message = body.error;
    } catch {
      // Keep the generic status message for an empty or non-JSON API error.
    }
    throw new Error(message);
  }
  return response.status === 204 ? null : response.json();
}
