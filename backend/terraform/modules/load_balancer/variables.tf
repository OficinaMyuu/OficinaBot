variable "project_name" {
  type        = string
  description = "Application name used as the OCI resource name prefix."
}

variable "compartment_id" {
  type        = string
  description = "Compartment OCID where load balancer resources are created."
}

variable "common_tags" {
  type        = map(string)
  description = "Freeform tags applied to load balancer resources."
}

variable "public_subnet_id" {
  type        = string
  description = "Public subnet OCID for the load balancer."
}

variable "lb_nsg_id" {
  type        = string
  description = "Network security group OCID for the load balancer."
}

variable "api_private_ip" {
  type        = string
  description = "Private IP for the API backend."
}

variable "api_port" {
  type        = number
  description = "API service port."
}

variable "api_health_path" {
  type        = string
  description = "API health-check path."
}

variable "lb_min_bandwidth_mbps" {
  type        = number
  description = "Flexible load balancer minimum bandwidth."
}

variable "lb_max_bandwidth_mbps" {
  type        = number
  description = "Flexible load balancer maximum bandwidth."
}

variable "lb_http_port" {
  type        = number
  description = "Public HTTP listener port."
}

variable "lb_https_port" {
  type        = number
  description = "Public HTTPS listener port."
}

variable "certificate_name" {
  type        = string
  description = "Name for the OCI load balancer certificate bundle."
}

variable "public_certificate" {
  type        = string
  description = "PEM-encoded public certificate for the HTTPS listener."
  sensitive   = true
}

variable "private_key" {
  type        = string
  description = "PEM-encoded private key for the HTTPS listener."
  sensitive   = true
}

variable "ca_certificate" {
  type        = string
  description = "Optional PEM-encoded CA certificate chain for the HTTPS listener."
  default     = null
  nullable    = true
  sensitive   = true
}

variable "health_return_code" {
  type        = number
  description = "Expected health-check HTTP status."
}

variable "health_interval_ms" {
  type        = number
  description = "Load balancer health-check interval in milliseconds."
}

variable "health_timeout_ms" {
  type        = number
  description = "Load balancer health-check timeout in milliseconds."
}

variable "health_retries" {
  type        = number
  description = "Load balancer health-check retries."
}
