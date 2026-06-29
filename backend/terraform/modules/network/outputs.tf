output "vcn_id" {
  value = oci_core_vcn.main.id
}

output "public_apps_subnet_id" {
  value = oci_core_subnet.public_apps.id
}

output "private_db_subnet_id" {
  value = oci_core_subnet.private_db.id
}

output "lb_nsg_id" {
  value = oci_core_network_security_group.lb.id
}

output "api_nsg_id" {
  value = oci_core_network_security_group.api.id
}

output "bots_nsg_id" {
  value = oci_core_network_security_group.bots.id
}
