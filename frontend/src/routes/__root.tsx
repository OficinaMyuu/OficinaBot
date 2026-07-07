/* eslint-disable react-refresh/only-export-components */
import { Outlet, createRootRoute } from '@tanstack/react-router'

export const Route = createRootRoute({
  component: RootRoute,
})

function RootRoute() {
  return <Outlet />
}
