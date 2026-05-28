import '@testing-library/jest-dom'
import { cleanup } from '@testing-library/react'
import { afterEach } from 'vitest'

// Clean up after each test (like clearing state, unmounting React trees)
afterEach(() => {
  cleanup()
})
