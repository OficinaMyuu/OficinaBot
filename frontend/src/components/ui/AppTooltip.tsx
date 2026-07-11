import * as Tooltip from "@radix-ui/react-tooltip"
import type { ReactElement } from "react"
import styles from "./AppTooltip.module.css"

type AppTooltipProps = {
  children: ReactElement
  imageAlt?: string
  imageSrc?: string
  label: string
  side?: "top" | "right" | "bottom" | "left"
}

export function AppTooltip({
  children,
  imageAlt = "",
  imageSrc,
  label,
  side = "top"
}: AppTooltipProps) {
  return (
    <Tooltip.Provider delayDuration={300}>
      <Tooltip.Root>
        <Tooltip.Trigger asChild>{children}</Tooltip.Trigger>
        <Tooltip.Portal>
          <Tooltip.Content
            className={styles.content}
            side={side}
            sideOffset={6}
          >
            {imageSrc ? (
              <img className={styles.image} src={imageSrc} alt={imageAlt} />
            ) : null}
            <span>{label}</span>
            <Tooltip.Arrow className={styles.arrow} />
          </Tooltip.Content>
        </Tooltip.Portal>
      </Tooltip.Root>
    </Tooltip.Provider>
  )
}
