import { Group, Panel, Separator } from "react-resizable-panels"
import { DashboardHeader } from "./DashboardHeader"
import { DashboardSidebar } from "./DashboardSidebar"

import styles from "./DashboardLayout.module.css"

type DashboardLayoutProps = {
  children: React.ReactNode
  title: string
}

export function DashboardLayout({ children, title }: DashboardLayoutProps) {
  return (
    <div className={styles.shell}>
      <Group orientation="horizontal" disableCursor>
        <Panel
          defaultSize={282}
          minSize={240}
          maxSize={400}
          className={styles.sidebarPanel}
        >
          <DashboardSidebar />
        </Panel>

        <Separator className={styles.resizeHandle} />

        <Panel minSize={30}>
          <div className={styles.workspace}>
            <DashboardHeader title={title} />
            <main className={styles.content}>{children}</main>
          </div>
        </Panel>
      </Group>
    </div>
  )
}
