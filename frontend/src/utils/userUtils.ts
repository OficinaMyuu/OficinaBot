import { getDiscordDefaultAvatarUrl } from "@/config/discordUrls"

type AvatarContainer = {
  id: string
  avatar_url: string | null
}

type DiscordNameContainer = {
  display_name: string
  global_name: string | null
  username: string | null
}

export function getEffectiveAvatarUrl(user: AvatarContainer): string {
  return user.avatar_url || getDiscordDefaultAvatarUrl(Number(user.id) % 5)
}

export function getDiscordDisplayName(user: DiscordNameContainer): string {
  return user.global_name ?? user.username ?? user.display_name
}
