import client from '../api/client'

export const aiService = {
  chat: (message, history = []) =>
    client.post('/assistant/chat', { message, history }).then((r) => r.data)
}