variable "project_name" {
  type        = string
  description = "Application name used as the OCI resource name prefix."
  default     = "oficina-web"
}

variable "tenancy_ocid" {
  type        = string
  description = "OCI tenancy OCID."
  sensitive   = true
}

variable "user_ocid" {
  type        = string
  description = "OCI user OCID used by Terraform."
  sensitive   = true
}

variable "fingerprint" {
  type        = string
  description = "Fingerprint of the OCI API key."
  sensitive   = true
}

variable "private_key_path" {
  type        = string
  description = "Local path to the OCI API private key PEM file."
}

variable "region" {
  type        = string
  description = "OCI region identifier."
  default     = "us-ashburn-1"
}

variable "compartment_id" {
  type        = string
  description = "Compartment OCID where resources are created."
  sensitive   = true
}

variable "availability_domain_index" {
  type        = number
  description = "Zero-based availability domain index."
  default     = 2

  validation {
    condition     = var.availability_domain_index >= 0
    error_message = "availability_domain_index must be zero or greater."
  }
}

variable "bots_availability_domain_index" {
  type        = number
  description = "Zero-based availability domain index for the bots VM."
  default     = 1

  validation {
    condition     = var.bots_availability_domain_index >= 0
    error_message = "bots_availability_domain_index must be zero or greater."
  }
}

variable "vcn_dns_label" {
  type        = string
  description = "DNS label for the VCN."
  default     = "community"
}

variable "freeform_tags" {
  type        = map(string)
  description = "Extra freeform tags applied to OCI resources."
  default     = {}
}

variable "vcn_cidr" {
  type        = string
  description = "VCN CIDR block."
  default     = "10.0.0.0/16"
}

variable "public_subnet_cidr" {
  type        = string
  description = "Public subnet CIDR for application VMs and the load balancer."
  default     = "10.0.1.0/24"
}

variable "private_db_subnet_cidr" {
  type        = string
  description = "Private subnet CIDR for MySQL."
  default     = "10.0.2.0/24"
}

variable "api_private_ip" {
  type        = string
  description = "Fixed private IP for the API VM."
  default     = "10.0.1.10"
}

variable "bots_private_ip" {
  type        = string
  description = "Fixed private IP for the bots VM."
  default     = "10.0.1.11"
}

variable "mysql_private_ip" {
  type        = string
  description = "Fixed private IP for MySQL."
  default     = "10.0.2.10"
}

variable "api_port" {
  type        = number
  description = "Port where the API listens on the VM."
  default     = 8080
}

variable "api_health_path" {
  type        = string
  description = "API health-check path used by the load balancer."
  default     = "/health"
}

variable "mysql_port" {
  type        = number
  description = "MySQL classic protocol port."
  default     = 3306
}

variable "mysql_x_port" {
  type        = number
  description = "MySQL X protocol port."
  default     = 33060
}

variable "ssh_source_cidr" {
  type        = string
  description = "Public source CIDR allowed to SSH into the VMs."
}

variable "api_compute_shape" {
  type        = string
  description = "Compute shape for the API VM."
  default     = "VM.Standard.E2.1.Micro"

  validation {
    condition     = var.api_compute_shape == "VM.Standard.E2.1.Micro"
    error_message = "Only VM.Standard.E2.1.Micro is allowed for the API VM so it stays in the intended Always Free footprint."
  }
}

variable "bots_compute_shape" {
  type        = string
  description = "Compute shape for the bots VM."
  default     = "VM.Standard.A1.Flex"

  validation {
    condition     = var.bots_compute_shape == "VM.Standard.A1.Flex"
    error_message = "Only VM.Standard.A1.Flex is allowed for the bots VM so it stays in the intended Always Free footprint."
  }
}

variable "bots_compute_ocpus" {
  type        = number
  description = "OCPU count for the bots VM flexible shape."
  default     = 1

  validation {
    condition     = var.bots_compute_ocpus > 0 && var.bots_compute_ocpus <= 1
    error_message = "The bots VM must use at most 1 OCPU."
  }
}

variable "bots_compute_memory_gbs" {
  type        = number
  description = "Memory in GB for the bots VM flexible shape."
  default     = 6

  validation {
    condition     = var.bots_compute_memory_gbs > 0 && var.bots_compute_memory_gbs <= 6
    error_message = "The bots VM must use at most 6 GB of memory."
  }
}

variable "image_operating_system" {
  type        = string
  description = "OCI image operating system filter."
  default     = "Canonical Ubuntu"
}

variable "image_operating_system_version" {
  type        = string
  description = "OCI image operating system version filter."
  default     = "22.04"
}

variable "api_instance_image_ocid" {
  type        = string
  description = "Explicit API VM image OCID override. When null, Terraform searches for the latest matching Ubuntu image."
  default     = null
  nullable    = true
}

