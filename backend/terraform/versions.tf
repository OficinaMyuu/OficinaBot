terraform {
  required_version = ">= 1.15.5"

  required_providers {
    oci = {
      source  = "oracle/oci"
      version = ">= 8.20.0"
    }
  }

  # The real backend config is injected through an ignored .tfbackend file.
  # Backends are initialized before provider variables, so OCI API auth lives there too.
  backend "oci" {}
}
