export const categories = [
  { id: 5, name: '三角洲行动', color: '#00A86B', key: 'delta-force', logo: 'delta-force' },
  { id: 8, name: '无畏契约', color: '#FF6B6B', key: 'valorant', logo: 'valorant' },
  { id: 9, name: 'Counter-Strike', color: '#FFB800', key: 'cs2', logo: 'cs2' },
  { id: 10, name: 'Apex Legends', color: '#A52A2A', key: 'apex', logo: 'apex' }
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
  'delta-force': new URL('../assets/logos/三角洲行动.png', import.meta.url).href,
  'valorant': new URL('../assets/logos/无畏契约.jpg', import.meta.url).href,
  'cs2': new URL('../assets/logos/cs2.png', import.meta.url).href,
  'apex': new URL('../assets/logos/apex.jpg', import.meta.url).href
}
