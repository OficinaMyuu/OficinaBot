const BASE_AVATAR_URL = "https://cdn.discordapp.com/embed/avatars"

type AvatarContainer = {
  id: string
  avatar_url: string | null
}

export function getEffectiveAvatarUrl(user: AvatarContainer): string {
  return user.avatar_url || `${BASE_AVATAR_URL}/${Number(user.id) % 5}.png`
}
