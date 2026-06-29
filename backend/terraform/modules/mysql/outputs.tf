output "mysql_private_ip" {
  value = var.mysql_private_ip
}

output "mysql_system_id" {
  value = oci_mysql_mysql_db_system.mysql.id
}
