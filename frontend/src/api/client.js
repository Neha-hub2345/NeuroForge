import axios from 'axios'

const baseURL = import.meta.env.VITE_API_URL || 'http://localhost:9000/api'

const client = axios.create({
  baseURL,
  headers: {
    'Content-Type': 'application/json'
  }
})

// Normalize error messages coming from the backend's GlobalExceptionHandler
// ({ error: "..." }) into a plain string so callers can just do
// catch (err) { setError(err.message) }
client.interceptors.response.use(
  (res) => res,
  (err) => {
    const backendMessage = err?.response?.data?.error
    const message = backendMessage || err.message || 'Something went wrong'
    return Promise.reject(new Error(message))
  }
)

export default client
