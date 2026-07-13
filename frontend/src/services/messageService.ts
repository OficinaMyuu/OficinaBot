import { apiClient } from "./apiClient"
import type {
  ChannelMessagesPage,
  ChannelMessagesQuery,
  MessageVersionsResponse
} from "@/types/message"

export const messageService = {
  list(
    channelId: string,
    query: ChannelMessagesQuery = {}
  ): Promise<ChannelMessagesPage> {
    const params = new URLSearchParams()
    if (query.limit !== undefined) params.set("limit", String(query.limit))
    if (query.before) params.set("before", query.before)
    if (query.after) params.set("after", query.after)
    if (query.around) params.set("around", query.around)
    const suffix = params.size > 0 ? `?${params.toString()}` : ""
    return apiClient.get<ChannelMessagesPage>(
      `/channels/${channelId}/messages${suffix}`
    )
  },

  versions(
    channelId: string,
    messageId: string
  ): Promise<MessageVersionsResponse> {
    return apiClient.get<MessageVersionsResponse>(
      `/channels/${channelId}/messages/${messageId}/versions`
    )
  },

  lottieSticker(stickerId: string): Promise<Record<string, unknown>> {
    return apiClient.get<Record<string, unknown>>(
      `/discord/stickers/${stickerId}/lottie`
    )
  }
}
