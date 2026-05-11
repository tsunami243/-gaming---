export const categories = [
  { id: 1, name: '三角洲行动', color: '#00A86B', key: 'delta-force', logo: 'delta-force' },
  { id: 2, name: '无畏契约', color: '#FF6B6B', key: 'valorant', logo: 'valorant' },
  { id: 3, name: 'Counter-Strike', color: '#FFB800', key: 'cs2', logo: 'cs2' },
  { id: 4, name: 'Apex Legends', color: '#A52A2A', key: 'apex', logo: 'apex' }
]

export const categoryOptions = categories.map(c => ({
  label: c.name,
  value: c.id
}))

export const getCategoryName = (id) => {
  const category = categories.find(c => c.id === id)
  return category?.name || '全部'
}

export const getCategoryColor = (id) => {
  const category = categories.find(c => c.id === id)
  return category?.color || '#1A1A1A'
}

export const getCategoryKey = (id) => {
  const category = categories.find(c => c.id === id)
  return category?.key || 'all'
}

export const gameLogos = {
  'delta-force': 'https://cdn.jsdelivr.net/npm/game-logos@1.0.0/delta-force.png',
  'valorant': 'https://cdn.jsdelivr.net/npm/game-logos@1.0.0/valorant.png',
  'cs2': 'https://cdn.jsdelivr.net/npm/game-logos@1.0.0/cs2.png',
  'apex': 'https://cdn.jsdelivr.net/npm/game-logos@1.0.0/apex.png'
}
