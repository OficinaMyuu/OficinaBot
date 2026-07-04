locals {
  selected_ad_name      = data.oci_identity_availability_domains.ads.availability_domains[var.availability_domain_index].name
  selected_bots_ad_name = data.oci_identity_availability_domains.ads.availability_domains[var.bots_availability_domain_index].name

  api_instance_image_id  = var.api_instance_image_ocid != null ? var.api_instance_image_ocid : data.oci_core_images.api_ubuntu[0].images[0].id
  bots_instance_image_id = var.bots_instance_image_ocid != null ? var.bots_instance_image_ocid : data.oci_core_images.bots_ubuntu[0].images[0].id

  common_tags = merge(
    var.freeform_tags,
    {
      project    = var.project_name
      managed_by = "terraform"
    }
  )
}
