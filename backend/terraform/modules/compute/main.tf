resource "oci_core_instance" "api" {
  availability_domain = var.availability_domain
  compartment_id      = var.compartment_id
  display_name        = "${var.project_name}-api-vm"
  shape               = var.api_compute_shape

  is_pv_encryption_in_transit_enabled = var.enable_in_transit_encryption
  preserve_boot_volume                = false

  create_vnic_details {
    subnet_id                 = var.public_subnet_id
    assign_public_ip          = true
    assign_private_dns_record = true
    private_ip                = var.api_private_ip
    hostname_label            = "api"
    nsg_ids                   = [var.api_nsg_id]
  }

  instance_options {
    are_legacy_imds_endpoints_disabled = true
  }

  metadata = local.ssh_metadata

  source_details {
    source_type             = "image"
    source_id               = var.api_image_id
    boot_volume_size_in_gbs = var.boot_volume_size_gbs
    boot_volume_vpus_per_gb = var.boot_volume_vpus_per_gb
  }

  freeform_tags = var.common_tags
}

resource "oci_core_instance" "bots" {
  availability_domain = var.bots_availability_domain
  compartment_id      = var.compartment_id
  display_name        = "${var.project_name}-bots-vm"
  shape               = var.bots_compute_shape

  is_pv_encryption_in_transit_enabled = var.enable_in_transit_encryption
  preserve_boot_volume                = false

  create_vnic_details {
    subnet_id                 = var.public_subnet_id
    assign_public_ip          = true
    assign_private_dns_record = true
    private_ip                = var.bots_private_ip
    hostname_label            = "bots"
    nsg_ids                   = [var.bots_nsg_id]
  }

  instance_options {
    are_legacy_imds_endpoints_disabled = true
  }

  metadata = local.ssh_metadata

  shape_config {
    ocpus         = var.bots_compute_ocpus
    memory_in_gbs = var.bots_compute_memory_gbs
  }

  source_details {
    source_type             = "image"
    source_id               = var.bots_image_id
    boot_volume_size_in_gbs = var.boot_volume_size_gbs
    boot_volume_vpus_per_gb = var.boot_volume_vpus_per_gb
  }

  freeform_tags = var.common_tags
}
