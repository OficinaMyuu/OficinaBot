variable "project_name" {
  type        = string
  description = "Application name used as the OCI resource name prefix."
}

variable "compartment_id" {
  type        = string
  description = "Compartment OCID where compute resources are created."
}

variable "common_tags" {
  type        = map(string)
  description = "Freeform tags applied to compute resources."
}

variable "availability_domain" {
  type        = string
  description = "Availability domain where the API instance is created."
}

variable "bots_availability_domain" {
  type        = string
  description = "Availability domain where the bot instance is created."
}

variable "api_compute_shape" {
  type        = string
  description = "Compute shape for the API instance."
}

variable "bots_compute_shape" {
  type        = string
  description = "Compute shape for the bot instance."
}

variable "bots_compute_ocpus" {
  type        = number
  description = "OCPU count for the bot instance flexible shape."
}

variable "bots_compute_memory_gbs" {
  type        = number
  description = "Memory in GB for the bot instance flexible shape."
}

variable "api_image_id" {
  type        = string
  description = "Image OCID used by the API instance."
}

variable "bots_image_id" {
  type        = string
  description = "Image OCID used by the bot instance."
}

variable "public_subnet_id" {
  type        = string
  description = "Public subnet OCID for the application instances."
}

variable "api_nsg_id" {
  type        = string
  description = "Network security group OCID for the API instance."
}

variable "bots_nsg_id" {
  type        = string
  description = "Network security group OCID for the bot instance."
}

variable "api_private_ip" {
  type        = string
  description = "Fixed private IP for the API instance."
}

variable "bots_private_ip" {
  type        = string
  description = "Fixed private IP for the bot instance."
}

variable "boot_volume_size_gbs" {
  type        = number
  description = "Boot volume size override."
  nullable    = true
}

variable "boot_volume_vpus_per_gb" {
  type        = number
  description = "Boot volume performance override."
  nullable    = true
}

variable "enable_in_transit_encryption" {
  type        = bool
  description = "Enable in-transit encryption for paravirtualized block volume attachment."
}

variable "ssh_public_key" {
  type        = string
  description = "SSH public key for VM access."
}
