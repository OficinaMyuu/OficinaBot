resource "oci_load_balancer_load_balancer" "public" {
  compartment_id             = var.compartment_id
  display_name               = "${var.project_name}-public-lb"
  shape                      = "flexible"
  subnet_ids                 = [var.public_subnet_id]
  is_private                 = false
  ip_mode                    = "IPV4"
  network_security_group_ids = [var.lb_nsg_id]

  shape_details {
    minimum_bandwidth_in_mbps = var.lb_min_bandwidth_mbps
    maximum_bandwidth_in_mbps = var.lb_max_bandwidth_mbps
  }

  freeform_tags = var.common_tags
}

resource "oci_load_balancer_backend_set" "api" {
  load_balancer_id = oci_load_balancer_load_balancer.public.id
  name             = "api-backend-set"
  policy           = "ROUND_ROBIN"

  health_checker {
    protocol            = "HTTP"
    port                = var.api_port
    url_path            = var.api_health_path
    return_code         = var.health_return_code
    interval_ms         = var.health_interval_ms
    timeout_in_millis   = var.health_timeout_ms
    retries             = var.health_retries
    response_body_regex = ""
  }
}

resource "oci_load_balancer_backend" "api" {
  load_balancer_id = oci_load_balancer_load_balancer.public.id
  backendset_name  = oci_load_balancer_backend_set.api.name

  ip_address = var.api_private_ip
  port       = var.api_port

  backup  = false
  drain   = false
  offline = false
  weight  = 1
}

resource "oci_load_balancer_listener" "http" {
  load_balancer_id         = oci_load_balancer_load_balancer.public.id
  name                     = "http"
  default_backend_set_name = oci_load_balancer_backend_set.api.name
  port                     = var.lb_http_port
  protocol                 = "HTTP"

  connection_configuration {
    idle_timeout_in_seconds = 60
  }
}
