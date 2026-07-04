output "selected_availability_domain" {
  value = local.selected_ad_name
}

output "api_vm_private_ip" {
  value = module.compute.api_vm_private_ip
}

output "api_vm_public_ip" {
  value = module.compute.api_vm_public_ip
}

output "bots_vm_private_ip" {
  value = module.compute.bots_vm_private_ip
}

output "bots_vm_public_ip" {
  value = module.compute.bots_vm_public_ip
}

output "mysql_private_ip" {
  value = module.mysql.mysql_private_ip
}

output "mysql_private_host_hint" {
  value = "${var.mysql_hostname_label}.db.${var.vcn_dns_label}.oraclevcn.com"
}

output "load_balancer_public_ip" {
  value = module.load_balancer.public_ip
}

output "api_http_url_hint" {
  value = module.load_balancer.http_url_hint
}

output "api_https_url_hint" {
  value = module.load_balancer.https_url_hint
}