variable "bots_instance_image_ocid" {
  type        = string
  description = "Explicit bots VM image OCID override. When null, Terraform searches for the latest matching Ubuntu image."
  default     = null
  nullable    = true
}

variable "boot_volume_size_gbs" {
  type        = number
  description = "Boot volume size override."
  default     = 50
  nullable    = false

  validation {
    condition     = var.boot_volume_size_gbs == 50
    error_message = "Use exactly 50 GB to stay within the intended Always Free footprint and OCI minimum boot volume size."
  }
}

variable "boot_volume_vpus_per_gb" {
  type        = number
  description = "Boot volume performance override. Leave null to use the OCI default."
  default     = null
  nullable    = true

  validation {
    condition     = var.boot_volume_vpus_per_gb == null || var.boot_volume_vpus_per_gb == 10
    error_message = "Use the default volume performance or 10 VPUs/GB to avoid paid block volume performance settings."
  }
}

variable "enable_in_transit_encryption" {
  type        = bool
  description = "Enable in-transit encryption for paravirtualized block volume attachment."
  default     = true
}

variable "ssh_public_key" {
  type        = string
  description = "SSH public key for VM access."
}

variable "cloudflare_ipv4_cidrs" {
  type        = list(string)
  description = "Cloudflare IPv4 CIDR ranges allowed to reach the public load balancer."
  default = [
    "103.21.244.0/22",
    "103.22.200.0/22",
    "103.31.4.0/22",
    "104.16.0.0/13",
    "104.24.0.0/14",
    "108.162.192.0/18",
    "131.0.72.0/22",
    "141.101.64.0/18",
    "162.158.0.0/15",
    "172.64.0.0/13",
    "173.245.48.0/20",
    "188.114.96.0/20",
    "190.93.240.0/20",
    "197.234.240.0/22",
    "198.41.128.0/17",
  ]
}

variable "lb_min_bandwidth_mbps" {
  type        = number
  description = "Flexible load balancer minimum bandwidth."
  default     = 10

  validation {
    condition     = var.lb_min_bandwidth_mbps == 10
    error_message = "The OCI Always Free flexible load balancer allowance is 10 Mbps."
  }
}

variable "lb_max_bandwidth_mbps" {
  type        = number
  description = "Flexible load balancer maximum bandwidth."
  default     = 10

  validation {
    condition     = var.lb_max_bandwidth_mbps == 10
    error_message = "The OCI Always Free flexible load balancer allowance is 10 Mbps."
  }
}

variable "lb_http_port" {
  type        = number
  description = "Public HTTP listener port."
  default     = 80
}

variable "lb_https_port" {
  type        = number
  description = "Public HTTPS listener port."
  default     = 443
}

variable "lb_https_certificate_name" {
  type        = string
  description = "Name for the OCI load balancer certificate bundle. Change this when rotating certificate material."
  default     = "api-origin-2026-07"
}

variable "lb_https_public_certificate" {
  type        = string
  description = "PEM-encoded public Cloudflare Origin CA certificate for the API load balancer listener."
  sensitive   = true
}

variable "lb_https_private_key" {
  type        = string
  description = "PEM-encoded private key for the API load balancer HTTPS certificate."
  sensitive   = true
}

variable "lb_https_ca_certificate" {
  type        = string
  description = "Optional PEM-encoded CA certificate chain for the API load balancer HTTPS certificate."
  default     = null
  nullable    = true
  sensitive   = true
}

variable "lb_health_return_code" {
  type        = number
  description = "Expected health-check HTTP status."
  default     = 200
}

variable "lb_health_interval_ms" {
  type        = number
  description = "Load balancer health-check interval in milliseconds."
  default     = 10000
}

variable "lb_health_timeout_ms" {
  type        = number
  description = "Load balancer health-check timeout in milliseconds."
  default     = 3000
}

variable "lb_health_retries" {
  type        = number
  description = "Load balancer health-check retries."
  default     = 3
}

variable "mysql_shape_name" {
  type        = string
  description = "OCI MySQL shape."
  default     = "MySQL.Free"

  validation {
    condition     = var.mysql_shape_name == "MySQL.Free"
    error_message = "Only MySQL.Free is allowed so the DB system stays in OCI Always Free."
  }
}

variable "mysql_data_storage_size_gb" {
  type        = number
  description = "MySQL storage size in GB."
  default     = 50

  validation {
    condition     = var.mysql_data_storage_size_gb == 50
    error_message = "OCI Always Free MySQL uses the 50 GB free storage size."
  }
}

variable "mysql_admin_username" {
  type        = string
  description = "MySQL admin username."
}

variable "mysql_admin_password" {
  type        = string
  description = "MySQL admin password."
  sensitive   = true
}

variable "mysql_hostname_label" {
  type        = string
  description = "Private DNS hostname label for MySQL."
  default     = "mysql"
}

variable "mysql_delete_protected" {
  type        = bool
  description = "Prevent accidental MySQL deletion."
  default     = false
}
