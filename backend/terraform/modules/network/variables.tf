variable "project_name" {
  type        = string
  description = "Application name used as the OCI resource name prefix."
}

variable "compartment_id" {
  type        = string
  description = "Compartment OCID where network resources are created."
}

variable "common_tags" {
  type        = map(string)
  description = "Freeform tags applied to network resources."
}

variable "vcn_cidr" {
  type        = string
  description = "VCN CIDR block."
}

variable "vcn_dns_label" {
  type        = string
  description = "DNS label for the VCN."
}

variable "public_subnet_cidr" {
  type        = string
  description = "Public subnet CIDR."
}

variable "private_db_subnet_cidr" {
  type        = string
  description = "Private database subnet CIDR."
}

variable "api_port" {
  type        = number
  description = "API service port."
}

variable "mysql_port" {
  type        = number
  description = "MySQL classic protocol port."
}

variable "lb_http_port" {
  type        = number
  description = "Public HTTP listener port."
}

variable "ssh_source_cidr" {
  type        = string
  description = "Source CIDR allowed to SSH into application VMs."
}
