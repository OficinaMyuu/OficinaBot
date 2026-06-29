variable "project_name" {
  type        = string
  description = "Application name used as the OCI resource name prefix."
}

variable "compartment_id" {
  type        = string
  description = "Compartment OCID where MySQL is created."
}

variable "common_tags" {
  type        = map(string)
  description = "Freeform tags applied to MySQL resources."
}

variable "availability_domain" {
  type        = string
  description = "Availability domain where MySQL is created."
}

variable "private_db_subnet_id" {
  type        = string
  description = "Private subnet OCID for MySQL."
}

variable "mysql_shape_name" {
  type        = string
  description = "OCI MySQL shape."
}

variable "mysql_data_storage_size_gb" {
  type        = number
  description = "MySQL storage size in GB."
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
}

variable "mysql_private_ip" {
  type        = string
  description = "Fixed private IP for MySQL."
}

variable "mysql_port" {
  type        = number
  description = "MySQL classic protocol port."
}

variable "mysql_x_port" {
  type        = number
  description = "MySQL X protocol port."
}

variable "mysql_delete_protected" {
  type        = bool
  description = "Prevent accidental MySQL deletion."
}
