module "network" {
  source = "./modules/network"

  project_name           = var.project_name
  compartment_id         = var.compartment_id
  common_tags            = local.common_tags
  vcn_cidr               = var.vcn_cidr
  vcn_dns_label          = var.vcn_dns_label
  public_subnet_cidr     = var.public_subnet_cidr
  private_db_subnet_cidr = var.private_db_subnet_cidr
  api_port               = var.api_port
  mysql_port             = var.mysql_port
  lb_http_port           = var.lb_http_port
  lb_https_port          = var.lb_https_port
  cloudflare_ipv4_cidrs  = var.cloudflare_ipv4_cidrs
  ssh_source_cidr        = var.ssh_source_cidr
}

module "compute" {
  source = "./modules/compute"

  project_name                 = var.project_name
  compartment_id               = var.compartment_id
  common_tags                  = local.common_tags
  availability_domain          = local.selected_ad_name
  bots_availability_domain     = local.selected_bots_ad_name
  api_compute_shape            = var.api_compute_shape
  bots_compute_shape           = var.bots_compute_shape
  bots_compute_ocpus           = var.bots_compute_ocpus
  bots_compute_memory_gbs      = var.bots_compute_memory_gbs
  api_image_id                 = local.api_instance_image_id
  bots_image_id                = local.bots_instance_image_id
  public_subnet_id             = module.network.public_apps_subnet_id
  api_nsg_id                   = module.network.api_nsg_id
  bots_nsg_id                  = module.network.bots_nsg_id
  api_private_ip               = var.api_private_ip
  bots_private_ip              = var.bots_private_ip
  boot_volume_size_gbs         = var.boot_volume_size_gbs
  boot_volume_vpus_per_gb      = var.boot_volume_vpus_per_gb
  enable_in_transit_encryption = var.enable_in_transit_encryption
  ssh_public_key               = var.ssh_public_key
}

module "mysql" {
  source = "./modules/mysql"

  project_name               = var.project_name
  compartment_id             = var.compartment_id
  common_tags                = local.common_tags
  availability_domain        = local.selected_ad_name
  private_db_subnet_id       = module.network.private_db_subnet_id
  mysql_shape_name           = var.mysql_shape_name
  mysql_data_storage_size_gb = var.mysql_data_storage_size_gb
  mysql_admin_username       = var.mysql_admin_username
  mysql_admin_password       = var.mysql_admin_password
  mysql_hostname_label       = var.mysql_hostname_label
  mysql_private_ip           = var.mysql_private_ip
  mysql_port                 = var.mysql_port
  mysql_x_port               = var.mysql_x_port
  mysql_delete_protected     = var.mysql_delete_protected
}

module "load_balancer" {
  source = "./modules/load_balancer"

  project_name          = var.project_name
  compartment_id        = var.compartment_id
  common_tags           = local.common_tags
  public_subnet_id      = module.network.public_apps_subnet_id
  lb_nsg_id             = module.network.lb_nsg_id
  api_private_ip        = var.api_private_ip
  api_port              = var.api_port
  api_health_path       = var.api_health_path
  lb_min_bandwidth_mbps = var.lb_min_bandwidth_mbps
  lb_max_bandwidth_mbps = var.lb_max_bandwidth_mbps
  lb_http_port          = var.lb_http_port
  lb_https_port         = var.lb_https_port
  certificate_name      = var.lb_https_certificate_name
  public_certificate    = var.lb_https_public_certificate
  private_key           = var.lb_https_private_key
  ca_certificate        = var.lb_https_ca_certificate
  health_return_code    = var.lb_health_return_code
  health_interval_ms    = var.lb_health_interval_ms
  health_timeout_ms     = var.lb_health_timeout_ms
  health_retries        = var.lb_health_retries

  depends_on = [
    module.compute
  ]
}
