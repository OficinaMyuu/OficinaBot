locals {
  selected_ad_name = data.oci_identity_availability_domains.ads.availability_domains[var.availability_domain_index].name

  instance_image_id = var.instance_image_ocid != null ? var.instance_image_ocid : data.oci_core_images.ubuntu[0].images[0].id

  common_tags = merge(
    var.freeform_tags,
    {
      project    = var.project_name
      managed_by = "terraform"
    }
  )
}
