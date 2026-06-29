output "api_vm_private_ip" {
  value = oci_core_instance.api.private_ip
}

output "api_vm_public_ip" {
  value = oci_core_instance.api.public_ip
}

output "bots_vm_private_ip" {
  value = oci_core_instance.bots.private_ip
}

output "bots_vm_public_ip" {
  value = oci_core_instance.bots.public_ip
}
