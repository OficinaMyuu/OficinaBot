import { useEffect, useRef, useState } from "react"
import { useQuery } from "@tanstack/react-query"
import { useTranslation } from "react-i18next"
import { messageService } from "@/services/messageService"

import styles from "./Sticker.module.css"

export function Sticker({ stickerId }: { stickerId: string }) {
  const { t } = useTranslation()
  const [format, setFormat] = useState<"image" | "lottie">("image")
  const containerRef = useRef<HTMLDivElement>(null)
  const label = t("messages.sticker", { id: stickerId })
  const lottieQuery = useQuery({
    queryKey: ["discord-sticker-lottie", stickerId],
    queryFn: () => messageService.lottieSticker(stickerId),
    enabled: format === "lottie",
    staleTime: Infinity
  })

  useEffect(() => {
    const container = containerRef.current
    const animationData = lottieQuery.data
    if (!container || !animationData) return
    let destroyed = false
    let animation: import("lottie-web").AnimationItem | undefined
    void import("lottie-web/build/player/lottie_light").then(
      ({ default: lottie }) => {
        if (destroyed) return
        const reducedMotion = window.matchMedia(
          "(prefers-reduced-motion: reduce)"
        ).matches
        animation = lottie.loadAnimation({
          animationData: structuredClone(animationData),
          autoplay: !reducedMotion,
          container,
          loop: !reducedMotion,
          renderer: "svg"
        })
        if (reducedMotion) animation.goToAndStop(0, true)
      }
    )
    return () => {
      destroyed = true
      animation?.destroy()
    }
  }, [lottieQuery.data])

  if (format === "image") {
    return (
      <img
        className={styles.sticker}
        src={`https://cdn.discordapp.com/stickers/${stickerId}.png`}
        onError={() => setFormat("lottie")}
        alt={label}
        draggable={false}
      />
    )
  }

  if (lottieQuery.isError) {
    return <div className={styles.unavailable}>{label}</div>
  }

  return (
    <div
      ref={containerRef}
      className={styles.sticker}
      role="img"
      aria-label={label}
    />
  )
}
