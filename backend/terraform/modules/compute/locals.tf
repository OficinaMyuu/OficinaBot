locals {
  ssh_metadata = {
    ssh_authorized_keys = var.ssh_public_key
  }
}
