output "public_ip" {
  value = try(oci_load_balancer_load_balancer.public.ip_address_details[0].ip_address, null)
}

output "http_url_hint" {
  value = "http://${try(oci_load_balancer_load_balancer.public.ip_address_details[0].ip_address, "LB_IP_PENDING")}"
}

output "https_url_hint" {
  value = "https://${try(oci_load_balancer_load_balancer.public.ip_address_details[0].ip_address, "LB_IP_PENDING")}"
}
