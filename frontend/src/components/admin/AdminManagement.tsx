import React, { useState } from 'react'
import { useAuth } from '../../context/AuthContext'
import DashboardLayout from '../layout/DashboardLayout'
import Button from '../ui/Button'
import { Table, Thead, Tbody, Th, Td, Tr } from '../ui/Table'
import { FormField, Input } from '../ui/Form'
import Modal from '../ui/Modal'
import { useToast } from '../ui/Toast'
import { ErrorState } from '../ui/FeedbackStates'

interface AllowlistedAdmin {
  id: string
  username: string
  discordId: string
  addedBy: string
}

const initialAdmins: AllowlistedAdmin[] = [
  { id: '1', username: 'Leonardo#0001', discordId: '123456789012345678', addedBy: 'System Init' },
  {
    id: '2',
    username: 'Moderator#0001',
    discordId: '987654321098765432',
    addedBy: 'Leonardo#0001',
  },
]

export const AdminManagement: React.FC = () => {
  const { user } = useAuth()
  const { showToast } = useToast()
  const [admins, setAdmins] = useState<AllowlistedAdmin[]>(initialAdmins)

  // Form states
  const [newUsername, setNewUsername] = useState('')
  const [newDiscordId, setNewDiscordId] = useState('')
  const [formError, setFormError] = useState<{ username?: string; discordId?: string }>({})

  // Modal delete confirmation states
  const [selectedAdmin, setSelectedAdmin] = useState<AllowlistedAdmin | null>(null)
  const [isDeleteModalOpen, setIsDeleteModalOpen] = useState(false)

  // 1. Guard check: Only Owner has access
  if (!user?.isOwner) {
    return (
      <DashboardLayout pageTitle="Admin Management">
        <ErrorState
          title="Access Restrained"
          message="The admin user allowlist panel is strictly reserved for the Primary Server Owner (Leonardo#0001)."
        />
      </DashboardLayout>
    )
  }

  // 2. Owner-only allowlist panel
  const handleAddAdmin = (e: React.FormEvent) => {
    e.preventDefault()

    const errors: { username?: string; discordId?: string } = {}
    if (!newUsername) errors.username = 'Discord username is required.'
    if (!newDiscordId) {
      errors.discordId = 'Discord ID is required.'
    } else if (!/^\d{17,19}$/.test(newDiscordId)) {
      errors.discordId = 'Discord ID must be a numeric value containing 17 to 19 digits.'
    }

    if (Object.keys(errors).length > 0) {
      setFormError(errors)
      showToast('Validation failed. Please check allowlist details.', 'error')
      return
    }

    setFormError({})
    const newRecord: AllowlistedAdmin = {
      id: Math.random().toString(36).substring(2, 9),
      username: newUsername,
      discordId: newDiscordId,
      addedBy: user.username,
    }

    setAdmins([...admins, newRecord])
    setNewUsername('')
    setNewDiscordId('')
    showToast(`Successfully allowlisted admin: ${newUsername}`, 'success')
  }

  const triggerDeleteConfirm = (admin: AllowlistedAdmin) => {
    // Avoid self-deletion
    if (admin.discordId === '123456789012345678') {
      showToast('Action forbidden: cannot remove primary owner.', 'error')
      return
    }
    setSelectedAdmin(admin)
    setIsDeleteModalOpen(true)
  }

  const handleDeleteAdmin = () => {
    if (!selectedAdmin) return

    setAdmins(admins.filter((a) => a.id !== selectedAdmin.id))
    showToast(`Removed admin: ${selectedAdmin.username} from allowlist.`, 'warning')
    setIsDeleteModalOpen(false)
    setSelectedAdmin(null)
  }

  return (
    <DashboardLayout pageTitle="Admin Management">
      <div style={{ display: 'flex', flexDirection: 'column', gap: '24px' }}>
        <div
          style={{
            display: 'grid',
            gridTemplateColumns: 'repeat(auto-fit, minmax(320px, 1fr))',
            gap: '24px',
            alignItems: 'start',
          }}
        >
          {/* Add Allowlisted User Form */}
          <form
            onSubmit={handleAddAdmin}
            style={{
              backgroundColor: 'var(--bg-panel)',
              border: '1px solid var(--border-medium)',
              borderRadius: '8px',
              padding: '20px',
              display: 'flex',
              flexDirection: 'column',
              gap: '16px',
            }}
          >
            <h3
              style={{
                margin: 0,
                color: 'var(--text-primary)',
                fontSize: '16px',
                fontWeight: 'bold',
              }}
            >
              Allowlist New Admin
            </h3>
            <p
              style={{
                margin: 0,
                fontSize: '13px',
                color: 'var(--text-secondary)',
                lineHeight: '1.4',
              }}
            >
              Add a moderator or co-admin. Allowlisted users will be authorized to access the
              dashboard panels upon logging in with Discord.
            </p>

            <FormField label="Discord Tag / Username" error={formError.username}>
              <Input
                value={newUsername}
                onChange={(e) => setNewUsername(e.target.value)}
                placeholder="e.g. NewMod#0001 or tag"
                hasError={!!formError.username}
              />
            </FormField>

            <FormField
              label="Discord User ID"
              error={formError.discordId}
              helperText="Numeric ID. Enable Developer Mode on Discord to copy."
            >
              <Input
                value={newDiscordId}
                onChange={(e) => setNewDiscordId(e.target.value)}
                placeholder="e.g. 123456789012345678"
                hasError={!!formError.discordId}
              />
            </FormField>

            <Button type="submit">Allowlist User</Button>
          </form>

          {/* Current Allowlist Table */}
          <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
            <h3
              style={{
                margin: 0,
                color: 'var(--text-primary)',
                fontSize: '16px',
                fontWeight: 'bold',
              }}
            >
              Allowlisted Administrators
            </h3>

            <Table>
              <Thead>
                <Tr>
                  <Th>Admin Account</Th>
                  <Th>User ID</Th>
                  <Th style={{ width: '80px', textAlign: 'center' }}>Action</Th>
                </Tr>
              </Thead>
              <Tbody>
                {admins.map((admin) => (
                  <Tr key={admin.id}>
                    <Td style={{ fontWeight: 'bold' }}>
                      {admin.username}
                      {admin.discordId === '123456789012345678' && (
                        <span
                          style={{
                            marginLeft: '8px',
                            padding: '1px 6px',
                            borderRadius: '4px',
                            fontSize: '10px',
                            backgroundColor: 'rgba(124, 58, 237, 0.15)',
                            color: 'var(--color-secondary)',
                            border: '1px solid rgba(124, 58, 237, 0.3)',
                            fontWeight: 'bold',
                          }}
                        >
                          Owner
                        </span>
                      )}
                    </Td>
                    <Td style={{ fontFamily: 'monospace', color: 'var(--text-muted)' }}>
                      {admin.discordId}
                    </Td>
                    <Td style={{ textAlign: 'center' }}>
                      {admin.discordId !== '123456789012345678' ? (
                        <Button
                          variant="ghost"
                          size="sm"
                          onClick={() => triggerDeleteConfirm(admin)}
                          style={{ color: 'var(--color-danger)', padding: '4px' }}
                          title="Revoke dashboard permissions"
                        >
                          Revoke
                        </Button>
                      ) : (
                        <span style={{ fontSize: '12px', color: 'var(--text-muted)' }}>Lock</span>
                      )}
                    </Td>
                  </Tr>
                ))}
              </Tbody>
            </Table>
          </div>
        </div>
      </div>

      {/* Delete Confirmation Modal */}
      <Modal
        isOpen={isDeleteModalOpen}
        onClose={() => setIsDeleteModalOpen(false)}
        title="Revoke Admin Permissions"
        footer={
          <>
            <Button variant="secondary" onClick={() => setIsDeleteModalOpen(false)}>
              Cancel
            </Button>
            <Button variant="danger" onClick={handleDeleteAdmin}>
              Revoke Allowlist Authorization
            </Button>
          </>
        }
      >
        <p style={{ margin: 0 }}>
          Are you sure you want to remove <strong>{selectedAdmin?.username}</strong> (
          {selectedAdmin?.discordId}) from the administrative allowlist?
        </p>
        <p style={{ margin: '8px 0 0 0', color: 'var(--color-danger)', fontSize: '13px' }}>
          ⚠️ This action takes effect immediately. The user will be blocked from logging into the
          dashboard.
        </p>
      </Modal>
    </DashboardLayout>
  )
}

export default AdminManagement
