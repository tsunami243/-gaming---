export const stripHtml = (html) => {
  if (!html) return ''
  return html.replace(/<[^>]*>/g, '').replace(/&[^;]+;/g, ' ').trim()
}
