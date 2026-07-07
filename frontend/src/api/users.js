import client from './client'

export const usersApi = {
  getAll: () => client.get('/users').then((r) => r.data),
  getById: (id) => client.get(`/users/${id}`).then((r) => r.data),
  assignRole: (id, role) => client.post(`/users/${id}/assignRole`, { role }).then((r) => r.data),
  assignTeam: (id, teamId) =>
    client.post(`/users/${id}/assignTeam`, { teamId }).then((r) => r.data)
}
