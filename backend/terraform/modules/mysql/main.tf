resource "oci_mysql_mysql_db_system" "mysql" {
  availability_domain = var.availability_domain
  compartment_id      = var.compartment_id
  display_name        = "${var.project_name}-mysql"
  description         = "Private MySQL DB for ${var.project_name}"
  shape_name          = var.mysql_shape_name
  subnet_id           = var.private_db_subnet_id

  admin_username = var.mysql_admin_username
  admin_password = var.mysql_admin_password

  data_storage_size_in_gb = var.mysql_data_storage_size_gb
  hostname_label          = var.mysql_hostname_label
  ip_address              = var.mysql_private_ip
  port                    = var.mysql_port
  port_x                  = var.mysql_x_port
  is_highly_available     = false

  deletion_policy {
    automatic_backup_retention = "DELETE"
    final_backup               = "SKIP_FINAL_BACKUP"
    is_delete_protected        = var.mysql_delete_protected
  }

  lifecycle {
    ignore_changes = [
      mysql_version
    ]
  }

  freeform_tags = var.common_tags
}
