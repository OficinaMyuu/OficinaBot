resource "oci_core_vcn" "main" {
  compartment_id = var.compartment_id
  cidr_block     = var.vcn_cidr
  display_name   = "${var.project_name}-vcn"
  dns_label      = var.vcn_dns_label

  freeform_tags = var.common_tags
}

resource "oci_core_internet_gateway" "main" {
  compartment_id = var.compartment_id
  vcn_id         = oci_core_vcn.main.id
  display_name   = "${var.project_name}-igw"
  enabled        = true

  freeform_tags = var.common_tags
}

resource "oci_core_route_table" "public" {
  compartment_id = var.compartment_id
  vcn_id         = oci_core_vcn.main.id
  display_name   = "${var.project_name}-public-rt"

  route_rules {
    destination       = "0.0.0.0/0"
    destination_type  = "CIDR_BLOCK"
    network_entity_id = oci_core_internet_gateway.main.id
  }

  freeform_tags = var.common_tags
}

resource "oci_core_route_table" "private_db" {
  compartment_id = var.compartment_id
  vcn_id         = oci_core_vcn.main.id
  display_name   = "${var.project_name}-private-db-rt"

  freeform_tags = var.common_tags
}

resource "oci_core_security_list" "private_db" {
  compartment_id = var.compartment_id
  vcn_id         = oci_core_vcn.main.id
  display_name   = "${var.project_name}-private-db-sl"

  ingress_security_rules {
    protocol = local.protocol_tcp
    source   = var.public_subnet_cidr

    tcp_options {
      min = var.mysql_port
      max = var.mysql_port
    }

    description = "Allow MySQL from public apps subnet"
  }

  egress_security_rules {
    protocol    = "all"
    destination = "0.0.0.0/0"
    description = "Allow DB outbound, still constrained by private route table"
  }

  freeform_tags = var.common_tags
}

resource "oci_core_security_list" "empty" {
  compartment_id = var.compartment_id
  vcn_id         = oci_core_vcn.main.id
  display_name   = "${var.project_name}-empty-sl"

  freeform_tags = var.common_tags
}

resource "oci_core_subnet" "public_apps" {
  compartment_id             = var.compartment_id
  vcn_id                     = oci_core_vcn.main.id
  cidr_block                 = var.public_subnet_cidr
  display_name               = "${var.project_name}-public-apps-subnet"
  dns_label                  = "public"
  route_table_id             = oci_core_route_table.public.id
  security_list_ids          = [oci_core_security_list.empty.id]
  prohibit_public_ip_on_vnic = false

  freeform_tags = var.common_tags
}

resource "oci_core_subnet" "private_db" {
  compartment_id             = var.compartment_id
  vcn_id                     = oci_core_vcn.main.id
  cidr_block                 = var.private_db_subnet_cidr
  display_name               = "${var.project_name}-private-db-subnet"
  dns_label                  = "db"
  route_table_id             = oci_core_route_table.private_db.id
  security_list_ids          = [oci_core_security_list.private_db.id]
  prohibit_public_ip_on_vnic = true

  freeform_tags = var.common_tags
}

resource "oci_core_network_security_group" "lb" {
  compartment_id = var.compartment_id
  vcn_id         = oci_core_vcn.main.id
  display_name   = "${var.project_name}-lb-nsg"

  freeform_tags = var.common_tags
}

resource "oci_core_network_security_group" "api" {
  compartment_id = var.compartment_id
  vcn_id         = oci_core_vcn.main.id
  display_name   = "${var.project_name}-api-nsg"

  freeform_tags = var.common_tags
}

resource "oci_core_network_security_group" "bots" {
  compartment_id = var.compartment_id
  vcn_id         = oci_core_vcn.main.id
  display_name   = "${var.project_name}-bots-nsg"

  freeform_tags = var.common_tags
}

resource "oci_core_network_security_group_security_rule" "lb_ingress_cloudflare" {
  for_each = local.cloudflare_lb_ingress_rules

  network_security_group_id = oci_core_network_security_group.lb.id
  direction                 = "INGRESS"
  protocol                  = local.protocol_tcp
  source                    = each.value.source
  source_type               = "CIDR_BLOCK"
  description               = "Cloudflare ${each.value.name} to load balancer"

  tcp_options {
    destination_port_range {
      min = each.value.port
      max = each.value.port
    }
  }
}

resource "oci_core_network_security_group_security_rule" "lb_egress_to_api" {
  network_security_group_id = oci_core_network_security_group.lb.id
  direction                 = "EGRESS"
  protocol                  = local.protocol_tcp
  destination               = oci_core_network_security_group.api.id
  destination_type          = "NETWORK_SECURITY_GROUP"
  description               = "LB to API VM"

  tcp_options {
    destination_port_range {
      min = var.api_port
      max = var.api_port
    }
  }
}

resource "oci_core_network_security_group_security_rule" "api_ingress_from_lb" {
  network_security_group_id = oci_core_network_security_group.api.id
  direction                 = "INGRESS"
  protocol                  = local.protocol_tcp
  source                    = oci_core_network_security_group.lb.id
  source_type               = "NETWORK_SECURITY_GROUP"
  description               = "API traffic only from LB"

  tcp_options {
    destination_port_range {
      min = var.api_port
      max = var.api_port
    }
  }
}

resource "oci_core_network_security_group_security_rule" "api_ingress_from_bots" {
  network_security_group_id = oci_core_network_security_group.api.id
  direction                 = "INGRESS"
  protocol                  = local.protocol_tcp
  source                    = oci_core_network_security_group.bots.id
  source_type               = "NETWORK_SECURITY_GROUP"
  description               = "Private backend API traffic from bots VM"

  tcp_options {
    destination_port_range {
      min = var.api_port
      max = var.api_port
    }
  }
}

resource "oci_core_network_security_group_security_rule" "api_ingress_ssh" {
  network_security_group_id = oci_core_network_security_group.api.id
  direction                 = "INGRESS"
  protocol                  = local.protocol_tcp
  source                    = var.ssh_source_cidr
  source_type               = "CIDR_BLOCK"
  description               = "SSH to API VM from admin IP only"

  tcp_options {
    destination_port_range {
      min = 22
      max = 22
    }
  }
}

resource "oci_core_network_security_group_security_rule" "api_egress_all" {
  network_security_group_id = oci_core_network_security_group.api.id
  direction                 = "EGRESS"
  protocol                  = "all"
  destination               = "0.0.0.0/0"
  destination_type          = "CIDR_BLOCK"
  description               = "API outbound internet and VCN traffic"
}

resource "oci_core_network_security_group_security_rule" "bots_ingress_ssh" {
  network_security_group_id = oci_core_network_security_group.bots.id
  direction                 = "INGRESS"
  protocol                  = local.protocol_tcp
  source                    = var.ssh_source_cidr
  source_type               = "CIDR_BLOCK"
  description               = "SSH to bots VM from admin IP only"

  tcp_options {
    destination_port_range {
      min = 22
      max = 22
    }
  }
}

resource "oci_core_network_security_group_security_rule" "bots_egress_all" {
  network_security_group_id = oci_core_network_security_group.bots.id
  direction                 = "EGRESS"
  protocol                  = "all"
  destination               = "0.0.0.0/0"
  destination_type          = "CIDR_BLOCK"
  description               = "Bots outbound internet and VCN traffic"
}
