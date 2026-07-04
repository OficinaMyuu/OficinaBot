locals {
  protocol_tcp = "6"

  lb_ingress_ports = {
    http  = var.lb_http_port
    https = var.lb_https_port
  }

  cloudflare_lb_ingress_rules = merge([
    for cidr in var.cloudflare_ipv4_cidrs : {
      for name, port in local.lb_ingress_ports : "${cidr}-${name}" => {
        source = cidr
        port   = port
        name   = upper(name)
      }
    }
  ]...)
}
