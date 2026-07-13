/**
 * Discord guild permission bits returned by the OAuth guilds endpoint.
 *
 * Keep these values as bigint: Discord serializes permissions as strings and
 * permission bits above Number.MAX_SAFE_INTEGER must remain exact.
 */
export const DiscordPermission = {
  CreateInstantInvite: 1n << 0n,
  KickMembers: 1n << 1n,
  BanMembers: 1n << 2n,
  Administrator: 1n << 3n,
  ManageChannels: 1n << 4n,
  ManageGuild: 1n << 5n,
  AddReactions: 1n << 6n,
  ViewAuditLog: 1n << 7n,
  PrioritySpeaker: 1n << 8n,
  Stream: 1n << 9n,
  ViewChannel: 1n << 10n,
  SendMessages: 1n << 11n,
  SendTtsMessages: 1n << 12n,
  ManageMessages: 1n << 13n,
  EmbedLinks: 1n << 14n,
  AttachFiles: 1n << 15n,
  ReadMessageHistory: 1n << 16n,
  MentionEveryone: 1n << 17n,
  UseExternalEmojis: 1n << 18n,
  ViewGuildInsights: 1n << 19n,
  Connect: 1n << 20n,
  Speak: 1n << 21n,
  MuteMembers: 1n << 22n,
  DeafenMembers: 1n << 23n,
  MoveMembers: 1n << 24n,
  UseVad: 1n << 25n,
  ChangeNickname: 1n << 26n,
  ManageNicknames: 1n << 27n,
  ManageRoles: 1n << 28n,
  ManageWebhooks: 1n << 29n,
  ManageGuildExpressions: 1n << 30n,
  UseApplicationCommands: 1n << 31n,
  RequestToSpeak: 1n << 32n,
  ManageEvents: 1n << 33n,
  ManageThreads: 1n << 34n,
  CreatePublicThreads: 1n << 35n,
  CreatePrivateThreads: 1n << 36n,
  UseExternalStickers: 1n << 37n,
  SendMessagesInThreads: 1n << 38n,
  UseEmbeddedActivities: 1n << 39n,
  ModerateMembers: 1n << 40n,
  ViewCreatorMonetizationAnalytics: 1n << 41n,
  UseSoundboard: 1n << 42n,
  CreateGuildExpressions: 1n << 43n,
  CreateEvents: 1n << 44n,
  UseExternalSounds: 1n << 45n,
  SendVoiceMessages: 1n << 46n,
  SetVoiceChannelStatus: 1n << 48n,
  SendPolls: 1n << 49n,
  UseExternalApps: 1n << 50n,
  PinMessages: 1n << 51n,
  BypassSlowmode: 1n << 52n
} as const satisfies Record<string, bigint>

export type DiscordPermissionFlag = (typeof DiscordPermission)[keyof typeof DiscordPermission]

const DECIMAL_PERMISSION_PATTERN = /^\d+$/

export class DiscordPermissions {
  readonly value: bigint

  constructor(value: bigint | string) {
    this.value = parsePermissionValue(value)
  }

  has(permission: DiscordPermissionFlag): boolean {
    return this.isAdmin() || this.hasRaw(permission)
  }

  hasAny(...permissions: readonly DiscordPermissionFlag[]): boolean {
    return permissions.some((permission) => this.has(permission))
  }

  hasAll(...permissions: readonly DiscordPermissionFlag[]): boolean {
    return permissions.every((permission) => this.has(permission))
  }

  isAdmin(): boolean {
    return this.hasRaw(DiscordPermission.Administrator)
  }

  isServerManager(): boolean {
    return this.has(DiscordPermission.ManageGuild)
  }

  toString(): string {
    return this.value.toString()
  }

  private hasRaw(permission: DiscordPermissionFlag): boolean {
    return (this.value & permission) === permission
  }
}

function parsePermissionValue(value: bigint | string): bigint {
  if (typeof value === "bigint") {
    if (value < 0n) {
      throw new TypeError("Discord permissions cannot be negative")
    }
    return value
  }

  if (!DECIMAL_PERMISSION_PATTERN.test(value)) {
    throw new TypeError(
      "Discord permissions must be an unsigned decimal string"
    )
  }

  return BigInt(value)
}
